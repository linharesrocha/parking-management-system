package br.com.estapar.parkingmanagement.infrastructure.persistence.repository;

import br.com.estapar.parkingmanagement.domain.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
}
