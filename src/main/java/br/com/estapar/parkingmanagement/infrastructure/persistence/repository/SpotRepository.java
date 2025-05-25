package br.com.estapar.parkingmanagement.infrastructure.persistence.repository;

import br.com.estapar.parkingmanagement.domain.model.Sector;
import br.com.estapar.parkingmanagement.domain.model.Spot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpotRepository extends JpaRepository<Spot, Long> {
    /**
     * Busca uma vaga de estacionamento por suas coordenadas geográficas.
     * @param lat A latitude da vaga.
     * @param lng A longitude da vaga.
     * @return um Optional contendo a Spot se encontrada, caso contrário, um Optional vazio.
     */
    Optional<Spot> findByLatAndLng(Double lat, Double lng);

    /**
     *  Conta o número de vagas de estacionamento em um setor específico com base no status de ocupação.
     * @param sector O setor no qual as vagas de estacionamento estão localizadas.
     * @param occupied Um valor booleano indicando o status de ocupação das vagas.
     * @return O número de vagas de estacionamento que correspondem aos critérios fornecidos.
     */
    long countBySectorAndOccupied(Sector sector, boolean occupied);
}
