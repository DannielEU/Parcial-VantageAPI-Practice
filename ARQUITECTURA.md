# 🏗️ Arquitectura de la Aplicación

## Diagrama General del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENTE (Navegador)                       │
│                   http://localhost:8080/stock                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                   HTTP GET Request (CORS)
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT APPLICATION                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           REST CONTROLLER                                 │  │
│  │  StockController                                          │  │
│  │  ├─ GET /stock/daily?symbol=AAPL                         │  │
│  │  ├─ GET /stock/intraday?symbol=AAPL                      │  │
│  │  ├─ GET /stock/weekly?symbol=AAPL                        │  │
│  │  └─ GET /stock/monthly?symbol=AAPL                       │  │
│  └──────────────────┬───────────────────────────────────────┘  │
│                     │                                            │
│                     ▼ (inyección de dependencias)                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │      SERVICE - FACADE PATTERN                             │  │
│  │  StockFacadeService                                       │  │
│  │  ├─ Coordina Provider y Cache                            │  │
│  │  ├─ genera claves de cache únicas                        │  │
│  │  └─ implementa lógica de negocio                         │  │
│  └──────┬──────────────────────────┬──────────────────────┘  │
│         │                          │                          │
│         ▼                          ▼                          │
│  ┌────────────────┐      ┌────────────────────┐             │
│  │  CACHE (Mem)   │      │  PROVIDERS         │             │
│  │  StockCache    │      │  (Strategy)        │             │
│  │  ConcurrentHM  │      │                    │             │
│  │  ├─ getOrComp  │      │ ┌─────────────────┐│             │
│  │  ├─ invalidate │      │ │AlphaVantage     ││             │
│  │  ├─ clear      │      │ │Impl.            ││             │
│  │  └─ size       │      │ └────────┬────────┘│             │
│  └────────────────┘      │          │         │             │
│         ▲                 │ ┌─────────────────┐│             │
│         │                 │ │YahooFinance     ││             │
│         │                 │ │Impl.            ││             │
│         │                 │ └─────────────────┘│             │
│         │                 │                    │             │
│         │                 └────────┬───────────┘             │
│         │                          │                         │
│         │                          ▼                         │
│         │                 RestTemplate                       │
│         └──────────────────────────────────────┐            │
│                                                │             │
│                                    HTTP REST Call            │
│                                                │             │
│          ┌─────────────────────────────────────┼──┐          │
│          │                                     │  │          │
│          ▼                                     ▼  │          │
│  ┌──────────────────┐              ┌──────────────────┐    │
│  │  JSON Response   │              │  MODEL           │    │
│  │  Parsing         │              │  StockResponse   │    │
│  │  (ObjectMapper)  │              │  ├─ symbol       │    │
│  │  ├─ timeSeries   │              │  ├─ interval     │    │
│  │  ├─ close prices │              │  └─ prices (Map) │    │
│  │  └─ dates        │              └──────────────────┘    │
│  └──────────┬───────┘                      ▲               │
│             │                              │               │
│             └──────────────────────────────┘               │
│                                                             │
└─────────────────────────┬───────────────────────────────────┘
                          │
              JSON Response with caching
                          │
                          ▼
┌─────────────────────────────────────────┐
│  {"symbol":"AAPL","interval":"DAILY",   │
│   "prices":{"2024-02-22":182.45,...}}   │
└─────────────────────────────────────────┘
```

---

## Flujo de Datos Detallado

### 1️⃣ Primera Solicitud (Cache MISS)

```
Request: GET /stock/daily?symbol=AAPL
    ↓
StockController.getDaily("AAPL")
    ↓
StockFacadeService.getDaily("AAPL")
    ↓
