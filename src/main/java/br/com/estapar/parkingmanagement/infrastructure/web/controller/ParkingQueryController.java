package br.com.estapar.parkingmanagement.infrastructure.web.controller;

import br.com.estapar.parkingmanagement.application.dto.error.ApiErrorResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.query.*;
import br.com.estapar.parkingmanagement.application.service.ParkingEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Parking Query API", description = "APIs para consulta de informações do estacionamento.")
public class ParkingQueryController {

    private final ParkingEventService parkingEventService;

    public ParkingQueryController(ParkingEventService parkingEventService) {
        this.parkingEventService = parkingEventService;
    }

    @PostMapping("/plate-status")
    @Operation(summary = "Consulta o status de um veículo",
            description = "Retorna o status atual de um veículo no estacionamento, incluindo preço acumulado e tempo estacionado, se estiver ativo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status do veículo encontrado com sucesso.",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PlateStatusResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: placa em branco ou formato incorreto).",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "Veículo não encontrado ou não possui um registro de estacionamento ativo.",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class)) })
    })
    public ResponseEntity<PlateStatusResponseDTO> getPlateStatus(@Valid @RequestBody PlateStatusRequestDTO requestDTO) {
        log.info("Recebida requisição para /plate-status: {}", requestDTO);

        Optional<PlateStatusResponseDTO> responseOpt = parkingEventService.getPlateStatus(requestDTO.getLicensePlate());

        return responseOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/spot-status")
    @Operation(summary = "Consulta o status de uma vaga específica",
            description = "Retorna se uma vaga está ocupada e, caso positivo, informações do veículo estacionado e custos acumulados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status da vaga encontrado com sucesso.",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SpotStatusResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: coordenadas ausentes ou inválidas).",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "Vaga não encontrada para as coordenadas fornecidas.",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class)) })
    })
    public ResponseEntity<SpotStatusResponseDTO> getSpotStatus(@Valid @RequestBody SpotStatusRequestDTO requestDTO) {
        log.info("Recebida requisição para /spot-status: lat={}, lng={]", requestDTO.getLat(), requestDTO.getLng());

        Optional<SpotStatusResponseDTO> responseOpt = parkingEventService.getSpotStatus(requestDTO.getLat(), requestDTO.getLng());

        return responseOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/revenue")
    @Operation(summary = "Consulta o faturamento de um setor em uma data específica",
            description = "Retorna o faturamento total (soma das tarifas finais) para todos os veículos que saíram de um determinado setor em uma data específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Faturamento calculado com sucesso.",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RevenueResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Parâmetros de requisição inválidos (ex: data em formato incorreto, nome do setor ausente).",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class)) })
    })
    public ResponseEntity<RevenueResponseDTO> getRevenue(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("sector") String sectorName) {

        log.info("Recebida requisição para /revenue: date={}, sector={}", date, sectorName);

        RevenueResponseDTO responseDTO = parkingEventService.getRevenueForSectorAndDate(sectorName, date);

        return ResponseEntity.ok(responseDTO);
    }
}
