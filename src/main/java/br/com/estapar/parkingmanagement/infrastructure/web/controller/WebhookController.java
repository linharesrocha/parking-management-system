package br.com.estapar.parkingmanagement.infrastructure.web.controller;

import br.com.estapar.parkingmanagement.application.dto.webhook.WebhookEventDTO;
import br.com.estapar.parkingmanagement.application.service.ParkingEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final ParkingEventService parkingEventService;

    public WebhookController(ParkingEventService parkingEventService) {
        this.parkingEventService = parkingEventService;
    }

    @PostMapping
    public ResponseEntity<String> receiveEvent(@RequestBody WebhookEventDTO eventDTO) {
        log.info("Webhook recebido: {}", eventDTO);

        parkingEventService.processEvent(eventDTO);

        return ResponseEntity.ok("Evento recebido com sucesso.");
    }
}
