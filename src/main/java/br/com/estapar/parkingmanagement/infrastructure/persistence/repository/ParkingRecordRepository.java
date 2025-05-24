package br.com.estapar.parkingmanagement.infrastructure.persistence.repository;

import br.com.estapar.parkingmanagement.domain.model.ParkingRecord;
import br.com.estapar.parkingmanagement.domain.model.ParkingStatus;
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
}
