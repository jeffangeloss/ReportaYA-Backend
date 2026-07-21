/*
  ================================================================
  reportaya_poste.ino  —  "Poste Civico anti-inundacion"
  Integracion IoT (ESP32) <-> ReportaYA — loop fisico bidireccional
  ================================================================

  QUE HACE
    1) SENSA: lluvia (FC-37) + suelo saturado (FC-28). Si ambas señales se
       sostienen unos segundos, deduce riesgo de anegamiento.
    2) REPORTA: se loguea en el backend (JWT) y crea un reporte automatico
       (POST /api/reportes, tipo INFRAESTRUCTURA) con el GPS fijo del poste.
    3) REACCIONA: sondea el estado del reporte (GET /api/reportes/{id}) y lo
       refleja en fisico segun lo atiende la municipalidad:
         PENDIENTE  -> LED rojo,     barrera CERRADA, 1 beep
         REVISION   -> LED amarillo,  barrera CERRADA, 2 beeps
         FINALIZADO -> LED verde,     barrera ABIERTA, melodia
         RECHAZADO  -> LED rojo parpadeo, barrera ABIERTA, tono grave

  ARQUITECTURA
    - Todo corre con temporizadores millis() — SIN delay() — para no bloquear.
    - Maquina de estados del dispositivo:
        IDLE -> CONFIRMANDO -> REPORTADO/SEGUIMIENTO -> CERRADO -> IDLE

  LIBRERIAS (Library Manager)
    - ArduinoJson        (v7.x)
    - LiquidCrystal_I2C
    - ESP32Servo
    (WiFi.h y HTTPClient.h vienen con el core ESP32)

  ANTES DE FLASHEAR — edita el bloque CONFIG:
    - BASE_URL con la IP LAN del laptop del backend (NO localhost).
    - SVC_CUENTA_ID con el id que devuelve db/seed-cuenta-iot.sql.
    - POSTE_LAT / POSTE_LNG con la esquina que quieras representar.

  Este sketch es autocontenido para la demo. Si quieres conservar tu
  semaforo/servos originales, copia las funciones ry_* y la FSM a tu
  Inicio_Semaforos.ino y alterna con un flag de modo (NORMAL/REPORTAYA).
*/

#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <ESP32Servo.h>

// ======================= CONFIG (EDITAR) =======================
const char*  WIFI_SSID      = "TU_RED_WIFI";       // <-- SSID de tu red (hotspot/router)
const char*  WIFI_PASS      = "TU_PASSWORD_WIFI";  // <-- contrasena de esa red
String       BASE_URL       = "https://reportaya-backend.onrender.com";  // <-- backend DESPLEGADO (HTTPS)
// El poste apunta al backend publico en Render (via el hotspot con internet),
// ya no depende de la IP local del laptop. Al ser HTTPS se usa WiFiClientSecure
// con setInsecure() (demo: no valida el certificado TLS de Render).
const char*  SVC_USER       = "poste_iot";
const char*  SVC_PASS       = "poste123";
long         SVC_CUENTA_ID  = 7;                             // <-- id de poste_iot en Neon (el login lo corrige igual)
const double POSTE_LAT      = -12.08640;
const double POSTE_LNG      = -77.05000;
const char*  POSTE_DIR      = "Esquina Av. Salaverry, Jesus Maria (demo)";

// Tiempos (ms). Para la demo baja POLL_MS a ~6000-7000 para reacciones rapidas.
const uint32_t CONFIRM_MS   = 5000;     // condicion sostenida antes de reportar
const uint32_t POLL_MS      = 7000;     // cada cuanto se consulta el estado
const uint32_t COOLDOWN_MS  = 15000;    // exhibicion del estado terminal antes de resetear
const uint32_t SENSE_MS     = 1000;     // cada cuanto se leen los sensores
const uint32_t WIFI_RETRY_MS= 10000;    // reintento de reconexion WiFi

// Umbrales de histeresis del FC-28 (0-4095; seco pega ~4095, humedo baja).
// Umbrales rebajados: dispara con humedad moderada (valor < 2500) en vez de
// exigir suelo empapado. Entra a "saturado" si cae por debajo de IN; sale
// solo si supera OUT (banda muerta anti-oscilacion).
const int SOIL_WET_IN  = 2500;
const int SOIL_WET_OUT = 3000;
// ===============================================================

// -------------------------- PINES ------------------------------
#define LED_ROJO     16
#define LED_AMARILLO 17
#define LED_VERDE    18
#define SERVO1_PIN   19
#define SERVO2_PIN   23
#define FC28_PIN     34    // humedad de suelo (analogico, solo entrada)
#define FC37_PIN     26    // lluvia (digital, LOW = lluvia)
#define BUZZER_PIN   25    // buzzer (opcional)

