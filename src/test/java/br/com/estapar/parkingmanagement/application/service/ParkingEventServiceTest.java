package br.com.estapar.parkingmanagement.application.service;

import br.com.estapar.parkingmanagement.application.dto.query.PlateStatusResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.query.RevenueResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.query.SpotStatusResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.webhook.EventType;
import br.com.estapar.parkingmanagement.application.dto.webhook.WebhookEventDTO;
import br.com.estapar.parkingmanagement.domain.exception.ResourceNotFoundException;
import br.com.estapar.parkingmanagement.domain.model.*;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.ParkingRecordRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SectorRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SpotRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private SectorRepository sectorRepository;

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
    void processEvent_comEventoDeEstacionamentoEmSetorLotado_deveLancarExcecao() {
        // Arrange
        WebhookEventDTO parkedEvent = new WebhookEventDTO();
        parkedEvent.setEventType(EventType.PARKED);
        parkedEvent.setLicensePlate("FULL-001");
        parkedEvent.setLat(-25.0);
        parkedEvent.setLng(-45.0);

        Sector setorLotado = new Sector();
        setorLotado.setName("LOTADO");
        setorLotado.setMaxCapacity(1);

        Spot vagaNoSetorLotado = new Spot();
        vagaNoSetorLotado.setId(10L);
        vagaNoSetorLotado.setSector(setorLotado);
        vagaNoSetorLotado.setOccupied(false);

        Vehicle vehicle = new Vehicle("FULL-001");

        when(spotRepository.findByLatAndLng(parkedEvent.getLat(), parkedEvent.getLng()))
                .thenReturn(Optional.of(vagaNoSetorLotado));
        when(vehicleRepository.findById(parkedEvent.getLicensePlate()))
                .thenReturn(Optional.of(vehicle));

        when(spotRepository.countBySectorAndOccupied(setorLotado, true)).thenReturn(1L);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            parkingEventService.processEvent(parkedEvent);
        });

        assertTrue(exception.getMessage().contains("Lotação máxima atingida para o setor LOTADO"));
        verify(parkingRecordRepository, never()).save(any(ParkingRecord.class));
        verify(spotRepository, never()).save(vagaNoSetorLotado);
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
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            parkingEventService.processEvent(exitEvent);
        });

        // Opcional, mas bom: verificar a mensagem da exceção
        assertTrue(exception.getMessage().contains(plate), "A mensagem da exceção deveria conter a placa.");
    }

    @Test
    void getPlateStatus_quandoRegistroAtivoExiste_deveRetornarStatusDTOCorreto() {
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
        PlateStatusResponseDTO resultDTO = parkingEventService.getPlateStatus(licensePlate);

        // Assert
        assertNotNull(resultDTO, "O DTO retornado não deveria ser nulo.");
        assertEquals(licensePlate, resultDTO.getLicensePlate());
        assertEquals(-10.0, resultDTO.getLat());
        assertNotNull(resultDTO.getTimeParked(), "A duração do estacionamento não deveria ser nula.");
        assertEquals(0, new BigDecimal("30.00").compareTo(resultDTO.getPriceUntilNow()));
    }

    @Test
    void getPlateStatus_quandoNaoExisteRegistroAtivo_deveLancarResourceNotFoundException() {
        // Arrange
        String licensePlate = "NOT-PARKED-02";

        when(parkingRecordRepository.findByVehicleLicensePlateAndStatus(licensePlate, ParkingStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            parkingEventService.getPlateStatus(licensePlate);
        });
        assertTrue(exception.getMessage().contains(licensePlate));
    }

    @Test
    void getSpotStatus_quandoVagaNaoEncontrada_deveLancarResourceNotFoundException() {
        // Arrange
        Double lat = -10.0;
        Double lng = -20.0;

        when(spotRepository.findByLatAndLng(lat, lng)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            parkingEventService.getSpotStatus(lat, lng);
        });
        assertTrue(exception.getMessage().contains("Nenhuma vaga encontrada para as coordenadas"));
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
        SpotStatusResponseDTO resultDTO = parkingEventService.getSpotStatus(lat, lng);

        // Assert
        assertNotNull(resultDTO);
        assertFalse(resultDTO.isOccupied());
        assertNull(resultDTO.getLicensePlate());
        assertEquals(0, BigDecimal.ZERO.compareTo(resultDTO.getPriceUntilNow()));
        assertNull(resultDTO.getEntryTime());
        assertNull(resultDTO.getTimeParked());
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
        SpotStatusResponseDTO resultDTO = parkingEventService.getSpotStatus(lat, lng);

        // Assert
        assertNotNull(resultDTO);
        assertTrue(resultDTO.isOccupied());
        assertEquals("XYZ-1234", resultDTO.getLicensePlate());
        assertEquals(0, new BigDecimal("20.00").compareTo(resultDTO.getPriceUntilNow()));
        assertEquals(entryTime.format(DateTimeFormatter.ISO_DATE_TIME), resultDTO.getEntryTime());
        assertNotNull(resultDTO.getTimeParked());
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
        SpotStatusResponseDTO responseDTO = parkingEventService.getSpotStatus(lat, lng);

        // Assert
        assertNotNull(responseDTO);
        assertTrue(responseDTO.isOccupied());
        assertEquals("ERRO_INTERNO_VAGA_SEM_REGISTRO_ATIVO", responseDTO.getLicensePlate());
        assertNull(responseDTO.getPriceUntilNow());
        assertNull(responseDTO.getEntryTime());
        assertNull(responseDTO.getTimeParked());
    }

    @Test
    void getRevenueForSectorAndDate_quandoSetorNaoEncontrado_deveLancarResourceNotFoundException() {
        // Arrange
        String nomeSetorInexistente = "SETOR_QUE_NAO_EXISTE";
        LocalDate dataConsulta = LocalDate.of(2025, 1, 1);

        when(sectorRepository.findByName(nomeSetorInexistente)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            parkingEventService.getRevenueForSectorAndDate(nomeSetorInexistente, dataConsulta);
        });
        assertTrue(exception.getMessage().contains(nomeSetorInexistente));

        verify(parkingRecordRepository, never()).sumFinalFareBySectorAndDateRange(any(), any(), any(), any());
    }

    @Test
    void getRevenueForSectorAndDate_quandoSetorEncontradoMasSemRegistros_deveRetornarFaturamentoZero() {
        // Arrange
        String nomeSetor = "A";
        LocalDate dataConsulta = LocalDate.of(2025, 1, 1);
        Sector setorExistente = new Sector();
        setorExistente.setName(nomeSetor);

        LocalDateTime inicioDoDia = dataConsulta.atStartOfDay();
        LocalDateTime fimDoDia = dataConsulta.plusDays(1).atStartOfDay();

        when(sectorRepository.findByName(nomeSetor)).thenReturn(Optional.of(setorExistente));
        when(parkingRecordRepository.sumFinalFareBySectorAndDateRange(
                setorExistente, ParkingStatus.COMPLETED, inicioDoDia, fimDoDia))
                .thenReturn(null);

        // Act
        RevenueResponseDTO resultado = parkingEventService.getRevenueForSectorAndDate(nomeSetor, dataConsulta);

        // Assert
        assertNotNull(resultado);
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.getAmount()));
        assertEquals("BRL", resultado.getCurrency());
        assertNotNull(resultado.getTimestamp());
    }

    @Test
    void getRevenueForSectorAndDate_quandoSetorEncontradoComRegistros_deveRetornarFaturamentoCorreto() {
        // Arrange
        String nomeSetor = "B";
        LocalDate dataConsulta = LocalDate.of(2025, 1, 15);
        Sector setorExistente = new Sector();
        setorExistente.setName(nomeSetor);

        LocalDateTime inicioDoDia = dataConsulta.atStartOfDay();
        LocalDateTime fimDoDia = dataConsulta.plusDays(1).atStartOfDay();
        BigDecimal faturamentoEsperado = new BigDecimal("250.75");

        when(sectorRepository.findByName(nomeSetor)).thenReturn(Optional.of(setorExistente));
        when(parkingRecordRepository.sumFinalFareBySectorAndDateRange(
                setorExistente, ParkingStatus.COMPLETED, inicioDoDia, fimDoDia))
                .thenReturn(faturamentoEsperado);

        // Act
        RevenueResponseDTO resultado = parkingEventService.getRevenueForSectorAndDate(nomeSetor, dataConsulta);

        // Assert
        assertNotNull(resultado);
        assertEquals(0, faturamentoEsperado.compareTo(resultado.getAmount()));
        assertEquals("BRL", resultado.getCurrency());
        assertNotNull(resultado.getTimestamp());
    }
}
