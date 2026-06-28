# ReportaYA - Backend

Backend REST API del sistema de gestion de reportes urbanos **ReportaYA**, desarrollado con Spring Boot.

## Tecnologias

- Java 17
- Spring Boot 3.5.6
- PostgreSQL
- JWT (jjwt 0.12.3) para autenticacion
- BCrypt para hash de contrasenas
- Firebase Storage para almacenamiento de fotos
- Firebase Cloud Messaging para notificaciones push

## Requisitos

- Java 17+
- Maven 3.9+
- PostgreSQL
- (Opcional) Firebase service account key para fotos y notificaciones

## Configuracion

1. Crear la base de datos en PostgreSQL
2. Crear `application-local.properties` en la raiz del proyecto:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tu_db
SPRING_DATASOURCE_USERNAME=tu_usuario
SPRING_DATASOURCE_PASSWORD=tu_password
JWT_SECRET=un_string_de_al_menos_32_caracteres
FIREBASE_STORAGE_BUCKET=tu-proyecto.firebasestorage.app
RESEND_API_KEY=re_tu_api_key
RESEND_FROM=ReportaYA <noreply@tu-dominio-verificado>
APP_BASE_URL=http://localhost:8081
```

> **Verificacion de correo (Resend):** al registrar un ciudadano se envia un
> correo con un enlace de verificacion. La cuenta nace **inactiva** y no puede
> iniciar sesion hasta confirmar el correo. Si `RESEND_API_KEY` esta vacio, el
> enlace se imprime en consola (modo dev) y no se envia correo.

3. (Opcional) Colocar `firebase-service-account.json` en `src/main/resources/` para habilitar Firebase Storage y notificaciones push. Sin este archivo, las fotos se guardan localmente y las notificaciones se simulan en consola.

4. Ejecutar los scripts SQL en orden:
   - `db/crear-TABLAS.sql` — crea las tablas
   - `db/llenar-TABLAS.sql` — datos de prueba alineados a los JSON del front

## Ejecucion

```bash
mvn spring-boot:run
```

El servidor inicia en `http://localhost:8081`.

## Arquitectura

```
Controller → Service → Repository → PostgreSQL
                ↓
          Firebase Storage (fotos)
          Firebase Cloud Messaging (push)
```

- **Controllers** exponen los endpoints REST
- **Services** contienen la logica de negocio
- **Repositories** acceden a la base de datos via JPA

## Seguridad

Autenticacion via JWT con `HandlerInterceptor`:

- **Rutas publicas** (sin token): `/api/auth/**`, `/api/cuenta`, `/api/cuenta/verificar`, `/api/cuenta/reenviar-verificacion`
- **Rutas protegidas** (requieren `Authorization: Bearer <token>`): todo lo demas bajo `/api/**`

El token se obtiene con `POST /api/auth/login` y tiene validez de 24 horas.

## Endpoints

### Autenticacion (`/api/auth`) — Publico

| Metodo | Ruta | Descripcion | CU |
|---|---|---|---|
| POST | `/api/auth/login` | Iniciar sesion | CU-01 |
| POST | `/api/auth/recuperar-password` | Solicitar recuperacion de contrasena | CU-03 |
| POST | `/api/auth/restablecer-password` | Restablecer contrasena | CU-03 |

### Registro (`/api/cuenta`) — Publico

| Metodo | Ruta | Descripcion | CU |
|---|---|---|---|
| POST | `/api/cuenta` | Registrar ciudadano (queda inactivo + envia correo de verificacion) | CU-02 |
| GET | `/api/cuenta/verificar?token=X` | Verificar correo y activar la cuenta (devuelve HTML) | CU-02 |
| POST | `/api/cuenta/reenviar-verificacion` | Reenviar el correo de verificacion (`{"correo":"..."}`) | CU-02 |

### Reportes (`/api/reportes`) — Protegido

| Metodo | Ruta | Descripcion | CU |
|---|---|---|---|
| POST | `/api/reportes` | Crear reporte | CU-04 |
| GET | `/api/reportes?estado=X&page=N` | Listar reportes con filtro y paginacion | CU-05 |
| GET | `/api/reportes/{id}` | Detalle de un reporte (incluye fotos) | CU-05 |
| GET | `/api/reportes/cuenta/{cuentaId}?page=N` | Reportes de un ciudadano | CU-05 |
| GET | `/api/reportes/mapa?estado=X&tipo=Y` | Reportes para el mapa | CU-05 |
| PUT | `/api/reportes/{id}` | Actualizar reporte | CU-04 |
| POST | `/api/reportes/{id}/fotos` | Subir foto a un reporte | CU-04 |
| PATCH | `/api/reportes/{id}/estado` | Cambiar estado | - |
| POST | `/api/reportes/{id}/rechazar` | Rechazar reporte | CU-06 |
| DELETE | `/api/reportes/{id}` | Eliminar reporte | - |

### Operador (`/api/operador`) — Protegido

