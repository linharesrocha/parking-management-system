package br.com.estapar.parkingmanagement.infrastructure.persistence.repository;

import br.com.estapar.parkingmanagement.domain.model.ParkingRecord;
import br.com.estapar.parkingmanagement.domain.model.ParkingStatus;
import br.com.estapar.parkingmanagement.domain.model.Sector;
import br.com.estapar.parkingmanagement.domain.model.Spot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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


    /**
     * Calcula a soma total das tarifas finais (finalFare) para todos os registros de estacionamento
     * que foram completados em um determinado setor e dentro de um intervalo de datas específico.
     * A consulta considera o {@code exitTime} do registro para o intervalo de datas.
     *
     * @param sector    A entidade {@link Sector} para a qual o faturamento será calculado.
     * @param status    O {@link ParkingStatus} dos registros a serem considerados (tipicamente {@code COMPLETED}).
     * @param startDate O início do intervalo de datas (inclusivo) para o {@code exitTime}.
     * @param endDate   O fim do intervalo de datas (exclusivo) para o {@code exitTime}.
     * @return A soma total das tarifas como um {@link BigDecimal}. Pode retornar {@code null} se nenhum
     * registro correspondente for encontrado.
     */
    @Query("SELECT SUM(pr.finalFare) FROM ParkingRecord pr " +
            "WHERE pr.spot.sector = :sector " +
            "AND pr.status = :status " +
            "AND pr.exitTime >= :startDate AND pr.exitTime < :endDate")
    BigDecimal sumFinalFareBySectorAndDataRange(
            @Param("sector")Sector sector,
            @Param("status") ParkingStatus status,
            @Param("startDate")LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
