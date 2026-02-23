package com.api.parcial.controller;

import com.api.parcial.model.StockResponse;
import com.api.parcial.service.StockFacadeService;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para consultas de datos de acciones.
 * Expone endpoints para recuperar precios de acciones desde diferentes APIs.
 * Utiliza la fachada para abstrae la lógica de negocio y cachéo.
 */
@RestController
@RequestMapping("/stock")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockFacadeService facade;

    /**
     * Constructor que inyecta la dependencia del servicio fachada
     * @param facade Servicio de fachada para operaciones de acciones
     */
    public StockController(StockFacadeService facade) {
        this.facade = facade;
    }

    /**
     * Obtiene los precios diarios de una acción.
     * Los datos se cachean para evitar múltiples llamadas a la API externa.
     * @param symbol Símbolo del ticker (ej: AAPL, GOOGL, MSFT)
     * @return StockResponse con precios diarios
     */
    @GetMapping("/daily")
    public StockResponse getDaily(@RequestParam String symbol) {
        return facade.getDaily(symbol);
    }

    /**
     * Obtiene los precios intradiarios de una acción (cada 5 minutos).
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios intradiarios
     */
    @GetMapping("/intraday")
    public StockResponse getIntraday(@RequestParam String symbol) {
        return facade.getIntraday(symbol);
    }

    /**
     * Obtiene los precios semanales de una acción.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios semanales
     */
    @GetMapping("/weekly")
    public StockResponse getWeekly(@RequestParam String symbol) {
        return facade.getWeekly(symbol);
    }

    /**
     * Obtiene los precios mensuales de una acción.
     * @param symbol Símbolo del ticker
     * @return StockResponse con precios mensuales
     */
    @GetMapping("/monthly")
    public StockResponse getMonthly(@RequestParam String symbol) {
        return facade.getMonthly(symbol);
    }
}