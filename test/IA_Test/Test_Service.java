package IA_Test;

import app.services.GeminiService;

public class Test_Service {

    public static void main(String[] args) {
        System.out.println("Consultando a la IA...\n");

        String pregunta = "Explícame qué es Java en 2 oraciones";
        String respuesta = GeminiService.generarRespuesta(pregunta);

        System.out.println("Pregunta: " + pregunta);
        System.out.println("\nRespuesta:\n" + respuesta);
    }
}
