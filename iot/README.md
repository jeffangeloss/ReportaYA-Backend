# ReportaYA IoT — "Poste Cívico anti-inundación" (ESP32)

Integración de un dispositivo ESP32 al sistema de incidencias ReportaYA con un **loop físico
bidireccional**: el poste **sensa** una incidencia → **la reporta solo** a la nube → y **reacciona
físicamente** (LEDs, barrera con servos, LCD, buzzer) según cómo la municipalidad la atiende.

Es un prototipo de demostración académica. El firmware vive en
[`reportaya_poste/reportaya_poste.ino`](reportaya_poste/reportaya_poste.ino) y la cuenta de servicio
que usa se crea con [`../db/seed-cuenta-iot.sql`](../db/seed-cuenta-iot.sql).

---

## Cómo funciona

```
   Sensores  ──►  ESP32  ──POST /api/reportes──►  Backend Spring Boot (8081)
                   │  ▲                                │
   LEDs/LCD/   ◄───┘  └──GET /api/reportes/{id} (poll)─┘
   servos/buzzer                     (operador/técnico cambian el estado)
```

| Estado del reporte | LED | Barrera (servos) | LCD | Buzzer |
|---|---|---|---|---|
| PENDIENTE  | Rojo fijo       | Cerrada | `#id / PENDIENTE`   | 1 beep |
| REVISION   | Amarillo fijo   | Cerrada | `#id / EN REVISION` | 2 beeps |
| FINALIZADO | Verde fijo      | **Abierta** | `#id / RESUELTO :)` | melodía |
| RECHAZADO  | Rojo parpadeo   | Abierta | `#id / RECHAZADO`   | tono grave |

**Disparo del reporte:** lluvia (FC-37) **y** suelo saturado (FC-28) sostenidos ~5 s → crea un reporte
`INFRAESTRUCTURA` ("Riesgo de anegamiento en vía pública") con el GPS fijo del poste.

---

## Hardware y pines

| Componente | Pin | Notas |
|---|---|---|
| LED Rojo / Amarillo / Verde | GPIO16 / 17 / 18 | Estado del reporte |
| Servo 1 / Servo 2 (barrera) | GPIO19 / GPIO23 | Servo 2 montado invertido |
| LCD I2C 16×2 | SDA=21, SCL=22 (0x27) | Bus I2C compartido |
| FC-28 humedad de suelo | GPIO34 (ADC) | 0–4095; DRY≈3300, WET≈1300 |
| FC-37 lluvia | GPIO26 | `INPUT_PULLUP`, LOW = lluvia |
| Buzzer | GPIO25 | Opcional |

> **Alimenta los servos con una fuente 5 V externa** y GND común con el ESP32. Alimentarlos del pin
> 3V3 provoca caídas de tensión que reinician la placa al mover la barrera.

---

## Requisitos (Arduino IDE 2.x)

### 1) Soporte de placas
Boards Manager → instalar **`esp32`** de *Espressif Systems*. Luego selecciona tu placa
(p. ej. "ESP32 Dev Module") y el puerto COM correspondiente.

### 2) Librerías (Library Manager, `Ctrl+Shift+I`)

| Librería | Autor | Versión |
|---|---|---|
| **ArduinoJson** | Benoit Blanchon | **v7.x** (ver aviso abajo) |
| **LiquidCrystal I2C** | Frank de Brabander | cualquiera |
| **ESP32Servo** | Kevin Harrington / John K. Bennett | cualquiera |

> ⚠️ **ArduinoJson tiene que ser v7.** El firmware usa la API `JsonDocument`, introducida en la v7.
> Si el Library Manager te instala una v6.x (que usa `StaticJsonDocument` / `DynamicJsonDocument`),
> **el sketch no compila**. Verifica la versión en el desplegable antes de darle a *Install*.

> ℹ️ Se especifican los autores a propósito: hay varios *forks* con el mismo nombre. El de
> `LiquidCrystal I2C` de Frank de Brabander es el que expone `lcd.init()`, que es el que usa el sketch.

### 3) Lo que NO debes instalar
`WiFi.h`, `HTTPClient.h` y `Wire.h` **ya vienen incluidas** con el core ESP32. Si las buscas en el
Library Manager encontrarás librerías homónimas para otras placas (Arduino Uno WiFi, etc.) que
generan conflictos de compilación.

---

## Puesta en marcha

### 1) Backend + base de datos
1. Levanta PostgreSQL con el esquema (`db/crear-TABLAS.sql`) y datos (`db/llenar-TABLAS.sql`).
2. Crea la cuenta de servicio del dispositivo:
   ```
   psql -d <tu_bd> -f db/seed-cuenta-iot.sql
   ```
   Anota el `svc_cuenta_id` que imprime (es el `SVC_CUENTA_ID` del firmware). Usuario `poste_iot`,
   contraseña `poste123` (se migra a BCrypt sola en el primer login).
3. Levanta el backend en el puerto 8081 (`./mvnw spring-boot:run`).

