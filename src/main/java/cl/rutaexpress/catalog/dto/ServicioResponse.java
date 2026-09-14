package cl.rutaexpress.catalog.dto;

import java.math.BigDecimal;

public record ServicioResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal tarifa,
        Integer capacidadDisponible,
        Boolean activo
) {
}
