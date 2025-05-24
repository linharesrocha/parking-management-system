package br.com.estapar.parkingmanagement.application.service;

import br.com.estapar.parkingmanagement.application.dto.GarageConfigDTO;
import br.com.estapar.parkingmanagement.application.dto.SectorDTO;
import br.com.estapar.parkingmanagement.application.dto.SpotDTO;
import br.com.estapar.parkingmanagement.domain.model.Sector;
import br.com.estapar.parkingmanagement.infrastructure.adapter.out.web.GarageSimulatorClient;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SectorRepository;
import br.com.estapar.parkingmanagement.infrastructure.persistence.repository.SpotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GarageSetupServiceTest {

    @Mock
    private GarageSimulatorClient garageSimulatorClient;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private SpotRepository spotRepository;

    @InjectMocks
    private GarageSetupService garageSetupService;

    @Test
    void deveInicializarGaragem_quandoBancoDeDadosEstiverVazio() {
        // Arrange
        SectorDTO sectorDto = new SectorDTO();
        sectorDto.setName("A");
        sectorDto.setBasePrice(new BigDecimal("10.0"));
        sectorDto.setMaxCapacity(50);
        sectorDto.setOpenHour("08:00");
        sectorDto.setCloseHour("22:00");

        SpotDTO spotDto = new SpotDTO();
        spotDto.setSectorName("A");
        spotDto.setLat(-23.0);
        spotDto.setLng(-46.0);
        spotDto.setOccupied(false);

        GarageConfigDTO fakeConfig = new GarageConfigDTO();
        fakeConfig.setSectors(List.of(sectorDto));
        fakeConfig.setSpots(List.of(spotDto));

        when(sectorRepository.count()).thenReturn(0L);
        when(garageSimulatorClient.fetchGarageConfig()).thenReturn(fakeConfig);

        // Act
        garageSetupService.initializeGarage();

        // Assert
        ArgumentCaptor<Sector>  sectorArgumentCaptor = ArgumentCaptor.forClass(Sector.class);

        verify(sectorRepository, times(1)).save(sectorArgumentCaptor.capture());

        Sector savedSector = sectorArgumentCaptor.getValue();

        assertNotNull(savedSector);
        assertEquals("A", savedSector.getName());
        assertEquals(1, savedSector.getSpots().size());
        assertEquals(-23.0, savedSector.getSpots().get(0).getLat());

        verify(spotRepository, never()).save(any()); // verificando se cascade está funcionando
    }
}
