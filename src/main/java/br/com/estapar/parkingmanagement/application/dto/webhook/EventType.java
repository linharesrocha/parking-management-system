package br.com.estapar.parkingmanagement.application.dto.webhook;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipos de eventos que podem ser recebidos pelo webhook do simulador.")
public enum EventType {
    ENTRY,
    PARKED,
    EXIT
}
