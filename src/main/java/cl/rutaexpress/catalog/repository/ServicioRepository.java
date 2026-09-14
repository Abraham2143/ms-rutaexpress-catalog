package cl.rutaexpress.catalog.repository;

import cl.rutaexpress.catalog.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
}
