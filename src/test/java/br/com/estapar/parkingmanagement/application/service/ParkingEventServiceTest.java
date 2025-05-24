package br.com.estapar.parkingmanagement.application.service;

import br.com.estapar.parkingmanagement.application.dto.query.PlateStatusResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.query.SpotStatusResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.webhook.EventType;
import br.com.estapar.parkingmanagement.application.dto.webhook.WebhookEventDTO;
import br.com.estapar.parkingmanagement.domain.model.*;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.ParkingRecordRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SpotRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingEventServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private ParkingRecordRepository parkingRecordRepository;

    @InjectMocks
    private ParkingEventService parkingEventService;

    @Test
    void processEvent_comEntradaDeVeiculoNovo_deveCriarVeiculo() {
        // Arrange
        WebhookEventDTO entryEvent = new WebhookEventDTO();
        entryEvent.setEventType(EventType.ENTRY);
        entryEvent.setLicensePlate("NEW-0001");

        when(vehicleRepository.findById("NEW-0001")).thenReturn(Optional.empty());

        // Act
        parkingEventService.processEvent(entryEvent);

        // Assert
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void processEvent_comEntradaDeVeiculoExistente_naoDeveCriarVeiculo() {
        // Arrange
        WebhookEventDTO entryEvent = new WebhookEventDTO();
        entryEvent.setEventType(EventType.ENTRY);
        entryEvent.setLicensePlate("OLD-0002");

        Vehicle existingVehicle = new Vehicle("OLD-0002");

        when(vehicleRepository.findById("OLD-0002")).thenReturn(Optional.of(existingVehicle));

        // Act
        parkingEventService.processEvent(entryEvent);

        // Assert
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void processEvent_comEventoDeEstacionamento_deveCriarRegistroEAtualizarVaga() {
        // Arrange
        WebhookEventDTO parkedEvent = new WebhookEventDTO();
        parkedEvent.setEventType(EventType.PARKED);
        parkedEvent.setLicensePlate("ABC-1234");
        parkedEvent.setLat(-23.0);
        parkedEvent.setLng(-46.0);

        Sector sector = new Sector();
        sector.setName("A");
        sector.setMaxCapacity(100);
        sector.setBasePrice(new BigDecimal("10.00"));

        Spot spot = new Spot();
        spot.setId(1L);
        spot.setSector(sector);
        spot.setOccupied(false);

        Vehicle vehicle = new Vehicle("ABC-1234");

        when(spotRepository.findByLatAndLng(anyDouble(), anyDouble())).thenReturn(Optional.of(spot));
        when(vehicleRepository.findById(anyString())).thenReturn(Optional.of(vehicle));
        // simulando 30% de ocupação
        when(spotRepository.countBySectorAndOccupied(sector, true)).thenReturn(30L);

        // Act
        parkingEventService.processEvent(parkedEvent);

        // Assert
        ArgumentCaptor<ParkingRecord> recordCaptor = ArgumentCaptor.forClass(ParkingRecord.class);
        ArgumentCaptor<Spot> spotCaptor = ArgumentCaptor.forClass(Spot.class);

        verify(parkingRecordRepository, times(1)).save(recordCaptor.capture());
        verify(spotRepository, times(1)).save(spotCaptor.capture());

        ParkingRecord savedRecord = recordCaptor.getValue();
        assertEquals(ParkingStatus.ACTIVE, savedRecord.getStatus());
        // Preço base R$10 com 30% de ocupação -> regra de 50% -> preço normal
        assertEquals(0, new BigDecimal("10.00").compareTo(savedRecord.getPricePerHour()));

        Spot updatedSpot = spotCaptor.getValue();
        assertTrue(updatedSpot.isOccupied()); // Verifica se a vaga foi marcada como ocupada
    }

    @Test
    void processEvent_comEventoDeEstacionamentoEmVagaOcupada_deveLancarExcecao() {
        // Arrange
        WebhookEventDTO parkedEvent = new WebhookEventDTO();
        parkedEvent.setEventType(EventType.PARKED);
        parkedEvent.setLicensePlate("ABC-1234");
        parkedEvent.setLat(-23.0);
        parkedEvent.setLng(-46.0);

        Sector sector = new Sector();
        sector.setName("A");
        sector.setMaxCapacity(100);
        sector.setBasePrice(new BigDecimal("10.00"));

        Spot spotOcupada = new Spot();
        spotOcupada.setId(1L);
        spotOcupada.setSector(sector);
        spotOcupada.setOccupied(true); // vaga ocupada

        when(spotRepository.findByLatAndLng(anyDouble(), anyDouble())).thenReturn(Optional.of(spotOcupada));
        when(vehicleRepository.findById(anyString())).thenReturn(Optional.of(new Vehicle("QUALQUER-PLACA")));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            parkingEventService.processEvent(parkedEvent);
        });

        verify(parkingRecordRepository, never()).save(any());
    }

    @Test
    void processEvent_comEventoDeSaida_deveFinalizarRegistroECalcularTarifa() {
        // Arrange
        String plate = "ABC-1234";
        WebhookEventDTO exitEvent = new WebhookEventDTO();
        exitEvent.setEventType(EventType.EXIT);
        exitEvent.setLicensePlate(plate);

        LocalDateTime entryTime = LocalDateTime.parse("2025-01-01T10:00:00");
        LocalDateTime exitTime = LocalDateTime.parse("2025-01-01T12:00:00"); // 2 horas depois
        exitEvent.setExitTime(exitTime.format(DateTimeFormatter.ISO_DATE_TIME));

        Vehicle vehicle = new Vehicle(plate);
        Sector sector = new Sector();
        Spot spot = new Spot();
        spot.setId(1L);
        spot.setSector(sector);
        spot.setOccupied(true);

        ParkingRecord activeRecord = new ParkingRecord();
        activeRecord.setId(100L);
        activeRecord.setVehicle(vehicle);
        activeRecord.setSpot(spot);
        activeRecord.setEntryTime(entryTime);
        activeRecord.setStatus(ParkingStatus.ACTIVE);
        activeRecord.setPricePerHour(new BigDecimal("15.00"));

        when(parkingRecordRepository.findByVehicleLicensePlateAndStatus(plate, ParkingStatus.ACTIVE))
                .thenReturn(Optional.of(activeRecord));

        // Act
        parkingEventService.processEvent(exitEvent);

        // Assert
        ArgumentCaptor<ParkingRecord> recordCaptor = ArgumentCaptor.forClass(ParkingRecord.class);
        ArgumentCaptor<Spot> spotCaptor = ArgumentCaptor.forClass(Spot.class);

        verify(parkingRecordRepository, times(1)).save(recordCaptor.capture());
        verify(spotRepository, times(1)).save(spotCaptor.capture());

        ParkingRecord savedRecord = recordCaptor.getValue();
        assertEquals(ParkingStatus.COMPLETED, savedRecord.getStatus());
        assertEquals(exitTime, savedRecord.getExitTime());
        // Tarifa esperada: 2 horas * R$15.00 a hora = R$30.00
        assertEquals(0, new BigDecimal("30.00").compareTo(savedRecord.getFinalFare()));

        Spot freedSpot = spotCaptor.getValue();
        assertFalse(freedSpot.isOccupied());
    }

    @Test
    void processEvent_comEventoDeSaidaSemRegistroAtivo_deveLancarExcecao() {
        // Arrange
        String plate = "XYZ-7890";
        WebhookEventDTO exitEvent = new WebhookEventDTO();
        exitEvent.setEventType(EventType.EXIT);
        exitEvent.setLicensePlate(plate);
        exitEvent.setExitTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        when(parkingRecordRepository.findByVehicleLicensePlateAndStatus(plate, ParkingStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            parkingEventService.processEvent(exitEvent);
        });

        verify(spotRepository, never()).save(any());
    }

    @Test
    void getPlaceStatus_quandoRegistroAtivoExiste_deveRetornarStatusDTOCorreto() {
        // Arrange
        String licensePlate = "PARKED-01";
        LocalDateTime entryTime = LocalDateTime.now().minusHours(1).minusMinutes(30);

        Vehicle vehicle = new Vehicle(licensePlate);
        Spot spot = new Spot();
        spot.setLat(-10.0);
        spot.setLng(-20.0);

        ParkingRecord activeRecord = new ParkingRecord();
        activeRecord.setVehicle(vehicle);
        activeRecord.setSpot(spot);
        activeRecord.setEntryTime(entryTime);
        activeRecord.setPricePerHour(new BigDecimal("20.00"));

        when(parkingRecordRepository.findByVehicleLicensePlateAndStatus(licensePlate, ParkingStatus.ACTIVE))
                .thenReturn(Optional.of(activeRecord));

        // Act
        Optional<PlateStatusResponseDTO> resultOpt = parkingEventService.getPlateStatus(licensePlate);

        // Assert
        assertTrue(resultOpt.isPresent(), "O Optional retornado não deveria estar vazio.");

        PlateStatusResponseDTO resultDTO = resultOpt.get();
        assertEquals(licensePlate, resultDTO.getLicensePlate());
        assertEquals(-10.0, resultDTO.getLat());
        assertNotNull(resultDTO.getTimeParked(), "A duração do estacionamento não deveria ser nula.");
        assertEquals(0, new BigDecimal("30.00").compareTo(resultDTO.getPriceUntilNow()));
    }

    @Test
    void getPlateStatus_quandoNaoExisteRegistroAtivo_deveRetornarOptionalVazio() {
        // Arrange
        String licensePlate = "NOT-PARKED-02";

        when(parkingRecordRepository.findByVehicleLicensePlateAndStatus(licensePlate, ParkingStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Act
        Optional<PlateStatusResponseDTO> resultOpt = parkingEventService.getPlateStatus(licensePlate);

        // Assert
        assertTrue(resultOpt.isEmpty(), "O Optional retornado deveria estar vazio.");
    }

    @Test
    void getSpotStatus_quandoVagaNaoEncontrada_deveRetornarOptionalVazio() {
        // Arrange
        Double lat = -10.0;
        Double lng = -20.0;

        when(spotRepository.findByLatAndLng(lat, lng)).thenReturn(Optional.empty());

        // Act
        Optional<SpotStatusResponseDTO> resultOpt = parkingEventService.getSpotStatus(lat, lng);

        // Assert
        assertTrue(resultOpt.isEmpty(), "Deveria retornar um Optional vazio se a vaga não existe.");
    }

    @Test
    void getSpotStatus_quandoVagaEncontradaELivre_deveRetornarDTOCorreto() {
        // Arrange
        Double lat = -10.0;
        Double lng = -20.0;
        Spot vagaLivre = new Spot();
        vagaLivre.setId(1L);
        vagaLivre.setLat(lat);
        vagaLivre.setLng(lng);
        vagaLivre.setOccupied(false);

        when(spotRepository.findByLatAndLng(lat, lng)).thenReturn(Optional.of(vagaLivre));

        // Act
        Optional<SpotStatusResponseDTO> resultOpt = parkingEventService.getSpotStatus(lat, lng);

        // Assert
        assertTrue(resultOpt.isPresent());
        SpotStatusResponseDTO dto = resultOpt.get();
        assertFalse(dto.isOccupied(), "O DTO deveria indicar que a vaga não está ocupada.");
        assertNull(dto.getLicensePlate(), "Placa deveria ser nula para vaga livre.");
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getPriceUntilNow()), "Preço deveria ser zero para vaga livre.");
        assertNull(dto.getEntryTime(), "Tempo de entrada deveria ser nulo para vaga livre.");
        assertNull(dto.getTimeParked(), "Tempo estacionado deveria ser nulo para vaga livre.");
    }

    @Test
    void getSpotStatus_quandoVagaOcupadaComRegistroAtivo_deveRetornarDTOCompleto() {
        // Arrange
        Double lat = -10.0;
        Double lng = -20.0;
        LocalDateTime entryTime = LocalDateTime.now().minusHours(2);

        Vehicle vehicle = new Vehicle("XYZ-1234");
        Sector sector = new Sector();
        Spot vagaOcupada = new Spot();
        vagaOcupada.setId(1L);
        vagaOcupada.setLat(lat);
        vagaOcupada.setLng(lng);
        vagaOcupada.setOccupied(true);
        vagaOcupada.setSector(sector);

        ParkingRecord registroAtivo = new ParkingRecord();
        registroAtivo.setVehicle(vehicle);
        registroAtivo.setSpot(vagaOcupada);
        registroAtivo.setEntryTime(entryTime);
        registroAtivo.setStatus(ParkingStatus.ACTIVE);
        registroAtivo.setPricePerHour(new BigDecimal("10.00"));

        when(spotRepository.findByLatAndLng(lat, lng)).thenReturn(Optional.of(vagaOcupada));
        when(parkingRecordRepository.findBySpotAndStatus(vagaOcupada, ParkingStatus.ACTIVE))
                .thenReturn(Optional.of(registroAtivo));

        // Act
        Optional<SpotStatusResponseDTO> resultOpt = parkingEventService.getSpotStatus(lat, lng);

        // Assert
        assertTrue(resultOpt.isPresent());
        SpotStatusResponseDTO dto = resultOpt.get();
        assertTrue(dto.isOccupied());
        assertEquals("XYZ-1234", dto.getLicensePlate());
        assertEquals(0, new BigDecimal("20.00").compareTo(dto.getPriceUntilNow()));
        assertEquals(entryTime.format(DateTimeFormatter.ISO_DATE_TIME), dto.getEntryTime());
        assertNotNull(dto.getTimeParked());
    }

    @Test
    void getSpotStatus_quandoVagaOcupadaSemRegistroAtivo_deveRetornarOcupadaComMsgErro() {
        // Arrange
        Double lat = -10.0;
        Double lng = -20.0;

        Spot vagaOcupada = new Spot();
        vagaOcupada.setId(1L);
        vagaOcupada.setLat(lat);
        vagaOcupada.setLng(lng);
        vagaOcupada.setOccupied(true);

        when(spotRepository.findByLatAndLng(lat, lng)).thenReturn(Optional.of(vagaOcupada));
        when(parkingRecordRepository.findBySpotAndStatus(vagaOcupada, ParkingStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Act
        Optional<SpotStatusResponseDTO> resultOpt = parkingEventService.getSpotStatus(lat, lng);

        // Assert
        assertTrue(resultOpt.isPresent());
        SpotStatusResponseDTO dto = resultOpt.get();
        assertTrue(dto.isOccupied(), "A vaga ainda deve ser reportada como ocupada.");
        assertEquals("ERRO_INTERNO_VAGA_SEM_REGISTRO_ATIVO", dto.getLicensePlate(), "Deveria indicar um erro na placa.");
        assertNull(dto.getPriceUntilNow(), "Preço deveria ser nulo ou não presente no DTO em caso de erro interno.");
        assertNull(dto.getEntryTime());
        assertNull(dto.getTimeParked());
    }


}
