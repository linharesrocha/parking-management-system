package br.com.estapar.parkingmanagement.infrastructure.web.controller;

import br.com.estapar.parkingmanagement.application.dto.error.ApiErrorResponseDTO;
import br.com.estapar.parkingmanagement.application.dto.webhook.WebhookEventDTO;
import br.com.estapar.parkingmanagement.application.service.ParkingEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/webhook")
@Tag(name = "Webhook API", description = "Endpoint para recebimento de eventos do simulador de garagem.")
public class WebhookController {

    private final ParkingEventService parkingEventService;

    public WebhookController(ParkingEventService parkingEventService) {
        this.parkingEventService = parkingEventService;
    }

    @PostMapping
    @Operation(summary = "Recebe eventos do simulador de garagem",
            description = "Este endpoint é chamado pelo simulador para notificar sobre eventos como entrada, estacionamento e saída de veículos. " +
                    "O corpo da requisição deve ser um JSON correspondente ao WebhookEventDTO.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payload do evento enviado pelo simulador.",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WebhookEventDTO.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento recebido e processado com sucesso.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Evento recebido com sucesso."))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: JSON malformado, tipo de evento desconhecido).",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar o evento.",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponseDTO.class)) })
    })
    public ResponseEntity<String> receiveEvent(@RequestBody WebhookEventDTO eventDTO) {
        log.info("Webhook recebido: {}", eventDTO);

        parkingEventService.processEvent(eventDTO);

        return ResponseEntity.ok("Evento recebido com sucesso.");
    }
}
