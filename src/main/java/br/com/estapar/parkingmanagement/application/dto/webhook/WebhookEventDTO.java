package br.com.estapar.parkingmanagement.application.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookEventDTO {

    @JsonProperty("event_type")
    private EventType eventType;

    @JsonProperty("license_plate")
    private String licensePlate;

    @JsonProperty("entry_time")
    private String entryTime;

    @JsonProperty("exit_time")
    private String exitTime;

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("lng")
    private Double lng;
}
