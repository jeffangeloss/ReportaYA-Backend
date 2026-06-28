-- ============================================
-- DDL para crear las tablas del proyecto ReportaYA
-- Sistema de Gestion de Reportes Urbanos
-- ============================================
-- Ejecutar en la base de datos desde pgAdmin (Query Tool).
--
-- ESTADOS: PENDIENTE, REVISION, FINALIZADO, RECHAZADO
-- TIPOS PROBLEMA: INFRAESTRUCTURA, RESIDUOS, SEGURIDAD, ALUMBRADO, OTRO
-- TIPOS FOTO: INICIAL, FINAL
-- ============================================

-- 1) Personas
CREATE TABLE IF NOT EXISTS personas (
  id BIGSERIAL PRIMARY KEY,
  nombres VARCHAR(255) NOT NULL,
  apellidos VARCHAR(255) NOT NULL,
  dni VARCHAR(50) NOT NULL UNIQUE,
  telefono VARCHAR(50) NOT NULL,
  correo VARCHAR(255) NOT NULL UNIQUE
);

-- 2) Cuentas (tabla base para herencia JOINED)
CREATE TABLE IF NOT EXISTS cuentas (
  id BIGSERIAL PRIMARY KEY,
  usuario VARCHAR(255) NOT NULL UNIQUE,
  contrasena_hash VARCHAR(255) NOT NULL,
  persona_id BIGINT NOT NULL UNIQUE,
  fecha_creacion TIMESTAMP WITHOUT TIME ZONE,
  fecha_actualizacion TIMESTAMP WITHOUT TIME ZONE,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  token_verificacion VARCHAR(255),
  token_expiracion TIMESTAMP WITHOUT TIME ZONE,
  CONSTRAINT fk_cuentas_persona FOREIGN KEY (persona_id) REFERENCES personas (id) ON DELETE RESTRICT
);

-- Migracion para bases de datos ya creadas (verificacion de correo via Resend).
-- Hibernate (ddl-auto=update) tambien agrega estas columnas al iniciar la app.
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS token_verificacion VARCHAR(255);
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS token_expiracion TIMESTAMP WITHOUT TIME ZONE;

-- 3) Subclases de Cuenta (JOINED)
CREATE TABLE IF NOT EXISTS operadores_municipales (
  id BIGINT PRIMARY KEY,
  CONSTRAINT fk_op_mun_cuenta FOREIGN KEY (id) REFERENCES cuentas (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tecnicos (
  id BIGINT PRIMARY KEY,
  CONSTRAINT fk_tecnicos_cuenta FOREIGN KEY (id) REFERENCES cuentas (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ciudadanos (
  id BIGINT PRIMARY KEY,
  CONSTRAINT fk_ciudadanos_cuenta FOREIGN KEY (id) REFERENCES cuentas (id) ON DELETE CASCADE
);

-- 4) Ubicaciones
CREATE TABLE IF NOT EXISTS ubicaciones (
  id BIGSERIAL PRIMARY KEY,
  direccion VARCHAR(500) NOT NULL,
  latitud DOUBLE PRECISION,
  longitud DOUBLE PRECISION,
  distrito VARCHAR(255),
  provincia VARCHAR(255),
  departamento VARCHAR(255)
);

-- 5) Reportes
CREATE TABLE IF NOT EXISTS reportes (
  id BIGSERIAL PRIMARY KEY,
  titulo VARCHAR(255) NOT NULL,
  descripcion VARCHAR(1000) NOT NULL,
  cuenta_id BIGINT NOT NULL,
  ubicacion_id BIGINT NOT NULL,
  tecnico_id BIGINT,
  tipo_problema VARCHAR(50),
  estado VARCHAR(50) NOT NULL,
  fecha_creacion TIMESTAMP WITHOUT TIME ZONE,
  fecha_actualizacion TIMESTAMP WITHOUT TIME ZONE,
  comentario_resolucion VARCHAR(1000),
  fecha_cierre TIMESTAMP WITHOUT TIME ZONE,
  CONSTRAINT fk_reportes_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id) ON DELETE CASCADE,
  CONSTRAINT fk_reportes_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones (id) ON DELETE CASCADE,
  CONSTRAINT fk_reportes_tecnico FOREIGN KEY (tecnico_id) REFERENCES tecnicos (id) ON DELETE SET NULL
);

-- 6) Fotos
CREATE TABLE IF NOT EXISTS fotos (
  id BIGSERIAL PRIMARY KEY,
  reporte_id BIGINT NOT NULL,
  url VARCHAR(500) NOT NULL,
  tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('INICIAL', 'FINAL')),
  descripcion VARCHAR(255),
  fecha_carga TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  CONSTRAINT fk_fotos_reporte FOREIGN KEY (reporte_id) REFERENCES reportes (id) ON DELETE CASCADE
);

