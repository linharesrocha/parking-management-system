package br.com.estapar.parkingmanagement.infrastructure.web.controller;

import br.com.estapar.parkingmanagement.application.dto.query.*;
import br.com.estapar.parkingmanagement.application.service.ParkingEventService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ParkingQueryController {

    private final ParkingEventService parkingEventService;

    public ParkingQueryController(ParkingEventService parkingEventService) {
        this.parkingEventService = parkingEventService;
    }

    @PostMapping("/plate-status")
    public ResponseEntity<PlateStatusResponseDTO> getPlateStatus(@Valid @RequestBody PlateStatusRequestDTO requestDTO) {
        log.info("Recebida requisição para /plate-status: {}", requestDTO);

        Optional<PlateStatusResponseDTO> responseOpt = parkingEventService.getPlateStatus(requestDTO.getLicensePlate());

        return responseOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/spot-status")
    public ResponseEntity<SpotStatusResponseDTO> getSpotStatus(@Valid @RequestBody SpotStatusRequestDTO requestDTO) {
        log.info("Recebida requisição para /spot-status: lat={}, lng={]", requestDTO.getLat(), requestDTO.getLng());

        Optional<SpotStatusResponseDTO> responseOpt = parkingEventService.getSpotStatus(requestDTO.getLat(), requestDTO.getLng());

        return responseOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueResponseDTO> getRevenue(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("sector") String sectorName) {

        log.info("Recebida requisição para /revenue: date={}, sector={}", date, sectorName);

        RevenueResponseDTO responseDTO = parkingEventService.getRevenueForSectorAndDate(sectorName, date);

        return ResponseEntity.ok(responseDTO);
    }
}
