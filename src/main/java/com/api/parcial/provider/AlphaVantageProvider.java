package com.api.parcial.provider;

import com.api.parcial.model.StockResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AlphaVantageProvider implements StockProvider {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageProvider.class);
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    /**
     * Constructor que inyecta la clave API desde properties
     */
    public AlphaVantageProvider(@Value("${alphavantage.api.key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Obtiene datos intradiarios (cada 5 minutos) de una acción.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios cada 5 minutos
     */
    @Override
    public StockResponse getIntraday(String symbol) {
        String url = buildUrl("TIME_SERIES_INTRADAY", symbol, "interval=5min");
        String rawJson = callApi(url);
        return parseResponse(rawJson, symbol, "INTRADAY", "Time Series (5min)");
    }

    /**
     * Obtiene precios diarios de una acción.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios diarios
     */
    @Override
    public StockResponse getDaily(String symbol) {
        String url = buildUrl("TIME_SERIES_DAILY", symbol, "");
        String rawJson = callApi(url);
        return parseResponse(rawJson, symbol, "DAILY", "Time Series (Daily)");
    }

    /**
     * Obtiene precios semanales de una acción.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios semanales
     */
    @Override
    public StockResponse getWeekly(String symbol) {
        String url = buildUrl("TIME_SERIES_WEEKLY", symbol, "");
        String rawJson = callApi(url);
        return parseResponse(rawJson, symbol, "WEEKLY", "Time Series (Weekly)");
    }

    /**
     * Obtiene precios mensuales de una acción.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios mensuales
     */
    @Override
    public StockResponse getMonthly(String symbol) {
        String url = buildUrl("TIME_SERIES_MONTHLY", symbol, "");
        String rawJson = callApi(url);
        return parseResponse(rawJson, symbol, "MONTHLY", "Time Series (Monthly)");
    }

    /**
     * Construye la URL para llamar a Alpha Vantage.
     * @param function Función de la API (TIME_SERIES_DAILY, etc)
     * @param symbol Símbolo del ticker
     * @param additionalParams Parámetros adicionales
     * @return URL construida
     */
    private String buildUrl(String function, String symbol, String additionalParams) {
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("?function=").append(function)
                .append("&symbol=").append(symbol)
                .append("&apikey=").append(apiKey);
        
        if (!additionalParams.isEmpty()) {
            url.append("&").append(additionalParams);
        }
        
        return url.toString();
    }

    /**
     * Llama a la API externa con manejo de errores.
     * @param url URL a llamar
     * @return JSON crudo como String
     */
    private String callApi(String url) {
        try {
            logger.info("Llamando a API: {}", url.split("&apikey=")[0]); // Log sin mostrar API key
            String response = restTemplate.getForObject(url, String.class);
            logger.info("Respuesta recibida de Alpha Vantage");
            return response;
        } catch (RestClientException e) {
            logger.error("Error al llamar a Alpha Vantage API", e);
            throw new RuntimeException("Error al consultar datos de acciones", e);
        }
    }

    /**
     * Parsea la respuesta JSON de Alpha Vantage extrayendo precios.
     * @param rawJson JSON crudo recibido de la API
     * @param symbol Símbolo del ticker
     * @param interval Intervalo de tiempo
     * @param timeSeriesKey Clave JSON de la serie temporal (varía según tipo de datos)
     * @return StockResponse con datos parseados
     */
    private StockResponse parseResponse(String rawJson,
                                        String symbol,
                                        String interval,
                                        String timeSeriesKey) {

        try {
            JsonNode series = objectMapper
                    .readTree(rawJson)
                    .path(timeSeriesKey);

            Map<String, Double> prices = new HashMap<>();

            series.properties().forEach(entry ->
                    prices.put(
                            entry.getKey(),
                            entry.getValue().path("1. open").asDouble()
                    )
            );

            return new StockResponse(symbol, interval, prices);

        } catch (Exception e) {
            throw new RuntimeException("Error parseando respuesta", e);
        }
    }
}
