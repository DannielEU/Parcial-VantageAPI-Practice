package com.api.parcial.service;

import com.api.parcial.cache.StockCache;
import com.api.parcial.provider.StockProvider;
import com.api.parcial.model.StockResponse;
import org.springframework.stereotype.Service;

/**
 * Servicio fachada (Facade Pattern) que coordina el acceso a datos de acciones.
 * Abstrae la complejidad de múltiples proveedores y cachéo.
 * Implementa lógica de cachéo automático para optimizar llamadas a APIs externas.
 */
@Service
public class StockFacadeService {

    private final StockProvider provider;  // Proveedor de datos (AlphaVantage, etc)
    private final StockCache cache;         // Cache para evitar llamadas repetidas

    /**
     * Constructor que inyecta dependencias del proveedor y el cache
     * @param provider Implementación del proveedor de datos
     * @param cache Sistema de cachéo
     */
    public StockFacadeService(StockProvider provider, StockCache cache) {
        this.provider = provider;
        this.cache = cache;
    }

    /**
     * Obtiene datos diarios cacheyos.
     * Si están en cache, devuelve de ahí. Si no, llama al proveedor y cachea el resultado.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios diarios
     */
    public StockResponse getDaily(String symbol) {
        return cache.getOrCompute("DAILY_" + symbol,
                () -> provider.getDaily(symbol));
    }

    /**
     * Obtiene datos intradiarios (cada 5 minutos) con cachéo.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios intradiarios
     */
    public StockResponse getIntraday(String symbol) {
        return cache.getOrCompute("INTRADAY_" + symbol,
                () -> provider.getIntraday(symbol));
    }

    /**
     * Obtiene datos semanales con cachéo.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios semanales
     */
    public StockResponse getWeekly(String symbol) {
        return cache.getOrCompute("WEEKLY_" + symbol,
                () -> provider.getWeekly(symbol));
    }

    /**
     * Obtiene datos mensuales con cachéo.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios mensuales
     */
    public StockResponse getMonthly(String symbol) {
        return cache.getOrCompute("MONTHLY_" + symbol,
                () -> provider.getMonthly(symbol));
    }
}
