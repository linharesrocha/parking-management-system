package br.com.estapar.parkingmanagement.infrastructure.adapter.out.web;

import br.com.estapar.parkingmanagement.application.dto.GarageConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GarageSimulatorClient {

    private final Logger log = LoggerFactory.getLogger(GarageSimulatorClient.class);

    private final RestTemplate restTemplate;
    private final String simulatorUrl;

    public GarageSimulatorClient(RestTemplate restTemplate, @Value("${simulator.api.url}") String simulatorUrl) {
        this.restTemplate = restTemplate;
        this.simulatorUrl = simulatorUrl;
    }

    public GarageConfigDTO fetchGarageConfig() {
        String url = simulatorUrl + "/garage";
        log.info("Buscando configuração da garagem em: {}", url);
        return restTemplate.getForObject(url, GarageConfigDTO.class);
    }
}
