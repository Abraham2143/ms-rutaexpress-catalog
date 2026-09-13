package cl.rutaexpress.catalog.service;

import cl.rutaexpress.catalog.dto.ServicioRequest;
import cl.rutaexpress.catalog.dto.ServicioResponse;
import cl.rutaexpress.catalog.entity.Servicio;
import cl.rutaexpress.catalog.exception.ResourceNotFoundException;
import cl.rutaexpress.catalog.repository.ServicioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioServiceImplTests {

    @Mock
    private ServicioRepository repository;

    @InjectMocks
    private ServicioServiceImpl service;

    private final ServicioRequest request = new ServicioRequest(
            "Entrega Express", "Entrega durante el mismo dia", new BigDecimal("7990"), 20, true);

    @Test
    void crearPersisteTodosLosCamposYDevuelveIdGenerado() {
        when(repository.save(any(Servicio.class))).thenAnswer(invocation -> {
            Servicio servicio = invocation.getArgument(0);
            assertThat(servicio.getId()).isNull();
            assertThat(servicio.getNombre()).isEqualTo(request.nombre());
            assertThat(servicio.getDescripcion()).isEqualTo(request.descripcion());
            assertThat(servicio.getTarifa()).isEqualByComparingTo(request.tarifa());
            assertThat(servicio.getCapacidadDisponible()).isEqualTo(20);
            assertThat(servicio.getActivo()).isTrue();
            servicio.setId(1L);
            return servicio;
        });

        ServicioResponse response = service.crear(request);

        assertThat(response).isEqualTo(new ServicioResponse(1L, request.nombre(), request.descripcion(),
                request.tarifa(), 20, true));
        verify(repository).save(any(Servicio.class));
    }

    @Test
    void consultarServicioExistente() {
        when(repository.findById(1L)).thenReturn(Optional.of(servicioExistente()));

        ServicioResponse response = service.obtenerPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Entrega Express");
        verify(repository, never()).save(any());
    }

    @Test
    void listarIncluyeServiciosActivosEInactivos() {
        Servicio activo = servicioExistente();
        Servicio inactivo = servicioExistente();
        inactivo.setId(2L);
        inactivo.setActivo(false);
        when(repository.findAll()).thenReturn(List.of(activo, inactivo));

        assertThat(service.listar()).extracting(ServicioResponse::activo).containsExactly(true, false);
    }

    @Test
    void listarSinServiciosDevuelveListaVacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }

    @Test
    void actualizarReemplazaCamposYConservaId() {
        Servicio existente = servicioExistente();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        ServicioRequest cambio = new ServicioRequest("Entrega normal", null, BigDecimal.ZERO, 0, false);

        ServicioResponse response = service.actualizar(1L, cambio);

        assertThat(response).isEqualTo(new ServicioResponse(1L, "Entrega normal", null, BigDecimal.ZERO, 0, false));
        verify(repository).save(existente);
    }

    @Test
    void consultarInexistenteLanzaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("99");
    }

    @Test
    void actualizarInexistenteNoCreaUnServicio() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    private Servicio servicioExistente() {
        Servicio servicio = new Servicio();
        servicio.setId(1L);
        servicio.setNombre(request.nombre());
        servicio.setDescripcion(request.descripcion());
        servicio.setTarifa(request.tarifa());
        servicio.setCapacidadDisponible(request.capacidadDisponible());
        servicio.setActivo(request.activo());
        return servicio;
    }
}
