package com.api.parcial.cache;

import com.api.parcial.model.StockResponse;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Sistema de cache en memoria para datos de acciones.
 * Evita llamadas repetidas a la API externa dentro del mismo proceso.
 * 
 * CARACTERÍSTICAS:
 * - Thread-safe: Usa ConcurrentHashMap para acceso concurrente
 * - Lazy loading: Solo calcula/obtiene datos cuando se solicitan
 * - En memoria: Rápido pero se pierde al reiniciar la aplicación
 * 
 * MEJORAS FUTURAS:
 * - Agregar TTL (Time To Live) para invalidar cache automáticamente
 * - Integrar Redis para cache distribuido
 * - Agregar eventos de invalidación manual
 */
@Component
public class StockCache {

    private static final Logger logger = LoggerFactory.getLogger(StockCache.class);
    private final ConcurrentHashMap<String, StockResponse> cache = new ConcurrentHashMap<>();

    /**
     * Obtiene valor del cache o lo calcula si no existe.
     * Si la clave existe, devuelve el valor cacheado.
     * Si no existe, ejecuta el supplier (función) y guarda el resultado.
     * 
     * @param key Clave única del cache
     * @param supplier Función que calcula el valor si no está en cache
     * @return Valor cacheado o calculado
     */
    public StockResponse getOrCompute(String key, Supplier<StockResponse> supplier) {
        if (cache.containsKey(key)) {
            logger.debug("Hit en cache para: {}", key);
            return cache.get(key);
        } else {
            logger.debug("Miss en cache para: {}. Calculando valor...", key);
            return cache.computeIfAbsent(key, k -> supplier.get());
        }
    }

    /**
     * Limpia una entrada específica del cache.
     * @param key Clave a eliminar
     */
    public void invalidate(String key) {
        logger.info("Invalidando cache para: {}", key);
        cache.remove(key);
    }

    /**
     * Limpia todo el cache.
     */
    public void clear() {
        logger.warn("Limpiando todo el cache");
        cache.clear();
    }

    /**
     * Retorna el tamaño actual del cache.
     * @return Número de elementos en cache
     */
    public int size() {
        return cache.size();
    }
}
