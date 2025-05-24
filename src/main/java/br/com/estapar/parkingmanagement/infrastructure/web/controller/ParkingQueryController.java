package br.com.estapar.parkingmanagement.infrastructure.web.controller;

import br.com.estapar.parkingmanagement.application.dto.query.PlateStatusRequestDTO;
import br.com.estapar.parkingmanagement.application.dto.query.PlateStatusResponseDTO;
import br.com.estapar.parkingmanagement.application.service.ParkingEventService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