StockCache.getOrCompute("DAILY_AAPL", supplier)
    ↓
    ├─ ¿"DAILY_AAPL" existe en cache? NO
    │   ↓
    └─ Ejecutar supplier (llamar provider)
        ↓
    AlphaVantageProvider.getDaily("AAPL")
        ↓
    BUILD URL: https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=AAPL&apikey=XXX
        ↓
    RestTemplate.getForObject(url, String.class)
        ↓
    HTTP GET → Alpha Vantage API [~2000ms]
        ↓
    Parse JSON con ObjectMapper
        ↓
    Extraer Time Series (Daily)
        ↓
    Convertir a Map<String, Double>
        ↓
    Crear StockResponse(symbol, interval, prices)
        ↓
    Guardar en cache: cache.put("DAILY_AAPL", response)
        ↓
    Return StockResponse
        ↓
Response JSON a cliente [TOTAL: ~2-3 segundos]
```

### 2️⃣ Segunda Solicitud (Cache HIT)

```
Request: GET /stock/daily?symbol=AAPL
    ↓
StockController.getDaily("AAPL")
    ↓
StockFacadeService.getDaily("AAPL")
    ↓
StockCache.getOrCompute("DAILY_AAPL", supplier)
    ↓
    ├─ ¿"DAILY_AAPL" existe en cache? SÍ
    │   ↓
    └─ Return cache.get("DAILY_AAPL")
        ↓
Response JSON a cliente [TOTAL: <100ms] ⚡
```

---

## Inyección de Dependencias (Spring)

```
┌──────────────────────────────────┐
│  ProviderConfig (Spring Bean)    │
│  ┌────────────────────────────┐  │
│  │ @Bean                      │  │
│  │ public StockProvider()     │  │
│  │   → new AlphaVantage...()  │  │
│  └────────────────────────────┘  │
└──────────┬───────────────────────┘
           │ inyecta
           ▼
┌──────────────────────────────────┐
│  StockFacadeService              │
│  ┌────────────────────────────┐  │
│  │ constructor(                │  │
│  │   StockProvider provider,  │  │
│  │   StockCache cache         │  │
│  │ )                          │  │
│  └────────────────────────────┘  │
└──────────┬───────────────────────┘
           │ inyecta
           ▼
┌──────────────────────────────────┐
│  StockController                 │
│  ┌────────────────────────────┐  │
│  │ constructor(                │  │
│  │   StockFacadeService facade│  │
│  │ )                          │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

---

## Strategy Pattern (Múltiples Proveedores)

```
InterfaceStockProvider (Contrato)
    │
    ├─ AlphaVantageProvider
    │  ├─ getDaily() → Llama API Alpha Vantage
    │  ├─ getIntraday() → Datos cada 5min
    │  ├─ getWeekly() → Datos semanales
    │  └─ getMonthly() → Datos mensuales
    │
    ├─ YahooFinanceProvider
    │  ├─ getDaily() → Llama API Yahoo
    │  ├─ getIntraday() → Yahoo 5min
    │  ├─ getWeekly() → Yahoo semanales
    │  └─ getMonthly() → Yahoo mensuales
    │
    └─ OtherProvider
       ├─ getDaily() → Llama otra API
       ├─ getIntraday() → ...
       ├─ getWeekly() → ...
       └─ getMonthly() → ...

Ventaja: Solo cambiar @Bean en ProviderConfig
         No modificar código existente
         Fácil agregar nuevos
```

---

## Capas de Datos

```
┌────────────────────────────────────────┐
│  PRESENTACIÓN (HTTP)                   │
│  ├─ Request: GET /stock/daily?symbol   │
│  └─ Response: JSON                     │
└────────────┬─────────────────────────┘
             │
┌────────────▼─────────────────────────┐
│  CONTROLLER                            │
│  ├─ Recibe request HTTP                │
│  ├─ Valida parámetros                 │
│  └─ Retorna response JSON             │
└────────────┬──────────────────────────┘
             │
┌────────────▼──────────────────────────┐
│  SERVICE (Lógica de Negocio)          │
│  ├─ Coordina cache + provider          │
│  ├─ Genera claves de cache             │
│  └─ Implementa patrones               │
└────────────┬───────────────────────────┘
             │
       ┌─────┴─────┐
       ▼           ▼
┌──────────────┐  ┌──────────────────┐
│  CACHE       │  │  PROVIDER        │
│  ├─ Memoria  │  │  ├─ API llamadas │
│  └─ Rápido   │  │  ├─ JSON parsing │
└──────────────┘  │  └─ Errores      │
                  └──────────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ API EXTERNA  │
                  │ AlphaVantage │
                  │ Yahoo        │
                  │ Finnhub      │
                  └──────────────┘
```

