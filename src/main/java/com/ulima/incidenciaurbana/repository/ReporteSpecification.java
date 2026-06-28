package com.ulima.incidenciaurbana.repository;

import com.ulima.incidenciaurbana.model.EstadoReporte;
import com.ulima.incidenciaurbana.model.Reporte;
import com.ulima.incidenciaurbana.model.TipoProblema;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ReporteSpecification {

    public static Specification<Reporte> filtrarParaMapa(EstadoReporte estado, TipoProblema tipo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (estado != null) predicates.add(cb.equal(root.get("estado"), estado));
            if (tipo != null) predicates.add(cb.equal(root.get("tipoProblema"), tipo));

            root.fetch("ubicacion", JoinType.LEFT);
            root.fetch("cuenta", JoinType.LEFT);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
