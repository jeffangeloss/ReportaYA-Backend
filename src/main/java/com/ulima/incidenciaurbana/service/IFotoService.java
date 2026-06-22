package com.ulima.incidenciaurbana.service;

import com.ulima.incidenciaurbana.dto.FotoDTO;
import com.ulima.incidenciaurbana.model.TipoFoto;

public interface IFotoService {

    FotoDTO subirFoto(Long reporteId, String archivoBase64, TipoFoto tipo, String descripcion);
}
