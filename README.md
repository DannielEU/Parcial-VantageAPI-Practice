# Stock API Service - Documentación Completa

Aplicación Spring Boot que proporciona una API REST para consultar datos históricos de acciones desde proveedores externos como **Alpha Vantage**.

## 📋 Índice

1. [Estructura del Proyecto](#estructura-del-proyecto)
2. [Características](#características)
3. [Requisitos Previos](#requisitos-previos)
4. [Instalación y Configuración](#instalación-y-configuración)
5. [Uso de la API](#uso-de-la-api)
6. [Arquitectura y Patrones](#arquitectura-y-patrones)
7. [Cómo Conectar con API Externa](#cómo-conectar-con-api-externa)
8. [Agregar Nuevo Proveedor](#agregar-nuevo-proveedor)
9. [Troubleshooting](#troubleshooting)

---

## 📁 Estructura del Proyecto

```
src/main/java/com/api/parcial/
├── ApiServiceApplication.java      # Punto de entrada de la aplicación
├── controller/
│   └── StockController.java        # Endpoints REST
├── service/
│   └── StockFacadeService.java     # Lógica de negocio (Facade Pattern)
├── provider/
│   ├── StockProvider.java          # Interfaz del proveedor (Strategy Pattern)
│   └── AlphaVantageProvider.java   # Implementación para Alpha Vantage
├── model/
│   └── StockResponse.java          # DTO de respuesta
├── cache/
│   └── StockCache.java             # Cache en memoria
└── config/
    ├── CorsConfig.java             # Configuración CORS
    └── ProviderConfig.java         # Inyección de dependencias

src/main/resources/
└── application.properties           # Configuración de la aplicación
```

### 📝 Descripción de Componentes

| Componente | Responsabilidad | Patrón |
|-----------|-----------------|--------|
| **Controller** | Expone endpoints REST | MVC |
| **FacadeService** | Coordina acceso a datos y cache | Facade Pattern |
| **Provider** | Obtiene datos de APIs externas | Strategy Pattern |
| **Cache** | Evita llamadas repetidas | Cache Pattern |
| **Model** | Representa los datos | DTO |

---

## ✨ Características

- ✅ **Endpoints REST** para consultar acciones en 4 intervalos: Diario, Semanal, Mensual, Intradiario
- ✅ **Cache en memoria** para optimizar llamadas a APIs externas
- ✅ **Manejo de errores** robusto con logs detallados
- ✅ **CORS habilitado** para acceso desde frontends
- ✅ **Arquitectura escalable** con patrón Strategy para múltiples proveedores
- ✅ **Inyección de dependencias** para código testeable
- ✅ **Configuración externa** de credenciales (API keys)

---

## 🔧 Requisitos Previos

- **Java 17** o superior
- **Maven** 3.6+
- **API Key de Alpha Vantage** (gratuita en https://www.alphavantage.co/)

### Obtener API Key:
1. Ve a https://www.alphavantage.co/
2. Completa el formulario para obtener una clave gratuita
3. Verifica tu email
4. Copiar la clave

---

## 🚀 Instalación y Configuración

### 1. Clonar/Descargar el proyecto
```bash
cd c:\Users\Danie\Documents\ARSW\Parcial-API\demo
```

### 2. Configurar API Key

#### Opción A: Variables de Entorno (Recomendado)
```bash
# Windows PowerShell
$env:ALPHAVANTAGE_API_KEY = "tu_clave_aqui"

# Windows CMD
set ALPHAVANTAGE_API_KEY=tu_clave_aqui

# Linux/Mac
export ALPHAVANTAGE_API_KEY=tu_clave_aqui
```

#### Opción B: application.properties
Edita `src/main/resources/application.properties`:
```properties
alphavantage.api.key=tu_clave_aqui_sin_spaces
```

#### Opción C: Archivo application-local.properties (Git-ignored)
1. Crea `src/main/resources/application-local.properties`
2. Agrega: `alphavantage.api.key=tu_clave_aqui`
3. Ejecuta con: `mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"`

### 3. Compilar el proyecto
```bash
mvn clean install
```

### 4. Ejecutar la aplicación
```bash
mvn spring-boot:run
```

La aplicación estará disponible en: **http://localhost:8080**

---

## 📡 Uso de la API

### Endpoints Disponibles

#### 1. Precios Diarios
```http
GET /stock/daily?symbol=AAPL
```
**Ejemplo:**
```bash
curl http://localhost:8080/stock/daily?symbol=AAPL
```

**Respuesta:**
```json
{
  "symbol": "AAPL",
  "interval": "DAILY",
  "prices": {
    "2024-02-22": 182.45,
    "2024-02-21": 181.20,
    "2024-02-20": 180.50
  }
}
```

#### 2. Precios Intradiarios (cada 5 minutos)
```http
GET /stock/intraday?symbol=GOOGL
```

#### 3. Precios Semanales
```http
GET /stock/weekly?symbol=MSFT
```

#### 4. Precios Mensuales
```http
GET /stock/monthly?symbol=TSLA
```

### Parámetros

| Parámetro | Requerido | Descripción | Ejemplos |
|-----------|-----------|------------|----------|
| `symbol` | Sí | Símbolo del ticker | AAPL, GOOGL, MSFT, TSLA |

### Códigos de Respuesta

| Código | Significado |
|--------|------------|
| 200 | Éxito - Datos obtenidos |
| 400 | Error de validación |
| 500 | Error interno del servidor |

---

## 🏗️ Arquitectura y Patrones

### 1. **Facade Pattern** (StockFacadeService)
Simplifica la interfaz hacia los clientes ocultando la complejidad interna:
- Coordina Provider y Cache
- Cliente solo interactúa con una clase

```
Cliente → Facade → [Provider, Cache]
```

### 2. **Strategy Pattern** (StockProvider Interface)
Permite intercambiar implementaciones sin cambiar el código cliente:
- Define interfaz `StockProvider`
- Múltiples implementaciones (AlphaVantage, Yahoo, etc)
- Runtime switch de proveedores

```
            ┌─ AlphaVantageProvider
StockProvider─┤─ YahooFinanceProvider
            └─ OtherProvider
```

### 3. **Dependency Injection** (Spring)
- ProviderConfig define qué implementación usar
- Spring inyecta automáticamente en FacadeService
- Fácil de testear con mocks

### 4. **Caching Pattern**
- `StockCache` evita llamadas repetidas
- Thread-safe con `ConcurrentHashMap`
- Mejora performance en 10-100x

### Flujo de Datos

```
Request HTTP
    ↓
StockController.getDaily(symbol)
    ↓
StockFacadeService.getDaily(symbol)
    ↓
    ├→ StockCache.getOrCompute()
    │   ├→ ¿Exists in cache? → Return
    │   └→ No existe → Call provider
    │
    └→ AlphaVantageProvider.getDaily()
        ├→ Build URL (con API key)
        ├→ HTTP REST call
        ├→ Parse JSON response
        └→ Return StockResponse + Cache
    ↓
Response JSON
```

---

## 🔌 Cómo Conectar con API Externa

### Proceso General

1. **Obtener credenciales** (API Key)
2. **Registrar la clave** en properties
3. **Crear cliente HTTP** (RestTemplate existe)
4. **Construir URL** con parámetros
5. **Manejar errores** y parsing

### Ejemplo: Alpha Vantage (Ya Implementado)

```java
// 1. URL con parámetros
String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" 
           + symbol + "&apikey=" + apiKey;

// 2. Llamada HTTP
String rawJson = restTemplate.getForObject(url, String.class);

// 3. Parsing de respuesta
JsonNode root = objectMapper.readTree(rawJson);
JsonNode timeSeries = root.get("Time Series (Daily)");

// 4. Extraer datos
prices.put(date, closePrice);
```

### Manejar Limitaciones de API

Alpha Vantage tiene limitaciones en la versión gratuita:
- **5 requests/minuto** máximo
- **500 requests/día** máximo

**Solución implementada:**
- Cache en memoria evita llamadas repetidas
- Logs informan sobre límites

```java
logger.warn("Advertencia de API: Thank you for using Alpha Vantage!");
// Esperar 12 segundos antes de reintentar
Thread.sleep(12000);
```

---

## 🆕 Agregar Nuevo Proveedor

### Paso 1: Crear Nueva Clase Implementadora

```java
package com.api.parcial.provider;

import com.api.parcial.model.StockResponse;
import org.springframework.stereotype.Service;

/**
 * Proveedor usando Yahoo Finance API
 */
@Service
public class YahooFinanceProvider implements StockProvider {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "https://query2.finance.yahoo.com";

    @Override
    public StockResponse getDaily(String symbol) {
        // Construir URL específica de Yahoo
        String url = API_URL + "/v8/finance/chart/" + symbol 
                   + "?interval=1d&range=1y";

        try {
            // Llamar API
            String rawJson = restTemplate.getForObject(url, String.class);
            
            // Parsear respuesta (formato diferente de Alpha Vantage)
            return parseYahooResponse(rawJson, symbol);
        } catch (Exception e) {
            logger.error("Error en Yahoo Finance", e);
            throw new RuntimeException("Error al obtener datos", e);
        }
    }

    @Override
    public StockResponse getIntraday(String symbol) {
        // Similar a getDaily pero con interval=5m
        return null;
    }

    @Override
    public StockResponse getWeekly(String symbol) {
        // interval=1wk
        return null;
    }

    @Override
    public StockResponse getMonthly(String symbol) {
        // interval=1mo
        return null;
    }

    private StockResponse parseYahooResponse(String rawJson, String symbol) {
        // Parsear estructura JSON de Yahoo (diferente a Alpha Vantage)
        Map<String, Double> prices = new HashMap<>();
        // ... lógica de parsing ...
        return new StockResponse(symbol, "DAILY", prices);
    }
}
```

### Paso 2: Actualizar ProviderConfig

**Opción A: Reemplazar proveedor actual**
```java
@Configuration
public class ProviderConfig {

    @Bean
    public StockProvider stockProvider() {
        // Cambiar a Yahoo Finance
        return new YahooFinanceProvider();  // ← CAMBIAR AQUI
    }
}
```

**Opción B: Múltiples proveedores (Recomendado)**
```java
@Configuration
public class ProviderConfig {

    @Bean(name = "alphaVantage")
    public StockProvider alphaVantageProvider() {
        return new AlphaVantageProvider();
    }

    @Bean(name = "yahooFinance")
    public StockProvider yahooFinanceProvider() {
        return new YahooFinanceProvider();
    }

    @Bean  // Proveedor por defecto
    public StockProvider stockProvider() {
        return alphaVantageProvider();
    }
}
```

### Paso 3: Usar en Servicio (Opcional - Si hay múltiples)
```java
@Service
public class StockFacadeService {

    private final StockProvider alphaVantage;
    private final StockProvider yahooFinance;

    public StockFacadeService(
        @Qualifier("alphaVantage") StockProvider alphaVantage,
        @Qualifier("yahooFinance") StockProvider yahooFinance
    ) {
        this.alphaVantage = alphaVantage;
        this.yahooFinance = yahooFinance;
    }

    // Usuario especifica qué proveedor usar
    @GetMapping("/daily")
    public StockResponse getDaily(
        @RequestParam String symbol,
        @RequestParam(defaultValue = "alpha") String provider
    ) {
        if ("yahoo".equals(provider)) {
            return yahooFinance.getDaily(symbol);
        }
        return alphaVantage.getDaily(symbol);
    }
}
```

### Paso 4: Agregar Configuración en application.properties
```properties
# Yahoo Finance Configuration (si aplica)
yahoofinance.api.url=https://query2.finance.yahoo.com

# Elegir proveedor por defecto
stock.provider=yahoo  # o "alpha"
```

### Comparativa de Proveedores

| Proveedor | Ventajas | Desventajas | Límites |
|-----------|----------|------------|---------|
| **Alpha Vantage** | Fácil, JSON limpio | Lento, rate limit bajo | 5/min (gratis) |
| **Yahoo Finance** | Rápido, libre | JSON complejo, menos histórico | Desconocido |
| **IEX Cloud** | Excelente datos | Pago | Varía |
| **Finnhub** | Buena API | Pago | Varía |

---

## 🐛 Troubleshooting

### Problema: "Error getting data from API"
**Causa:** API Key inválida o expirada
**Solución:**
1. Verificar que `ALPHAVANTAGE_API_KEY` está configurada
2. Validar la clave en https://www.alphavantage.co/
3. Generar una nueva si es necesario

### Problema: "429 Too Many Requests"
**Causa:** Límite de llamadas por minuto excedido
**Solución:**
1. Esperar 60 segundos (cache debería prevenir esto)
2. Actualizar cache más frecuentemente
3. Usar plan de pago de Alpha Vantage

### Problema: CORS Error en el navegador
**Causa:** El frontend no puede acceder por restricciones CORS
**Solución:**
El `CorsConfig` ya está configurado para permitir acceso. Si persiste:
```java
registry.allowedOrigins("http://localhost:3000")  // Frontend específico
        .allowedMethods("GET", "POST")
        .allowCredentials(true);
```

### Problema: NullPointerException en parseResponse
**Causa:** Estructura JSON diferente de esperada
**Solución:**
1. Verificar respuesta con curl:
```bash
curl "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=AAPL&apikey=YOUR_KEY"
```
2. Inspeccionar JSON en respuesta
3. Ajustar nombres de claves en parseResponse

### Problema: conexión lenta
**Causa:** Cache no funcionando o endpoint no cacheado
**Solución:**
1. Verificar logs: `INFO: Hit en cache para: DAILY_AAPL`
2. Segunda llamada debe ser instantánea
3. Si no, revisar StockCache.getOrCompute()

---

## 📊 Monitoreo y Logs

### Niveles de Log Configurables
```properties
# En application.properties
logging.level.com.api.parcial=DEBUG    # Ver todo
logging.level.com.api.parcial=INFO     # Info importante
logging.level.com.api.parcial=WARN     # Solo advertencias
```

### Ejemplos de Logs
```
INFO: Llamando a API: https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=AAPL
INFO: Respuesta recibida de Alpha Vantage
INFO: Parseados 100 precios para AAPL
DEBUG: Hit en cache para: DAILY_AAPL
```

---

## 🧪 Testing

Para agregar tests unitarios:

```java
@SpringBootTest
class StockControllerTest {

    @MockBean
    private StockFacadeService facadeService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetDaily() throws Exception {
        StockResponse mockResponse = new StockResponse("AAPL", "DAILY", 
            Map.of("2024-02-22", 182.45));
        
        when(facadeService.getDaily("AAPL"))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/stock/daily?symbol=AAPL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("AAPL"));
    }
}
```

---

## 📈 Mejoras Futuras

- [x] Comentarios en código
- [ ] Cache distribuido (Redis)
- [ ] TTL (Time To Live) en cache
- [ ] Base de datos persistente
- [ ] Autenticación con JWT
- [ ] Rate limiting
- [ ] Webhooks para actualizaciones en tiempo real
- [ ] Swagger/OpenAPI documentation
- [ ] Métricas con Prometheus
- [ ] Tests unitarios completos

---

## 📝 Licencia

Este proyecto es de código abierto. Úsalo libremente.

---

## 📞 Soporte

Para preguntas o problemas:
1. Revisar los logs: `mvn spring-boot:run | grep ERROR`
2. Consultar la sección Troubleshooting
3. Validar configuración en application.properties

---

**¡Disfruta usando Stock API Service!** 🚀
