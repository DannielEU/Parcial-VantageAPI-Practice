package com.api.parcial.config;

import com.api.parcial.provider.AlphaVantageProvider;
import com.api.parcial.provider.StockProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de inyección de dependencias para proveedores.
 * Define qué implementación de StockProvider se usa en la aplicación.
 * 
 * PATRÓN STRATEGY:
 * - Interface StockProvider define el contrato
 * - AlphaVantageProvider es una implementación específica
 * - Este bean permite intercambiar la implementación sin cambiar el código
 * 
 * PARA AGREGAR OTRO PROVEEDOR:
 * 1. Crear nueva clase que implemente StockProvider (ej: YahooFinanceProvider)
 * 2. Modificar este @Bean para retornar la nueva implementación
 * 3. O crear un segundo provider bean y usar @Qualifier en inyecciones
 */
@Configuration
public class ProviderConfig {

    /**
     * Define el proveedor de datos de acciones a usar en toda la aplicación.
     * Actualmente usa Alpha Vantage.
     * Spring inyectará automáticamente la clave API desde properties
     *
     * @return Instancia del proveedor AlphaVantage
     */
    @Bean
    public StockProvider stockProvider(@Value("${alphavantage.api.key:demo}") String apiKey) {
        // Devuelve AlphaVantageProvider como el proveedor por defecto
        // Spring inyectará automáticamente la clave API desde properties
        return new AlphaVantageProvider(apiKey);
    }
}
