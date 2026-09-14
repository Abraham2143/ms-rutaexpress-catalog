package cl.rutaexpress.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicioRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 1000) String descripcion,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal tarifa,
        @NotNull @PositiveOrZero Integer capacidadDisponible,
        @NotNull Boolean activo
) {
}
