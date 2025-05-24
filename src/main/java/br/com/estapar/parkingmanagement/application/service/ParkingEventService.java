package br.com.estapar.parkingmanagement.application.service;

import br.com.estapar.parkingmanagement.application.dto.query.PlateStatusResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.query.RevenueResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.query.SpotStatusResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.webhook.WebhookEventDTO;
import br.com.estapar.parkingmanagement.domain.exception.ResourceNotFoundException;
import br.com.estapar.parkingmanagement.domain.model.*;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.ParkingRecordRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SectorRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SpotRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class ParkingEventService {

    private static final Logger log = LoggerFactory.getLogger(ParkingEventService.class);

    private final VehicleRepository vehicleRepository;
    private final SpotRepository spotRepository;
    private final ParkingRecordRepository parkingRecordRepository;
    private final SectorRepository sectorRepository;

    public ParkingEventService(VehicleRepository vehicleRepository, SpotRepository spotRepository,
                               ParkingRecordRepository parkingRecordRepository, SectorRepository sectorRepository) {
        this.vehicleRepository = vehicleRepository;
        this.spotRepository = spotRepository;
        this.parkingRecordRepository = parkingRecordRepository;
        this.sectorRepository = sectorRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada para as coordenadas fornecidas."));

        Vehicle vehicle = vehicleRepository.findById(eventDTO.getLicensePlate())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado para a placa fornecida."));

        Sector sector = spot.getSector();

        // Verifica se o setor tem capacidade
        long occupiedSpots = spotRepository.countBySectorAndOccupied(sector, true);
        if(occupiedSpots >= sector.getMaxCapacity()) {
            log.warn("Tentativa de estacionar no setor '{}' que já está com lotação máxima de {} vagas. Rejeitando evento PARKED para a placa {}.",
                    sector.getName(), sector.getMaxCapacity(), eventDTO.getLicensePlate());
            throw new IllegalStateException("Lotação máxima atingida para o setor " + sector.getName());
        }

        // Verifica se a vaga já está ocupada
        if(spot.isOccupied()) {
            log.error("Tentativa de estacionar em vaga já ocupada. Vaga ID: {}", spot.getId());
            throw new IllegalStateException("Vaga já está ocupada.");
        }

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
        log.debug("Tratando SAÍDA para a placa: {}", eventDTO.getLicensePlate());
        String licensePlate = eventDTO.getLicensePlate();

        // Encontrar o ParkingRecord ativo para este veículo
        ParkingRecord activeRecord = parkingRecordRepository
                .findByVehicleLicensePlateAndStatus(licensePlate, ParkingStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de estacionamento ativo encontrado para a placa: " + licensePlate));

        // Definir hora da saída
        LocalDateTime exitTime = LocalDateTime.parse(eventDTO.getExitTime(), DateTimeFormatter.ISO_DATE_TIME);
        activeRecord.setExitTime(exitTime);

        // Calcular o valor final
        LocalDateTime entryTime = activeRecord.getEntryTime();
        BigDecimal pricePerHour = activeRecord.getPricePerHour();

        // Calcula a duração em minutos
        long durationInMinutes = Duration.between(entryTime, exitTime).toMinutes();

        if(durationInMinutes <= 0) {
            durationInMinutes = 0;
        }

        // Converte minutos para horas e calcula o valor
        BigDecimal durationInHours = new BigDecimal(durationInMinutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        BigDecimal finalFare = durationInHours.multiply(pricePerHour).setScale(2, RoundingMode.HALF_UP);

        activeRecord.setFinalFare(finalFare);

        // Mudar o status para COMPLETED
        activeRecord.setStatus(ParkingStatus.COMPLETED);
        parkingRecordRepository.save(activeRecord);

        // Liberar a vaga
        Spot spot = activeRecord.getSpot();
        spot.setOccupied(false);
        spotRepository.save(spot);

        log.info("Saída registrada para o veículo {}. Tempo: {} minutos. Valor: R${}. Vaga {} liberada.",
                licensePlate, durationInMinutes, finalFare, spot.getId());
    }

    public PlateStatusResponseDTO getPlateStatus(String licensePlate) {
        log.debug("Buscando status para a placa: {}", licensePlate);

        ParkingRecord activeRecord = parkingRecordRepository
                .findByVehicleLicensePlateAndStatus(licensePlate, ParkingStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de estacionamento ativo encontrado para a placa: " + licensePlate));

        // Extrair dados
        Spot spot = activeRecord.getSpot();
        Vehicle vehicle = activeRecord.getVehicle();

        LocalDateTime entryTime = activeRecord.getEntryTime();
        LocalDateTime currentTime = LocalDateTime.now();
        Duration duration = Duration.between(entryTime, currentTime);

        // Calcula o preço até o momento
        BigDecimal pricePerHour = activeRecord.getPricePerHour();

        // Converte a duração para horas decimais (ex: 1.5 para 1 hora e 30 min).
        // Usa 2 casas de precisão para a divisão para evitar dízimas no cálculo de horas.
        BigDecimal durationInHours = new BigDecimal(duration.toMinutes())
                .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);

        BigDecimal priceUntilNow = durationInHours.multiply(pricePerHour)
                .setScale(2, RoundingMode.HALF_UP);

        // Monta DTO da Resposta
        PlateStatusResponseDTO responseDTO = new PlateStatusResponseDTO(
                vehicle.getLicensePlate(),
                priceUntilNow,
                entryTime.format(DateTimeFormatter.ISO_DATE_TIME),
                duration.toString(),
                spot.getLat(),
                spot.getLng()
        );

        log.info("Status encontrado para a placa {}: {}", licensePlate, responseDTO);
        return responseDTO;
    }

    public SpotStatusResponseDTO getSpotStatus(Double lat, Double lng) {
        log.debug("Buscando status para a vaga em lat: {}, lng: {}", lat, lng);

        Spot spot = spotRepository.findByLatAndLng(lat, lng)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma vaga encontrada para as coordenadas lat: " + lat + ", lng: " + lng));

        SpotStatusResponseDTO responseDTO = new SpotStatusResponseDTO();
        responseDTO.setOccupied(spot.isOccupied());

        // Se a vaga estiver OCUPADA, encontra os detalhes do veículo e da estadia.
        if (spot.isOccupied()) {
            // Busca o registro de estacionamento ATIVO para ESTA vaga específica.
            Optional<ParkingRecord> activeRecordOpt = parkingRecordRepository
                    .findBySpotAndStatus(spot, ParkingStatus.ACTIVE);

            if (activeRecordOpt.isPresent()) {
                ParkingRecord activeRecord = activeRecordOpt.get();
                Vehicle vehicle = activeRecord.getVehicle();

                LocalDateTime entryTime = activeRecord.getEntryTime();
                LocalDateTime currentTime = LocalDateTime.now();
                Duration duration = Duration.between(entryTime, currentTime);

                BigDecimal pricePerHour = activeRecord.getPricePerHour();
                BigDecimal durationInHours = new BigDecimal(duration.toMinutes())
                        .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
                BigDecimal priceUntilNow = durationInHours.multiply(pricePerHour)
                        .setScale(2, RoundingMode.HALF_UP);

                responseDTO.setLicensePlate(vehicle.getLicensePlate());
                responseDTO.setPriceUntilNow(priceUntilNow);
                responseDTO.setEntryTime(entryTime.format(DateTimeFormatter.ISO_DATE_TIME));
                responseDTO.setTimeParked(duration.toString()); // Formato ISO "PTnHnMnS"
            } else {
                log.error("INCONSISTÊNCIA DE DADOS: Vaga ID {} está marcada como ocupada, mas não foi encontrado ParkingRecord ativo.", spot.getId());
                responseDTO.setLicensePlate("ERRO_INTERNO_VAGA_SEM_REGISTRO_ATIVO");
            }
        } else {
            // Se a vaga não está ocupada, os campos de veículo, preço e tempo permanecem nulos.
            responseDTO.setLicensePlate(null);
            responseDTO.setPriceUntilNow(BigDecimal.ZERO);
            responseDTO.setEntryTime(null);
            responseDTO.setTimeParked(null);
        }

        return responseDTO;
    }

    public RevenueResponseDTO getRevenueForSectorAndDate(String sectorName, LocalDate date) {
        log.debug("Calculando faturamento para o setor {} na data {}", sectorName, date);

        Sector sector = sectorRepository.findByName(sectorName)
                .orElseThrow(() -> new ResourceNotFoundException("Setor com nome '" + sectorName + "' não encontrado."));

        // Definindo o intervalo de tempo de consulta
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.plusDays(1).atStartOfDay();

        // Chama o repositório que faz a soma no banco de dados.
        BigDecimal totalRevenue = parkingRecordRepository.sumFinalFareBySectorAndDateRange(
                sector,
                ParkingStatus.COMPLETED,
                startDate,
                endDate
        );

        if(totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        log.info("Faturamento calculado para o setor {} na data {}: R$ {}", sectorName, date, totalRevenue);

        return new RevenueResponseDTO(totalRevenue, "BRL", LocalDateTime.now());
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
