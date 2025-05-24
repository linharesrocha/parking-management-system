package br.com.estapar.parkingmanagement.application.service;

import br.com.estapar.parkingmanagement.application.dto.webhook.WebhookEventDTO;
import br.com.estapar.parkingmanagement.domain.model.*;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.ParkingRecordRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SpotRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

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
        log.debug("Tratando ENTRADA para a placa: {}", eventDTO.getLicensePlate());
        String licensePlate = eventDTO.getLicensePlate();

        // Se o veículo nunca foi registrado, então cadastra ele.
        vehicleRepository.findById(licensePlate)
                .orElseGet(() -> {
                   log.info("Veículo com placa {} não encontrado. Criando novo registro.", licensePlate);
                   return vehicleRepository.save(new Vehicle(licensePlate));
                });

        log.info("Evento de ENTRADA processado para o veículo {}.", licensePlate);
    }

    private void handleParkedEvent(WebhookEventDTO eventDTO) {
        log.debug("Tratando ESTACIONAMENTO para a placa: {}", eventDTO.getLicensePlate());

        // Encontra a vaga e o veículo. Se não encontrar, lança uma exceção.
        Spot spot = spotRepository.findByLatAndLng(eventDTO.getLat(), eventDTO.getLng())
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));

        Vehicle vehicle = vehicleRepository.findById(eventDTO.getLicensePlate())
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));

        // Verifica se a vaga já está ocupada
        if(spot.isOccupied()) {
            log.error("Tentativa de estacionar em vaga já ocupada. Vaga ID: {}", spot.getId());
            throw new IllegalStateException("Vaga já está ocupada.");
        }

        Sector sector = spot.getSector();

        // Calcula o preço/hora dinâmico
        BigDecimal dynamicPricePerHour = calculateDynamicPrice(sector);

        // Cria o registro da estadia
        ParkingRecord record = new ParkingRecord();
        record.setVehicle(vehicle);
        record.setSpot(spot);
        record.setEntryTime(LocalDateTime.now());
        record.setStatus(ParkingStatus.ACTIVE);
        record.setPricePerHour(dynamicPricePerHour);

        parkingRecordRepository.save(record);

        // Atualiza o status da vaga
        spot.setOccupied(true);
        spotRepository.save(spot);

        log.info("Veículo {} estacionado na vaga {} do setor {}. Preço/hora aplicado: {}",
                vehicle.getLicensePlate(), spot.getId(), sector.getName(), dynamicPricePerHour);
    }

    private void handleExitEvent(WebhookEventDTO eventDTO) {
        log.debug("Lógica para tratar SAÍDA para a placa: {}", eventDTO.getLicensePlate());
        // TODO: Implementar a lógica de saída.
    }


    private BigDecimal calculateDynamicPrice(Sector sector) {
        // Verifica quantas vagas estão ocupadas no setor
        long occupiedSpots = spotRepository.countBySectorAndOccupied(sector, true);
        double occupancyRate = (double) occupiedSpots / sector.getMaxCapacity();

        BigDecimal basePrice = sector.getBasePrice();
        BigDecimal dynamicPrice;

        if(occupancyRate < 0.25) {
            dynamicPrice = basePrice.multiply(new BigDecimal("0.90"));
        } else if (occupancyRate < 0.50) {
            dynamicPrice = basePrice;
        } else if (occupancyRate < 0.75) {
            dynamicPrice = basePrice.multiply(new BigDecimal("1.10"));
        } else {
            dynamicPrice = basePrice.multiply(new BigDecimal("1.25"));
        }

        return dynamicPrice.setScale(2, RoundingMode.HALF_UP);
    }
}
