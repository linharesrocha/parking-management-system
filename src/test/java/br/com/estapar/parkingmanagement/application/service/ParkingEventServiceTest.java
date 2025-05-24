package br.com.estapar.parkingmanagement.application.service;

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
}