// Posiciones de la barrera (servos)
const int SERVO_CLOSE = 0;    // vía cerrada (riesgo)
const int SERVO_OPEN  = 90;   // vía despejada

// -------------------------- OBJETOS ----------------------------
LiquidCrystal_I2C lcd(0x27, 16, 2);
Servo servo1;   // barrera derecha
Servo servo2;   // barrera izquierda (montaje invertido)

// ---------------------------- TIPOS ----------------------------
// IMPORTANTE: estas structs van ANTES de la primera funcion del sketch.
// El IDE de Arduino genera prototipos automaticos y los inserta justo antes
// de la primera funcion; si las structs se declararan mas abajo, esos
// prototipos las referenciarian sin conocerlas ("does not name a type").
struct Tono      { int freq; uint32_t dur; };            // freq 0 = silencio
struct Deteccion { bool activa; String titulo; String desc; };

// -------------------- ESTADOS DEL DOMINIO ----------------------
enum Estado { E_DESCONOCIDO, E_PENDIENTE, E_REVISION, E_FINALIZADO, E_RECHAZADO };

Estado parseEstado(const char* s) {
  if (!s) return E_DESCONOCIDO;
  if (!strcmp(s, "PENDIENTE"))  return E_PENDIENTE;
  if (!strcmp(s, "REVISION"))   return E_REVISION;
  if (!strcmp(s, "FINALIZADO")) return E_FINALIZADO;
  if (!strcmp(s, "RECHAZADO"))  return E_RECHAZADO;
  return E_DESCONOCIDO;
}

// ----------------- MAQUINA DE ESTADOS DEL POSTE ----------------
enum Fase { IDLE, CONFIRMANDO, SEGUIMIENTO, CERRADO };
Fase   fase          = IDLE;
long   reporteId     = -1;
Estado estadoActual  = E_DESCONOCIDO;

// Disparo forzado para demo: se activa enviando 'r' por el Monitor Serie.
// Simula la deteccion (crea un reporte real) sin mojar los sensores.
bool   forzarReporte = false;

// -------------------- CREDENCIAL EN MEMORIA --------------------
String  jwtToken     = "";
bool    autenticado  = false;

// -------------------------- TIMERS -----------------------------
uint32_t tSense = 0, tPoll = 0, tCondStart = 0, tTerminal = 0, tWifi = 0;

// ------------------ HISTERESIS DEL SUELO -----------------------
bool sueloSaturado = false;

// ------------------ DIAGNOSTICO DE SENSORES --------------------
// Imprime las lecturas crudas por Serial para calibrar los umbrales.
const bool     DEBUG_SENSORES = true;
const uint32_t DEBUG_MS       = 2000;
uint32_t       tDebug         = 0;

// ===============================================================
//  BUZZER no bloqueante (secuenciador de tonos)
// ===============================================================
Tono     buzSeq[8];
int      buzLen = 0, buzIdx = 0;
uint32_t buzStepStart = 0;
bool     buzPlaying = false;

void buzApplyStep() {                       // aplica el tono del paso buzIdx
  if (buzSeq[buzIdx].freq > 0) tone(BUZZER_PIN, buzSeq[buzIdx].freq);
  else                         noTone(BUZZER_PIN);
  buzStepStart = millis();
}
void buzzerPlay(const Tono* seq, int len) { // arranca una secuencia (no bloqueante)
  buzLen = min(len, 8);
  for (int i = 0; i < buzLen; i++) buzSeq[i] = seq[i];
  buzIdx = 0; buzPlaying = (buzLen > 0);
  if (buzPlaying) buzApplyStep();
}
void buzzerTick() {                         // avanza la secuencia sin delay()
  if (!buzPlaying) return;
  if (millis() - buzStepStart >= buzSeq[buzIdx].dur) {
    buzIdx++;
    if (buzIdx >= buzLen) { noTone(BUZZER_PIN); buzPlaying = false; return; }
    buzApplyStep();
  }
}
void beep1()    { static const Tono s[] = {{2000,120}};                                  buzzerPlay(s,1); }
void beep2()    { static const Tono s[] = {{2000,100},{0,80},{2000,100}};                buzzerPlay(s,3); }
void melodia()  { static const Tono s[] = {{1046,120},{1318,120},{1568,120},{2093,220}}; buzzerPlay(s,4); }
void tonoGrave(){ static const Tono s[] = {{700,180},{0,60},{500,260}};                  buzzerPlay(s,3); }