### 2) Red (crítico)
- El ESP32 y el laptop del backend deben estar en el **mismo WiFi/hotspot**.
- Averigua la **IP LAN del laptop** (`ipconfig` en Windows / `ifconfig` en macOS/Linux), p. ej.
  `192.168.1.100`. Esa es la que va en `BASE_URL` — **nunca** `localhost` ni `127.0.0.1`.
- Permite el **puerto 8081 en el firewall** del laptop (conexiones entrantes).
- Verifícalo desde el celular en el mismo WiFi abriendo `http://<IP_LAN>:8081/api/reportes/mapa`
  (pide token, pero si responde algo distinto de "timeout" ya hay alcance de red).

### 3) Firmware
Edita el bloque **CONFIG** en `reportaya_poste.ino`:
```cpp
const char*  WIFI_SSID     = "jefferson";
const char*  WIFI_PASS     = "gabyjeff1617!!!";
String       BASE_URL      = "http://192.168.1.100:8081";  // IP LAN del laptop
long         SVC_CUENTA_ID = 7;                            // id del seed SQL
const double POSTE_LAT     = -12.08640;
const double POSTE_LNG     = -77.05000;
```
Compila y flashea. En el Monitor Serie (115200) verás `login OK` y la IP asignada.

---

## Guion de la demo (2–3 min)

Dos roles: **físico** (opera el sensor) y **municipalidad** (laptop con curl/Postman). El poste arranca
en verde, barrera abierta, LCD "Vigilando…". Para reacciones rápidas, `POLL_MS` ya está en ~7 s.

1. **Disparo físico:** moja el sensor de lluvia (FC-37) y humedece el de suelo (FC-28). Tras ~5 s el
   poste **crea el reporte**: LED **rojo**, LCD `#123 / PENDIENTE`, **baja la barrera**, beep.
2. **Prueba de nube:** confirma que llegó (reemplaza `<TOKEN>` por uno de login, y `123` por el id):
   ```bash
   # login para obtener un token (rol municipalidad)
   curl -s -X POST http://<IP_LAN>:8081/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"usuario":"op.maria","password":"123456"}'

   # ver el reporte recién creado
   curl -s http://<IP_LAN>:8081/api/reportes/123 -H "Authorization: Bearer <TOKEN>"
   ```
3. **La municipalidad atiende** → pasa a REVISION (al siguiente poll el poste vira a **amarillo**):
   ```bash
   curl -s -X PATCH "http://<IP_LAN>:8081/api/reportes/123/estado?nuevoEstado=REVISION" \
     -H "Authorization: Bearer <TOKEN>"
   ```
4. **Resolución** → FINALIZADO (el poste vira a **verde**, **abre la barrera**, melodía):
   ```bash
   curl -s -X PATCH "http://<IP_LAN>:8081/api/reportes/123/estado?nuevoEstado=FINALIZADO" \
     -H "Authorization: Bearer <TOKEN>"
   ```
5. **Cierre:** opcional, muestra las transiciones:
   ```bash
   curl -s http://<IP_LAN>:8081/api/historial-estados/reporte/123 -H "Authorization: Bearer <TOKEN>"
   ```

> El `PATCH /estado` es la vía simple para la demo (no necesita IDs de operador/técnico). Si quieres el
> flujo realista, usa `POST /api/operador/reportes/{id}/aceptar` y
> `PATCH /api/tecnicos/{id}/reportes/{reporteId}/completar` con cuentas OPERADOR/TÉCNICO.

---

## Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| El ESP32 no alcanza el backend | Hotspot con *aislamiento de clientes* | Prueba `ping <IP_LAN>` desde otro equipo; usa un router/AP casero o el hotspot del laptop |
| `connection refused` / timeout | `localhost` en `BASE_URL`, o firewall | Usa la IP LAN real; abre el puerto 8081 entrante |
| `login FALLO (HTTP 403)` | Cuenta inactiva | El seed la crea con `activo=true`; re-ejecútalo |
| `HTTP 401` a mitad de demo | Token expiró (24 h) | El firmware re-loguea solo; si persiste, revisa credenciales |
| Se crean muchos reportes | Sensor oscilando | Ya hay histéresis + confirmación 5 s + un reporte activo a la vez |
| La placa se reinicia al mover la barrera | Servos alimentados del 3V3 | Fuente 5 V externa con GND común |
| Texto cortado en la LCD | 16 columnas | L1 = `#id`, L2 = estado (ya se ajusta); opcional OLED SSD1306 (0x3C) |

---

## Nota de seguridad (deuda técnica conocida)

- La demo usa **HTTP plano** en LAN y credenciales en el firmware: aceptable para prototipo, no para
  producción.
- El backend toma el `cuentaId` del **body** (no del token) al crear reportes, así que un cliente
  autenticado podría reportar como otra cuenta. Mejora futura: un endpoint `POST /api/iot/reportes`
  que derive el `cuentaId` del token.
