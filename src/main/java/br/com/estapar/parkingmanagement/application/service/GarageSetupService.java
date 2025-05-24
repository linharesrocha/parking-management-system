package br.com.estapar.parkingmanagement.application.service;

import br.com.estapar.parkingmanagement.application.dto.GarageConfigDTO;
import br.com.estapar.parkingmanagement.application.dto.SectorDTO;
import br.com.estapar.parkingmanagement.application.dto.SpotDTO;
import br.com.estapar.parkingmanagement.domain.model.Sector;
import br.com.estapar.parkingmanagement.domain.model.Spot;
import br.com.estapar.parkingmanagement.infrastructure.adapter.out.web.GarageSimulatorClient;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SectorRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SpotRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class GarageSetupService {

    private static final Logger log = LoggerFactory.getLogger(GarageSetupService.class);

    private final GarageSimulatorClient garageSimulatorClient;
    private final SectorRepository sectorRepository;
    private final SpotRepository spotRepository;

    public GarageSetupService(GarageSimulatorClient garageSimulatorClient, SectorRepository sectorRepository, SpotRepository spotRepository) {
        this.garageSimulatorClient = garageSimulatorClient;
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
    }

    @PostConstruct // Executa este método apenas uma vez quando a aplicação inicia.
    @Transactional
    public void initializeGarage() {
        // Garante que o BD esteja vazio para não duplicar
        if(sectorRepository.count() > 0) {
            log.warn("Garagem já inicializada. Pulando setup.");
            return;
        }

        log.info("Iniciando setup da garagem...");
        GarageConfigDTO configDTO = garageSimulatorClient.fetchGarageConfig();

        // Cria um mapa de Vagas DTO por nome de setor para facilitar a busca
        Map<String, List<SpotDTO>> spotsBySector = configDTO.getSpots().stream()
                .collect(Collectors.groupingBy(SpotDTO::getSectorName));

        // Itera sobre os Setores DTO
        configDTO.getSectors().forEach(sectorDto -> {
            // 1. Converte o Setor DTO para entidade
            Sector sectorEntity = toSectorEntity(sectorDto);

            // 2. Encontra as vagas para este setor e as converte para entidades
            List<SpotDTO> sectorSpotsDto = spotsBySector.get(sectorDto.getName());
            if(sectorSpotsDto != null) {
                sectorSpotsDto.forEach(spotDto -> {
                    Spot spotEntity = toSpotEntity(spotDto, sectorEntity);
                    // 3. Adiciona a vaga na lista do setor
                    sectorEntity.getSpots().add(spotEntity);
                });
            }

            // 4. Salva o SETOR. CascadeType.ALL irá salvar todas as vagas junto
            sectorRepository.save(sectorEntity);
        });

        log.info("Setup da garagem concluído com sucesso! {} setores e {} vagas criadas.",
                configDTO.getSectors().size(), configDTO.getSpots().size());
    }

    private Sector toSectorEntity(SectorDTO dto) {
        Sector sector = new Sector();
        sector.setName(dto.getName());
        sector.setBasePrice(dto.getBasePrice());
        sector.setMaxCapacity(dto.getMaxCapacity());
        sector.setOpenHour(LocalTime.parse(dto.getOpenHour(), DateTimeFormatter.ofPattern("HH:mm")));
        sector.setCloseHour(LocalTime.parse(dto.getCloseHour(), DateTimeFormatter.ofPattern("HH:mm")));
        return sector;
    }

    private Spot toSpotEntity(SpotDTO dto, Sector sector) {
        Spot spot = new Spot();
        spot.setSector(sector);
        spot.setLat(dto.getLat());
        spot.setLng(dto.getLng());
        spot.setOccupied(dto.isOccupied());
        return spot;
    }
}
