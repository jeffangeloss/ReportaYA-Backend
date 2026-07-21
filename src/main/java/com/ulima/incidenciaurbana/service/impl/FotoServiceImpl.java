package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.FotoDTO;
import com.ulima.incidenciaurbana.model.Foto;
import com.ulima.incidenciaurbana.model.Reporte;
import com.ulima.incidenciaurbana.model.TipoFoto;
import com.ulima.incidenciaurbana.repository.FotoRepository;
import com.ulima.incidenciaurbana.repository.ReporteRepository;
import com.ulima.incidenciaurbana.service.IFirebaseStorageService;
import com.ulima.incidenciaurbana.service.IFotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class FotoServiceImpl implements IFotoService {

    private final FotoRepository fotoRepository;
    private final ReporteRepository reporteRepository;
    private final IFirebaseStorageService firebaseStorageService;

    @Autowired
    public FotoServiceImpl(FotoRepository fotoRepository,
            ReporteRepository reporteRepository,
            IFirebaseStorageService firebaseStorageService) {
        this.fotoRepository = fotoRepository;
        this.reporteRepository = reporteRepository;
        this.firebaseStorageService = firebaseStorageService;
    }

    @Override
    public FotoDTO subirFoto(Long reporteId, String archivoBase64, TipoFoto tipo, String descripcion) {
        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con id: " + reporteId));

        // Acepta tanto base64 puro como un data-URI ("data:image/png;base64,...."):
        // si viene el prefijo, se descarta y solo se decodifica la carga util.
        String base64Limpio = archivoBase64.contains(",")
                ? archivoBase64.substring(archivoBase64.indexOf(',') + 1)
                : archivoBase64;
        byte[] decodedBytes = Base64.getDecoder().decode(base64Limpio);
        String extension = determinarExtension(decodedBytes);
        String nombreArchivo = "fotos/reporte-" + reporteId + "-" + tipo + "-"
                + UUID.randomUUID() + "." + extension;

        String urlFoto;
        if (firebaseStorageService.isAvailable()) {
            urlFoto = firebaseStorageService.uploadFile(nombreArchivo, decodedBytes);
        } else {
            try {
                Path dirPath = Paths.get("uploads/fotos");
                Files.createDirectories(dirPath);
                Path filePath = dirPath.resolve(nombreArchivo);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, decodedBytes);
                urlFoto = "uploads/fotos/" + nombreArchivo;
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar foto localmente: " + e.getMessage(), e);
            }
        }

        Foto foto = new Foto(reporte, urlFoto, tipo, descripcion);
        foto = fotoRepository.save(foto);

        return toDTO(foto);
    }

    private FotoDTO toDTO(Foto foto) {
        FotoDTO dto = new FotoDTO();
        dto.setId(foto.getId());
        dto.setReporteId(foto.getReporte().getId());
        dto.setUrl(foto.getUrl());
        dto.setTipo(foto.getTipo());
        dto.setDescripcion(foto.getDescripcion());
        dto.setFechaCarga(foto.getFechaCarga());
        return dto;
    }

    private String determinarExtension(byte[] bytes) {
        if (bytes.length < 4) return "jpg";
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "png";
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return "jpg";
        if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes.length > 12 && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50)
            return "webp";
        return "jpg";
    }
}