-- 7) Historial de Estados
CREATE TABLE IF NOT EXISTS historial_estados (
  id BIGSERIAL PRIMARY KEY,
  reporte_id BIGINT NOT NULL,
  estado_anterior VARCHAR(50),
  estado_nuevo VARCHAR(50) NOT NULL,
  fecha_cambio TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  CONSTRAINT fk_historial_reporte FOREIGN KEY (reporte_id) REFERENCES reportes (id) ON DELETE CASCADE
);

-- 8) Token de Notificaciones (FCM tokens para Firebase)
CREATE TABLE IF NOT EXISTS token_notificaciones (
  id BIGSERIAL PRIMARY KEY,
  cuenta_id BIGINT NOT NULL UNIQUE,
  token VARCHAR(512) NOT NULL,
  CONSTRAINT fk_token_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id) ON DELETE CASCADE
);

-- Indices
CREATE INDEX IF NOT EXISTS idx_reportes_cuenta_id ON reportes (cuenta_id);
CREATE INDEX IF NOT EXISTS idx_reportes_ubicacion_id ON reportes (ubicacion_id);
CREATE INDEX IF NOT EXISTS idx_reportes_tecnico_id ON reportes (tecnico_id);
CREATE INDEX IF NOT EXISTS idx_reportes_estado ON reportes (estado);
CREATE INDEX IF NOT EXISTS idx_reportes_fecha_creacion ON reportes (fecha_creacion DESC);
CREATE INDEX IF NOT EXISTS idx_fotos_reporte_id ON fotos (reporte_id);
CREATE INDEX IF NOT EXISTS idx_historial_reporte_id ON historial_estados (reporte_id);
CREATE INDEX IF NOT EXISTS idx_historial_fecha_cambio ON historial_estados (fecha_cambio DESC);
CREATE INDEX IF NOT EXISTS idx_token_cuenta_id ON token_notificaciones (cuenta_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_token_verificacion ON cuentas (token_verificacion);

-- ============================================
-- SCRIPTS DE LIMPIEZA (usar con precaucion)
-- ============================================
/*
DROP TABLE IF EXISTS token_notificaciones CASCADE;
DROP TABLE IF EXISTS historial_estados CASCADE;
DROP TABLE IF EXISTS fotos CASCADE;
DROP TABLE IF EXISTS reportes CASCADE;
DROP TABLE IF EXISTS ubicaciones CASCADE;
DROP TABLE IF EXISTS operadores_municipales CASCADE;
DROP TABLE IF EXISTS tecnicos CASCADE;
DROP TABLE IF EXISTS ciudadanos CASCADE;
DROP TABLE IF EXISTS cuentas CASCADE;
DROP TABLE IF EXISTS personas CASCADE;
*/

-- ============================================
-- FLUJO DE ESTADOS:
-- PENDIENTE -> REVISION (operador acepta)
-- PENDIENTE -> RECHAZADO (operador rechaza)
-- REVISION  -> FINALIZADO (tecnico completa trabajo)
-- ============================================
