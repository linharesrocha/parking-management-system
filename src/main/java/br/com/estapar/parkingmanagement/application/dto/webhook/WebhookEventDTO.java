package br.com.estapar.parkingmanagement.application.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "DTO para receber eventos do simulador de garagem via webhook.")
public class WebhookEventDTO {

    @JsonProperty("event_type")
    @Schema(description = "Tipo do evento ocorrido.", example = "PARKED", requiredMode = Schema.RequiredMode.REQUIRED)
    private EventType eventType;

    @JsonProperty("license_plate")
    @Schema(description = "Placa do veículo associado ao evento (pode ser nula para alguns eventos).", example = "BRA2E19", nullable = true)
    private String licensePlate;

    @JsonProperty("entry_time")
    @Schema(description = "Data e hora de entrada (para eventos ENTRY), formato ISO.", example = "2025-01-01T12:00:00Z", nullable = true)
    private String entryTime;

    @JsonProperty("exit_time")
    @Schema(description = "Data e hora de saída (para eventos EXIT), formato ISO.", example = "2025-01-01T14:30:00Z", nullable = true)
    private String exitTime;

    @JsonProperty("lat")
    @Schema(description = "Latitude (para eventos PARKED).", example = "-23.561684", nullable = true)
    private Double lat;

    @JsonProperty("lng")
    @Schema(description = "Longitude (para eventos PARKED).", example = "-46.655981", nullable = true)
    private Double lng;
}