// ===============================================================
//  ACTUADORES (LEDs, servos, LCD)
// ===============================================================
void setLeds(bool r, bool a, bool v) {
  digitalWrite(LED_ROJO, r); digitalWrite(LED_AMARILLO, a); digitalWrite(LED_VERDE, v);
}
void setBarrera(int ang) { servo1.write(ang); servo2.write(SERVO_OPEN + SERVO_CLOSE - ang); }

void lcdLinea(uint8_t fila, const String& txt) {
  lcd.setCursor(0, fila);
  String t = txt; while (t.length() < 16) t += ' ';
  lcd.print(t.substring(0, 16));
}

// Parpadeo del LED rojo para RECHAZADO (no bloqueante)
bool     blinkRojo = false;
uint32_t tBlink = 0;
bool     blinkOn = false;
void actTick() {
  if (!blinkRojo) return;
  if (millis() - tBlink >= 400) {
    tBlink = millis(); blinkOn = !blinkOn;
    digitalWrite(LED_ROJO, blinkOn);
  }
}

const char* nombreEstado(Estado e) {
  switch (e) {
    case E_PENDIENTE:  return "PENDIENTE";
    case E_REVISION:   return "REVISION";
    case E_FINALIZADO: return "FINALIZADO";
    case E_RECHAZADO:  return "RECHAZADO";
    default:           return "?";
  }
}

void actEstado(Estado e) {
  blinkRojo = false;
  lcdLinea(0, "ReportaYA #" + String(reporteId));
  Serial.printf("[ACT] reporte #%ld -> %s\n", reporteId, nombreEstado(e));
  switch (e) {
    case E_PENDIENTE:
      setLeds(1,0,0); setBarrera(SERVO_CLOSE); lcdLinea(1, "PENDIENTE"); beep1(); break;
    case E_REVISION:
      setLeds(0,1,0); setBarrera(SERVO_CLOSE); lcdLinea(1, "EN REVISION"); beep2(); break;
    case E_FINALIZADO:
      setLeds(0,0,1); setBarrera(SERVO_OPEN);  lcdLinea(1, "RESUELTO :)"); melodia(); break;
    case E_RECHAZADO:
      setLeds(0,0,0); blinkRojo = true; setBarrera(SERVO_OPEN); lcdLinea(1, "RECHAZADO"); tonoGrave(); break;
    default:
      setLeds(0,0,0); break;
  }
}

void modoIdle() {
  setLeds(0,0,1);            // verde suave = todo en orden / vigilando
  setBarrera(SERVO_OPEN);
  lcdLinea(0, "ReportaYA IoT");
  lcdLinea(1, "Vigilando...");
}

// ===============================================================
//  CLIENTE REST ReportaYA
// ===============================================================
bool ry_login() {
  WiFiClientSecure client;
  client.setInsecure();                 // demo: no valida el cert TLS de Render
  HTTPClient http;
  http.setConnectTimeout(10000); http.setTimeout(20000);  // TLS + posible cold start de Render
  if (!http.begin(client, BASE_URL + "/api/auth/login")) return false;
  http.addHeader("Content-Type", "application/json");

  JsonDocument body;
  body["usuario"]  = SVC_USER;
  body["password"] = SVC_PASS;
  String out; serializeJson(body, out);

  int code = http.POST(out);
  bool ok = false;
  if (code == 200) {
    JsonDocument r;
    if (deserializeJson(r, http.getString()) == DeserializationError::Ok) {
      jwtToken = r["token"].as<String>();
      // por si el seed usara otro id, tomamos el cuentaId real del login:
      if (!r["cuentaId"].isNull()) SVC_CUENTA_ID = r["cuentaId"].as<long>();
      ok = jwtToken.length() > 0;
    }
  }
  autenticado = ok;
  http.end();
  Serial.printf("[RY] login %s (HTTP %d)\n", ok ? "OK" : "FALLO", code);
  return ok;
}

bool ry_ensureAuth() { return autenticado || ry_login(); }

// Reintenta el login en segundo plano si el arranque fallo (backend caido, IP
// equivocada, red aun no lista). Sin esto el poste quedaba sin sesion hasta que
// un sensor disparara, y el fallo pasaba desapercibido.
const uint32_t AUTH_RETRY_MS = 15000;
uint32_t tAuth = 0;
void auth_tick() {
  if (autenticado) return;
  if (tAuth != 0 && millis() - tAuth < AUTH_RETRY_MS) return;
  tAuth = millis();
  ry_login();
}

