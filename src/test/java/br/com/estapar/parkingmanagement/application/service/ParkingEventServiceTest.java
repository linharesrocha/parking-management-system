package br.com.estapar.parkingmanagement.application.service;

import br.com.estapar.parkingmanagement.application.dto.webhook.EventType;
import br.com.estapar.parkingmanagement.application.dto.webhook.WebhookEventDTO;
import br.com.estapar.parkingmanagement.domain.model.Vehicle;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.ParkingRecordRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SpotRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
    void proccessEvent_comEntradaDeVeiculoExistente_naoDeveCriarVeiculo() {
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
}