| Metodo | Ruta | Descripcion | CU |
|---|---|---|---|
| GET | `/api/operador/reportes?estado=X&page=N` | Cola de reportes por estado | CU-06 |
| POST | `/api/operador/reportes/{id}/aceptar` | Aceptar reporte (PENDIENTE→REVISION) | CU-06 |
| POST | `/api/operador/reportes/{id}/rechazar` | Rechazar reporte (PENDIENTE→RECHAZADO) | CU-06 |

### Asignaciones (`/api/asignaciones`) — Protegido

| Metodo | Ruta | Descripcion | CU |
|---|---|---|---|
| POST | `/api/asignaciones` | Asignar tecnico a reporte | CU-07 |

### Tecnicos (`/api/tecnicos`) — Protegido

| Metodo | Ruta | Descripcion | CU |
|---|---|---|---|
| GET | `/api/tecnicos?page=N` | Listar tecnicos disponibles | CU-07 |
| GET | `/api/tecnicos/{id}` | Obtener tecnico por ID | CU-07 |
| GET | `/api/tecnicos/{id}/reportes?estado=X&page=N` | Reportes asignados al tecnico | CU-08 |
| PATCH | `/api/tecnicos/{id}/reportes/{rid}/completar` | Completar reporte (fotos + comentario → FINALIZADO) | CU-08 |

### Historial (`/api/historial-estados`) — Protegido

| Metodo | Ruta | Descripcion | CU |
|---|---|---|---|
| GET | `/api/historial-estados/reporte/{id}` | Cronologia de cambios de estado | CU-05 |

### Ciudadanos (`/api/ciudadanos`) — Protegido

| Metodo | Ruta | Descripcion | CU |
|---|---|---|---|
| GET | `/api/ciudadanos/{id}` | Obtener ciudadano | - |
| PUT | `/api/ciudadanos/{id}` | Actualizar ciudadano | - |
| DELETE | `/api/ciudadanos/{id}` | Eliminar ciudadano | - |

### Notificaciones (`/api/notificaciones`) — Protegido

| Metodo | Ruta | Descripcion | CU |
|---|---|---|---|
| POST | `/api/notificaciones/registrar-token` | Registrar token FCM del dispositivo | - |
| POST | `/api/notificaciones/enviar-prueba` | Enviar notificacion de prueba | - |

## Flujo de estados

```
PENDIENTE → REVISION    (operador acepta)
PENDIENTE → RECHAZADO   (operador rechaza con motivo)
REVISION  → FINALIZADO  (tecnico completa con fotos + comentario)
```

## Flujo de fotos

```
1. Ciudadano crea reporte     → POST /api/reportes
2. Ciudadano sube fotos       → POST /api/reportes/{id}/fotos (×N, tipo INICIAL)
3. Tecnico completa reporte   → PATCH /api/tecnicos/{id}/reportes/{rid}/completar (fotos FINAL en el body)
```

Las fotos se suben como base64 en el JSON. El backend las sube a Firebase Storage y guarda la URL en la base de datos.

## Notificaciones push

Se disparan automaticamente cuando cambia el estado de un reporte. El ciudadano recibe push via Firebase Cloud Messaging cuando:

- Su reporte es aceptado (→ REVISION)
- Su reporte es rechazado (→ RECHAZADO)
- Le asignan tecnico
- Su reporte es finalizado (→ FINALIZADO)

Requisito: el front debe registrar el token FCM del dispositivo con `POST /api/notificaciones/registrar-token`.

## Estructura del proyecto

```
src/main/java/com/ulima/incidenciaurbana/
├── config/          WebConfig, JwtInterceptor, SecurityBeans, FirebaseConfig
├── controller/      Endpoints REST
├── dto/             Objetos de transferencia (request/response)
├── exception/       Excepciones personalizadas
├── model/           Entidades JPA (Cuenta, Reporte, Foto, etc.)
├── repository/      Interfaces JPA
├── service/         Interfaces de servicios
│   └── impl/        Implementaciones
└── util/            JwtUtil
```

## Datos de prueba

Las cuentas de prueba (password: `123456` para todas):

| ID | Usuario | Rol | Nombre |
|---|---|---|---|
| 1 | ciudadano | CIUDADANO | Ana Quispe |
| 2 | jose | CIUDADANO | Jose Rios |
| 3 | op.maria | OPERADOR_MUNICIPAL | Maria Rojas |
| 4 | tec.carlos | TECNICO | Carlos Mendoza |
| 5 | tec.lucia | TECNICO | Lucia Paredes |
| 6 | tec.maria | TECNICO | Maria Torres |

## Pruebas con REST Client

La carpeta `api-tests/` contiene archivos `.rest` para probar todos los endpoints:

| Archivo | Que prueba |
|---|---|
| `01-auth.rest` | Login de los 3 roles |
| `02-registro.rest` | Registro de ciudadano |
| `03-reportes-ciudadano.rest` | Crear reporte + subir fotos + consultas |
| `04-operador.rest` | Aceptar/rechazar + asignar tecnico |
| `05-tecnico.rest` | Reportes asignados + completar con fotos |
| `06-sin-token.rest` | Verificar seguridad JWT |
| `07-verificacion-correo.rest` | Registro + verificacion de correo (Resend) + login |
