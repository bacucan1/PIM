package app.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeminiService {
    
    private static final String API_KEY = "AIzaSyDQe-Sp5xlAiGAAJawDdhgbRyyzv1TLH9o";
    private static final String API_URL
            = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    private static final Gson gson = new Gson();

    public static String generarRespuesta(String prompt) {
        try {
            // Construir JSON request con Gson
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);
            
            String jsonBody = gson.toJson(requestBody);
            
            // Crear conexión HTTP
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            // Enviar request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // Leer respuesta
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
                br.close();
                
                // Parsear respuesta con Gson
                return extraerTextoRespuesta(response.toString());
            } else {
                // Leer error
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "utf-8"));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line.trim());
                }
                br.close();
                return "Error HTTP " + responseCode + ": " + errorResponse.toString();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al conectar con la IA: " + e.getMessage();
        }
    }
    
    // Extraer texto de respuesta JSON usando Gson
    private static String extraerTextoRespuesta(String json) {
        try {
            JsonObject jsonResponse = gson.fromJson(json, JsonObject.class);
            
            // Navegar por la estructura JSON
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                return "No se recibió respuesta del modelo";
            }
            
            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject content = firstCandidate.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            JsonObject firstPart = parts.get(0).getAsJsonObject();
            
            return firstPart.get("text").getAsString();
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al parsear respuesta: " + e.getMessage() + "\nJSON: " + json;
        }
    }
}