// Devuelve el id del reporte creado, o -1
long ry_crearReporte(const char* tipo, const String& titulo, const String& desc) {
  if (!ry_ensureAuth()) return -1;

  WiFiClientSecure client;
  client.setInsecure();
  HTTPClient http;
  http.setConnectTimeout(10000); http.setTimeout(20000);
  if (!http.begin(client, BASE_URL + "/api/reportes")) return -1;
  http.addHeader("Content-Type", "application/json");
  http.addHeader("Authorization", "Bearer " + jwtToken);

  JsonDocument body;
  body["titulo"]       = titulo;
  body["descripcion"]  = desc;
  body["cuentaId"]     = SVC_CUENTA_ID;      // el backend lo toma del BODY, no del token
  body["tipoProblema"] = tipo;
  JsonObject u = body["ubicacion"].to<JsonObject>();
  u["latitud"]   = POSTE_LAT;
  u["longitud"]  = POSTE_LNG;
  u["direccion"] = POSTE_DIR;
  String out; serializeJson(body, out);

  int code = http.POST(out);
  long id = -1;
  if (code == 401) {                          // token vencido -> re-login y 1 reintento
    http.end(); autenticado = false;
    if (ry_login()) return ry_crearReporte(tipo, titulo, desc);
    return -1;
  }
  if (code == 200 || code == 201) {
    JsonDocument r;
    if (deserializeJson(r, http.getString()) == DeserializationError::Ok)
      id = r["id"] | -1L;
  }
  http.end();
  Serial.printf("[RY] crearReporte HTTP %d -> id=%ld\n", code, id);
  return id;
}

// Devuelve el estado actual del reporte, o E_DESCONOCIDO
Estado ry_consultarEstado(long id) {
  if (id < 0 || !ry_ensureAuth()) return E_DESCONOCIDO;

  WiFiClientSecure client;
  client.setInsecure();
  HTTPClient http;
  http.setConnectTimeout(10000); http.setTimeout(20000);
  if (!http.begin(client, BASE_URL + "/api/reportes/" + String(id))) return E_DESCONOCIDO;
  http.addHeader("Authorization", "Bearer " + jwtToken);

  int code = http.GET();
  Estado e = E_DESCONOCIDO;
  if (code == 401) { http.end(); autenticado = false; ry_login(); return E_DESCONOCIDO; }
  if (code == 200) {
    JsonDocument r;
    if (deserializeJson(r, http.getString()) == DeserializationError::Ok)
      e = parseEstado(r["estado"] | "");
  }
  http.end();
  return e;
}

// ===============================================================
//  SENSADO con histeresis + confirmacion temporal
// ===============================================================
Deteccion sensores_evaluar() {
  bool lluvia = (digitalRead(FC37_PIN) == LOW);
  int  soil   = analogRead(FC28_PIN);

  // Histeresis del suelo (banda muerta para evitar oscilaciones)
  if (!sueloSaturado && soil < SOIL_WET_IN)  sueloSaturado = true;
  if ( sueloSaturado && soil > SOIL_WET_OUT) sueloSaturado = false;

  bool cond = lluvia && sueloSaturado;

  // --- Diagnostico / calibracion (poner DEBUG_SENSORES=false para silenciarlo) ---
  if (DEBUG_SENSORES && millis() - tDebug >= DEBUG_MS) {
    tDebug = millis();
    Serial.printf("[SENS] FC37(lluvia)=%s | FC28(suelo)=%4d %s | cond=%s | confirmando=%lums\n",
                  lluvia ? "SI" : "no",
                  soil,
                  sueloSaturado ? "SATURADO" : "seco",
                  cond ? "SI" : "no",
                  (unsigned long)(tCondStart == 0 ? 0 : millis() - tCondStart));
  }

  Deteccion d; d.activa = false;
  if (cond) {
    if (tCondStart == 0) tCondStart = millis();        // arranca el reloj de confirmacion
    else if (millis() - tCondStart >= CONFIRM_MS) {
      d.activa = true;
      d.titulo = "Riesgo de anegamiento en via publica";
      d.desc   = "Deteccion automatica: lluvia activa y suelo saturado (lectura FC-28="
                 + String(soil) + "). Punto: " + String(POSTE_DIR) + ".";
    }
  } else {
    tCondStart = 0;                                    // se corto la condicion -> reinicia
  }
  return d;
}

