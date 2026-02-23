# Guía de Implementación de Nuevos Proveedores

Esta guía te ayudará a agregar proveedores adicionales de datos de acciones a tu aplicación.

## 📋 Tabla de Contenidos

1. [Conceptos Clave](#conceptos-clave)
2. [Paso a Paso: Agregar Yahoo Finance](#paso-a-paso-agregar-yahoo-finance)
3. [Plantilla Base](#plantilla-base)
4. [Ejemplos Prácticos](#ejemplos-prácticos)
5. [Testing del Proveedor](#testing-del-proveedor)

---

## 🎯 Conceptos Clave

### ¿Qué es un Proveedor?
Un proveedor es una clase que implementa la interfaz `StockProvider` y conecta con una API externa específica para obtener datos de acciones.

### ¿Por qué Strategy Pattern?
El patrón Strategy permite:
- ✅ Intercambiar proveedores sin modificar código existente
- ✅ Agregar nuevos proveedores fácilmente
- ✅ Testear cada uno independientemente
- ✅ Usar múltiples proveedores simultáneamente

```
Interface StockProvider (contrato)
    ↑
    ├─ AlphaVantageProvider (implementación 1)
    ├─ YahooFinanceProvider (implementación 2)
    └─ CoinGeckoProvider (implementación 3)
```

---

## 🔧 Paso a Paso: Agregar Yahoo Finance

### Paso 1: Analizar la API de Yahoo Finance

**Endpoint ejemplo:**
```
https://query1.finance.yahoo.com/v8/finance/chart/AAPL?interval=1d&range=1y
```

**Respuesta estructura:**
```json
{
  "chart": {
    "result": [
      {
        "meta": {
          "symbol": "AAPL",
          "currency": "USD"
        },
        "timestamp": [1645228800, 1645315200, ...],
        "indicators": {
          "quote": [
            {
              "close": [182.45, 181.20, 180.50, ...],
              "open": [180.00, 182.00, ...],
              "volume": [50000000, 45000000, ...],
              "high": [183.00, 182.50, ...],
              "low": [179.50, 180.00, ...]
            }
          ]
        }
      }
    ]
  }
}
```

### Paso 2: Crear la Clase Implementadora

**Archivo:** `src/main/java/com/api/parcial/provider/YahooFinanceProvider.java`

```java
package com.api.parcial.provider;

import com.api.parcial.model.StockResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Proveedor de datos usando Yahoo Finance API.
 * 
 * Ventajas:
 * - No requiere API Key
 * - Datos históricos completos
 * - Rápido y confiable
 * 
 * Desventajas:
 * - No es una API oficial (puede cambiar)
 * - JSON más complejo
 */
@Service
public class YahooFinanceProvider implements StockProvider {

    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceProvider.class);
    private static final String BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public YahooFinanceProvider() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public StockResponse getIntraday(String symbol) {
        // interval=5m (5 minutos)
        String url = buildUrl(symbol, "5m", "1d");  // Último día
        String rawJson = callApi(url);
        return parseYahooResponse(rawJson, symbol, "INTRADAY");
    }

    @Override
    public StockResponse getDaily(String symbol) {
        // interval=1d (diario)
        String url = buildUrl(symbol, "1d", "1y");  // Último año
        String rawJson = callApi(url);
        return parseYahooResponse(rawJson, symbol, "DAILY");
    }

    @Override
    public StockResponse getWeekly(String symbol) {
        // interval=1wk (semanal)
        String url = buildUrl(symbol, "1wk", "5y");  // Últimos 5 años
        String rawJson = callApi(url);
        return parseYahooResponse(rawJson, symbol, "WEEKLY");
    }

    @Override
    public StockResponse getMonthly(String symbol) {
        // interval=1mo (mensual)
        String url = buildUrl(symbol, "1mo", "20y");  // Últimos 20 años
        String rawJson = callApi(url);
        return parseYahooResponse(rawJson, symbol, "MONTHLY");
    }

    /**
     * Construye URL para Yahoo Finance
     */
    private String buildUrl(String symbol, String interval, String range) {
        return BASE_URL + "/" + symbol 
            + "?interval=" + interval 
            + "&range=" + range;
    }

    /**
     * Realiza la llamada HTTP a Yahoo Finance
     */
    private String callApi(String url) {
        try {
            logger.info("Llamando a Yahoo Finance: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            logger.info("Respuesta recibida de Yahoo Finance");
            return response;
        } catch (Exception e) {
            logger.error("Error al llamar Yahoo Finance API", e);
            throw new RuntimeException("Error al consultar Yahoo Finance", e);
        }
    }

    /**
     * Parsea la respuesta JSON de Yahoo Finance
     */
    private StockResponse parseYahooResponse(String rawJson, String symbol, String interval) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            Map<String, Double> prices = new HashMap<>();

            // Navegar por la estructura compleja de Yahoo
            JsonNode result = root.get("chart").get("result").get(0);
            JsonNode timestamps = result.get("timestamp");
            JsonNode quotes = result.get("indicators").get("quote").get(0);
            JsonNode closes = quotes.get("close");

            // Convertir timestamps y precios a Map
            for (int i = 0; i < timestamps.size(); i++) {
                if (timestamps.get(i) != null && closes.get(i) != null) {
                    long timestamp = timestamps.get(i).asLong();
                    double closePrice = closes.get(i).asDouble();

                    // Convertir timestamp a fecha local
                    LocalDate date = Instant.ofEpochSecond(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    prices.put(date.toString(), closePrice);
                }
            }

            logger.info("Parseados {} precios para {} desde Yahoo", prices.size(), symbol);
            return new StockResponse(symbol, interval, prices);

        } catch (Exception e) {
            logger.error("Error al parsear respuesta de Yahoo Finance", e);
            throw new RuntimeException("Error al procesar datos", e);
        }
    }
}
```

### Paso 3: Registrar en ProviderConfig

**Opción A: Reemplazar proveedor actual**
```java
@Configuration
public class ProviderConfig {

    @Bean
    public StockProvider stockProvider() {
        return new YahooFinanceProvider();  // ← CAMBIAR
    }
}
```

**Opción B: Múltiples proveedores**
```java
@Configuration
public class ProviderConfig {

    @Bean(name = "alphaVantage")
    public StockProvider alphaVantageProvider(@Value("${alphavantage.api.key}") String apiKey) {
        return new AlphaVantageProvider(apiKey);
    }

    @Bean(name = "yahooFinance")
    public StockProvider yahooFinanceProvider() {
        return new YahooFinanceProvider();
    }

    @Bean  // Proveedor por defecto
    public StockProvider stockProvider() {
        return yahooFinanceProvider();  // Yahoo por defecto
    }
}
```

### Paso 4: Usar Múltiples Proveedores en el Controlador

```java
@RestController
@RequestMapping("/stock")
public class StockController {

    private final Map<String, StockProvider> providers;

    public StockController(@Qualifier("alphaVantage") StockProvider alphaVantage,
                         @Qualifier("yahooFinance") StockProvider yahooFinance) {
        this.providers = Map.of(
            "alpha", alphaVantage,
            "yahoo", yahooFinance
        );
    }

    @GetMapping("/daily")
    public StockResponse getDaily(
        @RequestParam String symbol,
        @RequestParam(defaultValue = "yahoo") String provider
    ) {
        return providers.get(provider).getDaily(symbol);
    }
}
```

---

## 📋 Plantilla Base

Usa esta plantilla para crear nuevos proveedores:

```java
package com.api.parcial.provider;

import com.api.parcial.model.StockResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Proveedor para [Nombre de la API].
 * 
 * Documentación API: [URL]
 */
@Service
public class [NombreProvider]Provider implements StockProvider {

    private static final Logger logger = LoggerFactory.getLogger([NombreProvider]Provider.class);
    private static final String BASE_URL = "[URL_BASE]";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;  // Si requiere API Key

    public [NombreProvider]Provider(/* Parámetros inyectados */) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public StockResponse getDaily(String symbol) {
        String url = buildUrl(symbol, "daily");
        String rawJson = callApi(url);
        return parseResponse(rawJson, symbol, "DAILY");
    }

    @Override
    public StockResponse getIntraday(String symbol) {
        // TODO: Implementar
        return null;
    }

    @Override
    public StockResponse getWeekly(String symbol) {
        // TODO: Implementar
        return null;
    }

    @Override
    public StockResponse getMonthly(String symbol) {
        // TODO: Implementar
        return null;
    }

    private String buildUrl(String symbol, String interval) {
        // Construir URL específica del proveedor
        return BASE_URL + "?symbol=" + symbol;
    }

    private String callApi(String url) {
        try {
            logger.info("Llamando a [Nombre]: {}", url);
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            logger.error("Error en llamada a API", e);
            throw new RuntimeException("Error", e);
        }
    }

    private StockResponse parseResponse(String rawJson, String symbol, String interval) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            Map<String, Double> prices = new HashMap<>();
            
            // TODO: Parsear JSON según estructura del proveedor
            
            return new StockResponse(symbol, interval, prices);
        } catch (Exception e) {
            logger.error("Error al parsear respuesta", e);
            throw new RuntimeException("Error parsing", e);
        }
    }
}
```

---

## 💡 Ejemplos Prácticos

### Ejemplo 1: CoinGecko (Criptomonedas)

```java
@Service
public class CoinGeckoProvider implements StockProvider {
    
    private static final String BASE_URL = "https://api.coingecko.com/api/v3";

    @Override
    public StockResponse getDaily(String symbol) {
        // symbol = "bitcoin", "ethereum", etc
        String url = BASE_URL + "/coins/" + symbol.toLowerCase() 
                   + "/market_chart?vs_currency=usd&days=365";
        
        String rawJson = restTemplate.getForObject(url, String.class);
        JsonNode root = objectMapper.readTree(rawJson);
        JsonNode prices = root.get("prices");
        
        Map<String, Double> priceMap = new HashMap<>();
        for (JsonNode price : prices) {
            long timestamp = price.get(0).asLong();
            double closePrice = price.get(1).asDouble();
            // Convertir timestamp a fecha
            priceMap.put(convertTimestamp(timestamp), closePrice);
        }
        
        return new StockResponse(symbol, "DAILY", priceMap);
    }
    
    // El resto es similar...
}
```

### Ejemplo 2: Finnhub (Requiere API Key)

```java
@Service
public class FinnhubProvider implements StockProvider {
    
    private final String apiKey;  // Inyectada desde @Value
    private static final String BASE_URL = "https://finnhub.io/api/v1";

    public FinnhubProvider(@Value("${finnhub.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public StockResponse getDaily(String symbol) {
        String url = BASE_URL + "/quote?symbol=" + symbol 
                   + "&token=" + apiKey;
        
        String rawJson = restTemplate.getForObject(url, String.class);
        // Parsear y retornar...
        return null;
    }
}
```

---

## 🧪 Testing del Proveedor

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class YahooFinanceProviderTest {

    @Autowired
    private YahooFinanceProvider provider;

    @Test
    public void testGetDaily() {
        StockResponse response = provider.getDaily("AAPL");
        
        assertNotNull(response);
        assertEquals("AAPL", response.getSymbol());
        assertEquals("DAILY", response.getInterval());
        assertFalse(response.getPrices().isEmpty());
    }

    @Test
    public void testGetIntraday() {
        StockResponse response = provider.getIntraday("GOOGL");
        
        assertNotNull(response);
        assertEquals("GOOGL", response.getSymbol());
        assertFalse(response.getPrices().isEmpty());
    }

    @Test
    public void testInvalidSymbol() {
        assertThrows(RuntimeException.class, () -> {
            provider.getDaily("INVALIDTELLTICKER123");
        });
    }
}
```

---

## 🔄 Migrar Entre Proveedores

Si necesitas cambiar de proveedor:

1. **Crear nuevo proveedor**
2. **Registrar en ProviderConfig**
3. **Cambiar o agregar @Bean**
4. Reiniciar aplicación
5. **Validar en logs:**
   ```
   INFO: Llamando a Yahoo Finance...
   INFO: Parseados 250 precios para AAPL
   ```

---

## 📊 Comparativa de APIs Recomendadas

| API | Pros | Contras | Tier Gratis |
|-----|------|---------|------------|
| **Yahoo Finance** | No requiere API Key, rápido | No oficial | ✅ Sí |
| **Alpha Vantage** | Muchos endpoints | Rate limit bajo | ✅ Limitado |
| **Finnhub** | Excelente API | Pago | ✅ Limited |
| **IEX Cloud** | Professional | Pago | ✅ Free tier |
| **CoinGecko** | Cryptos gratis | No stocks | ✅ Sí |
| **Polygon.io** | Actualizado | Pago | ✅ Limited |

---

## ⚠️ Consideraciones Importantes

1. **Rate Limiting:** Respetar límites de la API
2. **Caching:** Implementar cache para no saturar API
3. **Errores:** Manejar timeouts y errores
4. **Documentación:** Documentar cambios en formato JSON
5. **Testing:** Probar nuevos proveedores antes de usar en prod
6. **Credenciales:** Nunca commitear API Keys (usar env vars)

---

¡Listo! Ahora puedes agregar cualquier proveedor siguiendo esta guía. 🚀
