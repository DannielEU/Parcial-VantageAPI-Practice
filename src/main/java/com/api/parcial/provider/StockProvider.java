package com.api.parcial.provider;

import com.api.parcial.model.StockResponse;

/**
 * Interfaz contratos para proveedores de datos de acciones.
 * Define los métodos que cualquier proveedor debe implementar.
 * 
 * ESTRATEGIA DE DISEÑO:
 * - Interface: Permite intercambiar proveedores fácilmente (AlphaVantage, Yahoo Finance, etc)
 * - Strategy Pattern: Cada implementación es una estrategia diferente
 * - Facilita testing: Se puede usar mock de esta interfaz
 * - Escalable: Agregar nuevo proveedor = Nueva implementación sin modificar código existente
 */
public interface StockProvider {
    
    /**
     * Obtiene datos intradiarios (cada 5 minutos) de una acción.
     * @param symbol Símbolo del ticker (ej: AAPL, GOOGL, MSFT)
     * @return StockResponse con precios/volumen cada 5 minutos
     */
    StockResponse getIntraday(String symbol);
    
    /**
     * Obtiene precios diarios de una acción.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios de cierre diarios
     */
    StockResponse getDaily(String symbol);
    
    /**
     * Obtiene precios semanales de una acción.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios semanales
     */
    StockResponse getWeekly(String symbol);
    
    /**
     * Obtiene precios mensuales de una acción.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios mensuales
     */
    StockResponse getMonthly(String symbol);
}
