package com.ezeiza.cartelera.controller.frontend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendForwardController {

    /*
     * Rutas React actuales.
     */
    @GetMapping(value = {
            "/",
            "/display",
            "/history",
            "/users"
    })
    public String forwardLegacyRoutesToReact() {
        return "forward:/index.html";
    }

    /*
     * Display público para Yodeck.
     *
     * Ejemplos:
     * /display/ezeiza
     * /display/aeroparque
     * /display/the-pro-laundry
     */
    @GetMapping("/display/{plantCode}")
    public String forwardDisplayPlantRouteToReact() {
        return "forward:/index.html";
    }

    /*
     * Ruta interactiva futura para usuarios.
     *
     * Ejemplos:
     * /app/ezeiza
     * /app/aeroparque
     *
     * En el futuro esta ruta autenticará con Entra ID.
     */
    @GetMapping("/app/{plantCode}")
    public String forwardAppPlantRouteToReact() {
        return "forward:/index.html";
    }
}