package com.api.parcial.model;

import java.util.Map;

/**
 * Clase que representa la respuesta de datos de acciones.
 * Contiene el símbolo, intervalo de tiempo y los precios históricos.
 */
public class StockResponse {
    private String symbol;      // Símbolo del ticker (ej: AAPL, GOOGL)
    private String interval;    // Intervalo de tiempo (DAILY, WEEKLY, MONTHLY, INTRADAY)
    private Map<String, Double> prices;  // Mapa de fechas a precios de cierre

    /**
     * Constructor de StockResponse
     * @param symbol Símbolo del ticker
     * @param interval Intervalo de tiempo
     * @param prices Mapa de fechas a precios
     */
    public StockResponse(String symbol, String interval, Map<String, Double> prices) {
        this.symbol = symbol;
        this.interval = interval;
        this.prices = prices;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInterval() {
        return interval;
    }

    public Map<String, Double> getPrices() {
        return prices;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public void setPrices(Map<String, Double> prices) {
        this.prices = prices;
    }
}
