package cl.rutaexpress.catalog.service;

import cl.rutaexpress.catalog.dto.ServicioRequest;
import cl.rutaexpress.catalog.dto.ServicioResponse;
import cl.rutaexpress.catalog.entity.Servicio;
import cl.rutaexpress.catalog.exception.ResourceNotFoundException;
import cl.rutaexpress.catalog.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicioServiceImpl implements ServicioService {

    private final ServicioRepository repository;

    @Override
    public List<ServicioResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public ServicioResponse obtenerPorId(Long id) {
        return toResponse(buscarServicio(id));
    }

    @Override
    @Transactional
    public ServicioResponse crear(ServicioRequest request) {
        Servicio servicio = new Servicio();
        aplicarDatos(servicio, request);
        return toResponse(repository.save(servicio));
    }

    @Override
    @Transactional
    public ServicioResponse actualizar(Long id, ServicioRequest request) {
        Servicio servicio = buscarServicio(id);
        aplicarDatos(servicio, request);
        return toResponse(repository.save(servicio));
    }

    private Servicio buscarServicio(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el servicio con id " + id));
    }

    private void aplicarDatos(Servicio servicio, ServicioRequest request) {
        servicio.setNombre(request.nombre());
        servicio.setDescripcion(request.descripcion());
        servicio.setTarifa(request.tarifa());
        servicio.setCapacidadDisponible(request.capacidadDisponible());
        servicio.setActivo(request.activo());
    }

    private ServicioResponse toResponse(Servicio servicio) {
        return new ServicioResponse(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getTarifa(), servicio.getCapacidadDisponible(), servicio.getActivo());
    }
}
