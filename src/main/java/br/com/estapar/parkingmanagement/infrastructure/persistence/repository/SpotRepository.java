package br.com.estapar.parkingmanagement.infrastructure.persistence.repository;

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
}
