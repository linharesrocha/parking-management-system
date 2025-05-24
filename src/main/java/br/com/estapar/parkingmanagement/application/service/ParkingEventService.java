package br.com.estapar.parkingmanagement.application.service;

import br.com.estapar.parkingmanagement.application.dto.webhook.WebhookEventDTO;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.ParkingRecordRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SpotRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingEventService {

    private static final Logger log = LoggerFactory.getLogger(ParkingEventService.class);

    private final VehicleRepository vehicleRepository;
    private final SpotRepository spotRepository;
    private final ParkingRecordRepository parkingRecordRepository;

    public ParkingEventService(VehicleRepository vehicleRepository, SpotRepository spotRepository, ParkingRecordRepository parkingRecordRepository) {
        this.vehicleRepository = vehicleRepository;
        this.spotRepository = spotRepository;
        this.parkingRecordRepository = parkingRecordRepository;
    }

    @Transactional
    public void processEvent(WebhookEventDTO eventDTO) {
        log.info("Processando evento do tipo: {}", eventDTO.getEventType());

        // Switch para redirecionar o evento para o método correto
        switch(eventDTO.getEventType()) {
            case ENTRY -> handleEntryEvent(eventDTO);
            case PARKED -> handleParkedEvent(eventDTO);
            case EXIT -> handleExitEvent(eventDTO);
            default -> log.warn("Tipo de evento desconhecidos recebido: {}", eventDTO.getEventType());
        }
    }

    private void handleEntryEvent(WebhookEventDTO eventDTO) {
        log.debug("Lógica para tratar ENTRADA para a placa: {}", eventDTO.getLicensePlate());
        // TODO: Implementar a lógica de entrada.
    }

    private void handleParkedEvent(WebhookEventDTO eventDTO) {
        log.debug("Lógica para tratar ESTACIONAMENTO para a placa: {}", eventDTO.getLicensePlate());
        // TODO: Implementar a lógica de estacionamento.
    }

    private void handleExitEvent(WebhookEventDTO eventDTO) {
        log.debug("Lógica para tratar SAÍDA para a placa: {}", eventDTO.getLicensePlate());
        // TODO: Implementar a lógica de saída.
    }
}