// ===============================================================
//  ORQUESTACION (FSM del dispositivo)
// ===============================================================
void reportaya_tick() {
  uint32_t now = millis();

  // 1) Sensado periodico (solo mientras vigilamos, no durante el seguimiento)
  if ((fase == IDLE || fase == CONFIRMANDO) && now - tSense >= SENSE_MS) {
    tSense = now;
    Deteccion d = sensores_evaluar();
    if ((d.activa || forzarReporte) && reporteId < 0) {
      // Deteccion real (sensores) o forzada por comando 'r' (demo).
      String tit = d.activa ? d.titulo : "Riesgo de anegamiento (demo)";
      String des = d.activa ? d.desc
                            : "Deteccion simulada desde el poste para demostracion.";
      forzarReporte = false;
      long id = ry_crearReporte("INFRAESTRUCTURA", tit, des);
      if (id > 0) {
        reporteId = id; estadoActual = E_PENDIENTE; fase = SEGUIMIENTO;
        actEstado(E_PENDIENTE); tPoll = now;
      }
    } else if (tCondStart != 0) {
      // Condicion presente pero aun no confirmada -> "Detectando..."
      if (fase != CONFIRMANDO) { fase = CONFIRMANDO; lcdLinea(0, "ReportaYA IoT"); lcdLinea(1, "Detectando..."); }
    } else {
      // Sin condicion -> volver a vigilar
      if (fase != IDLE) { fase = IDLE; modoIdle(); }
    }
  }

  // 2) Seguimiento del estado (polling)
  if (fase == SEGUIMIENTO && now - tPoll >= POLL_MS) {
    tPoll = now;
    Estado e = ry_consultarEstado(reporteId);
    if (e != E_DESCONOCIDO && e != estadoActual) {
      estadoActual = e;
      actEstado(e);
      if (e == E_FINALIZADO || e == E_RECHAZADO) { fase = CERRADO; tTerminal = now; }
    }
  }

  // 3) Estado terminal: exhibe un rato y vuelve a vigilar
  if (fase == CERRADO && now - tTerminal >= COOLDOWN_MS) {
    reporteId = -1; estadoActual = E_DESCONOCIDO; blinkRojo = false;
    tCondStart = 0; sueloSaturado = false; fase = IDLE;
    modoIdle();
  }
}

// ===============================================================
//  WIFI
// ===============================================================
bool wifiAnunciada = false;

void wifi_tick() {
  if (WiFi.status() == WL_CONNECTED) {
    if (!wifiAnunciada) {                     // avisa una sola vez al conectar
      wifiAnunciada = true;
      Serial.print("[WiFi] conectado. IP: "); Serial.println(WiFi.localIP());
      Serial.print("[RY] backend: "); Serial.println(BASE_URL);
    }
    return;
  }
  wifiAnunciada = false;
  if (tWifi != 0 && millis() - tWifi < WIFI_RETRY_MS) return;
  tWifi = millis();
  Serial.print("[WiFi] conectando a "); Serial.println(WIFI_SSID);
  // disconnect(true) antes de begin(): sin esto, reintentar mientras ya hay un
  // intento en curso provoca "sta is connecting, cannot set config".
  WiFi.disconnect(true);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
}

// ===============================================================
//  SETUP / LOOP
// ===============================================================
void setup() {
  Serial.begin(115200);

  pinMode(LED_ROJO, OUTPUT); pinMode(LED_AMARILLO, OUTPUT); pinMode(LED_VERDE, OUTPUT);
  pinMode(FC37_PIN, INPUT_PULLUP);
  pinMode(BUZZER_PIN, OUTPUT);
  analogReadResolution(12);                 // 0-4095 para el FC-28

  Wire.begin();                             // SDA=21, SCL=22 por defecto en ESP32
  lcd.init(); lcd.backlight();
  lcdLinea(0, "ReportaYA IoT"); lcdLinea(1, "Iniciando...");

  servo1.setPeriodHertz(50); servo2.setPeriodHertz(50);
  servo1.attach(SERVO1_PIN, 500, 2400);
  servo2.attach(SERVO2_PIN, 500, 2400);
  setBarrera(SERVO_OPEN);

  wifi_tick();
  uint32_t t0 = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - t0 < 8000) { delay(200); }  // solo en boot
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("[WiFi] IP: "); Serial.println(WiFi.localIP());
    ry_login();
  }
  modoIdle();
}

// Comandos por Monitor Serie: 'r' fuerza un reporte (demo, sin mojar sensores).
void serial_tick() {
  while (Serial.available()) {
    char c = Serial.read();
    if (c == 'r' || c == 'R') {
      forzarReporte = true;
      Serial.println("[CMD] Reporte forzado solicitado");
    }
  }
}

void loop() {
  serial_tick();
  wifi_tick();
  buzzerTick();
  actTick();
  if (WiFi.status() == WL_CONNECTED) { auth_tick(); reportaya_tick(); }
}