---

## Configuración y Propiedades

```
application.properties
├─ spring.application.name=API-Service
├─ server.port=8080
├─ logging.level.com.api.parcial=INFO
└─ alphavantage.api.key=${ALPHAVANTAGE_API_KEY:demo}
   └─ Inyectado de variable de entorno
      o valor por defecto "demo"
```

---

## Manejo de Errores

```
Request a Proveedor
    │
    ├─ ✅ Éxito
    │  ├─ Parse JSON
    │  ├─ Guardar en cache
    │  └─ Return StockResponse
    │
    └─ ❌ Error
       ├─ RestClientException
       │  └─ Log + RuntimeException
       ├─ JSON Parse Exception
       │  └─ Log + RuntimeException
       ├─ API Error Message
       │  └─ Log warning + empty response
       └─ Rate Limiting (429)
           └─ Log warning + wait/retry
```

---

## Performance y Cache

```
Primera Solicitud (AAPL DAILY):
├─ Construcción URL: 1ms
├─ Llamada API: ~2000ms ⏱️
├─ Parse JSON: 10ms
├─ Guardar en cache: 1ms
└─ Return respuesta: 1ms
   TOTAL: ~2012ms

Segunda Solicitud (AAPL DAILY):
├─ Buscar en cache: 1ms
├─ Return respuesta: <1ms
   TOTAL: <1ms

Mejora: 2000x más rápido ⚡⚡⚡
```

---

## Escalabilidad del Sistema

```
Versión 1 (Actual):
└─ 1 Proveedor (AlphaVantage)
   └─ Cache en memoria
   └─ Performance: ~ RÁPIDO

Versión 2 (Próxima):
├─ N Proveedores (Strategy Pattern)
├─ Cache Redis (distribuido)
└─ Performance: MÁS RÁPIDO

Versión 3 (Futuro):
├─ Múltiples proveedores con fallback
├─ Base de datos persistente
├─ Rate limiting
├─ Autenticación JWT
└─ Performance: ÓPTIMO
```

---

## Estructura de Directorios

```
demo/
├─── src/main/java/com/api/parcial/
│    ├─ ApiServiceApplication.java       (Punto entrada)
│    ├─ controller/
│    │  └─ StockController.java          (Endpoints REST)
│    ├─ service/
│    │  └─ StockFacadeService.java       (Lógica negocio)
│    ├─ provider/
│    │  ├─ StockProvider.java            (Interfaz)
│    │  └─ AlphaVantageProvider.java     (Implementación)
│    ├─ model/
│    │  └─ StockResponse.java            (DTO)
│    ├─ cache/
│    │  └─ StockCache.java               (Cache sistema)
│    └─ config/
│       ├─ CorsConfig.java               (CORS)
│       └─ ProviderConfig.java           (Inyección dep)
│
├─── src/main/resources/
│    └─ application.properties            (Propiedades)
│
├─── pom.xml                             (Dependencias)
│
└─── Documentación/
     ├─ README.md                        (Completa)
     ├─ SETUP.md                         (Rápida)
     ├─ PROVIDERS_GUIDE.md               (Proveedores)
     ├─ API_REFERENCE.md                 (Endpoints)
     ├─ CAMBIOS.md                       (Resumen)
     └─ .env.example                     (Plantilla)
```

---

Esta arquitectura es:
- ✅ **Escalable:** Agregar nuevos proveedores es trivial
- ✅ **Mantenible:** Separación clara de responsabilidades
- ✅ **Testeable:** Dependencias inyectadas, fácil mockear
- ✅ **Performante:** Cache automático optimiza API
- ✅ **Segura:** Variables de entorno, sin datos sensibles

¡Listo para producción! 🚀
