package com.api.parcial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aplicación Spring Boot principal de API de Acciones.
 * 
 * DESCRIPCIÓN:
 * Proporciona endpoints REST para consultar datos históricos de acciones
 * desde proveedores externos (Alpha Vantage, etc).
 * 
 * CARACTERÍSTICAS:
 * - Cache en memoria para optimizar consultas
 * - Soporte para múltiples intervalos de tiempo (diario, semanal, etc)
 * - Arquitectura SOLID con uso de interfaces y inyección de dependencias
 * - CORS habilitado para acceso desde frontends
 * 
 * ENDPOINTS DISPONIBLES:
 * - GET /stock/daily?symbol=AAPL
 * - GET /stock/intraday?symbol=AAPL
 * - GET /stock/weekly?symbol=AAPL
 * - GET /stock/monthly?symbol=AAPL
 */
@SpringBootApplication
public class ApiServiceApplication {

	private static final Logger logger = LoggerFactory.getLogger(ApiServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ApiServiceApplication.class, args);
		logger.info("=== API-Service iniciada correctamente ===");
		logger.info("Accede a: http://localhost:8080/stock/daily?symbol=AAPL");
	}

}
