package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.CiudadanoDTO;
import com.ulima.incidenciaurbana.model.Ciudadano;
import com.ulima.incidenciaurbana.repository.CiudadanoRepository;
import com.ulima.incidenciaurbana.service.ICiudadanoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CiudadanoServiceImpl implements ICiudadanoService {

    private final CiudadanoRepository ciudadanoRepository;

    @Autowired
    public CiudadanoServiceImpl(CiudadanoRepository ciudadanoRepository) {
        this.ciudadanoRepository = ciudadanoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CiudadanoDTO obtenerCiudadanoPorId(Long id) {
        if (id == null) throw new RuntimeException("El ID del ciudadano es obligatorio");
        Ciudadano ciudadano = ciudadanoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ciudadano no encontrado con id: " + id));
        return convertirADTO(ciudadano);
    }

    @Override
    public CiudadanoDTO actualizarCiudadano(Long id, CiudadanoDTO ciudadanoDTO) {
        if (id == null) throw new RuntimeException("El ID del ciudadano es obligatorio");
        Ciudadano ciudadano = ciudadanoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ciudadano no encontrado con id: " + id));

        ciudadano.getPersona().setTelefono(ciudadanoDTO.getTelefono());
        ciudadano.getPersona().setCorreo(ciudadanoDTO.getCorreo());

        ciudadano = ciudadanoRepository.save(ciudadano);
        return convertirADTO(ciudadano);
    }

    @Override
    public void eliminarCiudadano(Long id) {
        if (id == null) throw new RuntimeException("El ID del ciudadano es obligatorio");
        if (!ciudadanoRepository.existsById(id)) {
            throw new RuntimeException("Ciudadano no encontrado con id: " + id);
        }
        ciudadanoRepository.deleteById(id);
    }

    private CiudadanoDTO convertirADTO(Ciudadano ciudadano) {
        return new CiudadanoDTO(
                ciudadano.getId(),
                ciudadano.getUsuario(),
                ciudadano.getPersona().getNombres(),
                ciudadano.getPersona().getApellidos(),
                ciudadano.getPersona().getDni(),
                ciudadano.getPersona().getTelefono(),
                ciudadano.getPersona().getCorreo(),
                ciudadano.isActivo());
    }
}
