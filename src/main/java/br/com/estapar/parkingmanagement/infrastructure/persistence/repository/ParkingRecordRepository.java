package br.com.estapar.parkingmanagement.infrastructure.persistence.repository;

import br.com.estapar.parkingmanagement.domain.model.ParkingRecord;
import br.com.estapar.parkingmanagement.domain.model.ParkingStatus;
import br.com.estapar.parkingmanagement.domain.model.Spot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParkingRecordRepository extends JpaRepository<ParkingRecord, Long> {

    /**
     * Busca o registro de estacionamento ATIVO para um veículo específico.
     * Método principal de consulta para o endpoint de 'plate-status'
     *
     * @param licensePlate A placa do veículo a ser consultado.
     * @param status O status desejado (neste caso, sempre será ACTIVE).
     * @return um Optional contendo o ParkingRecord ativo, se houver.
     */
    Optional<ParkingRecord> findByVehicleLicensePlateAndStatus(String licensePlate, ParkingStatus status);


    /**
     * Busca um registro de estacionamento baseado em uma Vaga (Spot) específica e um Status de estacionamento.
     * Útil para encontrar, por exemplo, qual veículo está ativamente estacionado em uma determinada vaga.
     *
     * @param spot   A entidade {@link Spot} pela qual filtrar os registros de estacionamento.
     * @param status O {@link ParkingStatus} desejado para o registro (ex: ACTIVE).
     * @return um {@link Optional} contendo o {@link ParkingRecord} correspondente,
     * ou um Optional vazio se nenhum registro for encontrado com os critérios fornecidos.
     */
    Optional<ParkingRecord> findBySpotAndStatus(Spot spot, ParkingStatus status);
}
