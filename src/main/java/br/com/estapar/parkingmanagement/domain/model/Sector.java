package br.com.estapar.parkingmanagement.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "sectors")
public class Sector {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    private LocalTime openHour;

    @Column(nullable = false)
    private LocalTime closeHour;

    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL,  fetch = FetchType.LAZY)
    private List<Spot> spots = new ArrayList<>();
}
