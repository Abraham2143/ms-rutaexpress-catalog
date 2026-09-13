package cl.rutaexpress.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "SERVICIOS")
@Getter
@Setter
@NoArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "serviciosSequence")
    @SequenceGenerator(name = "serviciosSequence", sequenceName = "SERVICIOS_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 1000)
    private String descripcion;

    @Column(name = "TARIFA", nullable = false, precision = 14, scale = 2)
    private BigDecimal tarifa;

    @Column(name = "CAPACIDAD_DISPONIBLE", nullable = false)
    private Integer capacidadDisponible;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo;
}
