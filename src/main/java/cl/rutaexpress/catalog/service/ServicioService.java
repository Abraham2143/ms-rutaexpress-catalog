package cl.rutaexpress.catalog.service;

import cl.rutaexpress.catalog.dto.ServicioRequest;
import cl.rutaexpress.catalog.dto.ServicioResponse;

import java.util.List;

public interface ServicioService {
    List<ServicioResponse> listar();

    ServicioResponse obtenerPorId(Long id);

    ServicioResponse crear(ServicioRequest request);

    ServicioResponse actualizar(Long id, ServicioRequest request);
}
