package com.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.util.*;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")
public class ApiApplication {
    private final String DATA_DIR = "data";
    private final String USERS_FILE = "data/users.json";
    private final String PERSONAL_INFO_FILE = "data/personal_info.json";
    private final String FINANCIAL_INFO_FILE = "data/financial_info.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Key signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    @PostConstruct
    public void init() {
        new File(DATA_DIR).mkdirs();
        initializeJsonFile(USERS_FILE);
        initializeJsonFile(PERSONAL_INFO_FILE);
        initializeJsonFile(FINANCIAL_INFO_FILE);
    }

    private void initializeJsonFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                mapper.writeValue(file, mapper.createArrayNode());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayNode readJsonArray(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return mapper.createArrayNode();
        }
        return (ArrayNode) mapper.readTree(file);
    }

    private void writeJsonArray(String filePath, ArrayNode array) throws IOException {
        mapper.writeValue(new File(filePath), array);
    }

    private double parseNumericValue(Object value) {
        if (value == null) return 0.0;
        
        try {
            if (value instanceof String) {
                String strValue = ((String) value)
                    .replace("$", "")
                    .replace(",", "")
                    .replace(" ", "")
                    .trim();
                return strValue.isEmpty() ? 0.0 : Double.parseDouble(strValue);
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Exception e) {
            // Si hay error, retornar 0
        }
        return 0.0;
    }

    private String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(signingKey)
                .compact();
    }

    private String validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody Map<String, String> credentials) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email y password son requeridos"));
            }

            ArrayNode users = readJsonArray(USERS_FILE);
            
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).get("email").asText().equals(email)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "El email ya está registrado"));
                }
            }

            ObjectNode newUser = mapper.createObjectNode();
            newUser.put("id", UUID.randomUUID().toString());
            newUser.put("email", email);
            newUser.put("password", passwordEncoder.encode(password));
            users.add(newUser);

            writeJsonArray(USERS_FILE, users);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Usuario registrado exitosamente",
                "id", newUser.get("id").asText()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en el registro: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email y password son requeridos"));
            }

            ArrayNode users = readJsonArray(USERS_FILE);
            
            for (int i = 0; i < users.size(); i++) {
                ObjectNode user = (ObjectNode) users.get(i);
                if (user.get("email").asText().equals(email)) {
                    if (passwordEncoder.matches(password, user.get("password").asText())) {
                        String token = generateToken(email);
                        return ResponseEntity.ok(Map.of(
                            "mensaje", "Login exitoso",
                            "token", token
                        ));
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Contraseña incorrecta"));
                    }
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario no encontrado"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error en el login: " + e.getMessage()));
        }
    }

    @PostMapping("/info_personal")
    public ResponseEntity<?> guardarInfoPersonal(@RequestHeader("x-access-token") String token, 
                                               @RequestBody Map<String, Object> datos) {
        String email = validateToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token inválido"));
        }

        try {
            ArrayNode infoPersonal = readJsonArray(PERSONAL_INFO_FILE);
            
            ObjectNode newInfo = mapper.createObjectNode();
            newInfo.put("id", UUID.randomUUID().toString());
            newInfo.put("email", email);
            newInfo.put("timestamp", new Date().toString());
            
            datos.forEach((key, value) -> {
                if (value instanceof String) {
                    newInfo.put(key, (String) value);
                } else if (value instanceof Integer) {
                    newInfo.put(key, (Integer) value);
                } else if (value instanceof Long) {
                    newInfo.put(key, (Long) value);
                } else if (value instanceof Double) {
                    newInfo.put(key, (Double) value);
                } else if (value instanceof Float) {
                    newInfo.put(key, (Float) value);
                } else if (value instanceof Boolean) {
                    newInfo.put(key, (Boolean) value);
                } else if (value instanceof Number) {
                    newInfo.put(key, ((Number) value).doubleValue());
                }
            });

            infoPersonal.add(newInfo);
            writeJsonArray(PERSONAL_INFO_FILE, infoPersonal);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Información personal guardada correctamente",
                "id", newInfo.get("id").asText()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la información: " + e.getMessage()));
        }
    }

    @PostMapping("/info_financiera")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> guardarInfoFinanciera(
            @RequestHeader(value = "x-access-token", required = false) String token,
            @RequestBody Map<String, Object> datos) {
        try {
            String email = "default@example.com";
            if (token != null && !token.isEmpty()) {
                String validatedEmail = validateToken(token);
                if (validatedEmail != null) {
                    email = validatedEmail;
                }
            }

            System.out.println("🔍 Datos recibidos completos: " + datos);

            double ingreso = parseNumericValue(datos.get("ingreso"));
            double arriendoHipo = parseNumericValue(datos.get("arriendoHipo"));
            double services = parseNumericValue(datos.get("services"));
            double alimentacion = parseNumericValue(datos.get("alimentacion"));
            double transporte = parseNumericValue(datos.get("transporte"));
            double otros = parseNumericValue(datos.get("otros"));
            
            String fuenteIngreso = datos.containsKey("fuenteIngreso") 
                ? datos.get("fuenteIngreso").toString() 
                : datos.getOrDefault("fuenteIngresos", "No especificado").toString();
            
            double totalGastos = arriendoHipo + services + alimentacion + transporte + otros;
            double disponible = ingreso - totalGastos;

            System.out.println("💰 Ingreso: " + ingreso);
            System.out.println("💸 Total Gastos: " + totalGastos);
            System.out.println("✅ Disponible: " + disponible);

            ArrayNode infoFinanciera = readJsonArray(FINANCIAL_INFO_FILE);
            
            boolean encontrado = false;
            for (int i = 0; i < infoFinanciera.size(); i++) {
                ObjectNode registro = (ObjectNode) infoFinanciera.get(i);
                if (registro.get("email").asText().equals(email)) {
                    registro.put("timestamp", new Date().toString());
                    registro.put("fuenteIngreso", fuenteIngreso);
                    
                    ObjectNode gastosInfo = mapper.createObjectNode();
                    gastosInfo.put("ingreso", ingreso);
                    gastosInfo.put("arriendoHipo", arriendoHipo);
                    gastosInfo.put("services", services);
                    gastosInfo.put("alimentacion", alimentacion);
                    gastosInfo.put("transporte", transporte);
                    gastosInfo.put("otros", otros);
                    gastosInfo.put("totalGastos", totalGastos);
                    gastosInfo.put("disponible", disponible);
                    
                    registro.set("gastos", gastosInfo);
                    encontrado = true;
                    break;
                }
            }
            
            if (!encontrado) {
                ObjectNode newInfo = mapper.createObjectNode();
                newInfo.put("id", UUID.randomUUID().toString());
                newInfo.put("email", email);
                newInfo.put("timestamp", new Date().toString());
                newInfo.put("fuenteIngreso", fuenteIngreso);
                
                ObjectNode gastosInfo = mapper.createObjectNode();
                gastosInfo.put("ingreso", ingreso);
                gastosInfo.put("arriendoHipo", arriendoHipo);
                gastosInfo.put("services", services);
                gastosInfo.put("alimentacion", alimentacion);
                gastosInfo.put("transporte", transporte);
                gastosInfo.put("otros", otros);
                gastosInfo.put("totalGastos", totalGastos);
                gastosInfo.put("disponible", disponible);
                
                newInfo.set("gastos", gastosInfo);
                infoFinanciera.add(newInfo);
            }
            
            writeJsonArray(FINANCIAL_INFO_FILE, infoFinanciera);

            System.out.println("✅ Datos guardados exitosamente");

            ObjectNode response = mapper.createObjectNode();
            response.put("ingreso", ingreso);
            response.put("arriendoHipo", arriendoHipo);
            response.put("services", services);
            response.put("alimentacion", alimentacion);
            response.put("transporte", transporte);
            response.put("otros", otros);
            response.put("totalGastos", totalGastos);
            response.put("disponible", disponible);
            response.put("fuenteIngreso", fuenteIngreso);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Información financiera guardada correctamente",
                "gastos", response
            ));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al guardar la información: " + e.getMessage()));
        }
    }

    @GetMapping("/obtener_info_financiera")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> obtenerInfoFinanciera(
            @RequestHeader(value = "x-access-token", required = false) String token) {
        try {
            String email = "default@example.com";
            if (token != null && !token.isEmpty()) {
                String validatedEmail = validateToken(token);
                if (validatedEmail != null) {
                    email = validatedEmail;
                }
            }

            System.out.println("🔍 Buscando datos para email: " + email);

            ArrayNode infoFinanciera = readJsonArray(FINANCIAL_INFO_FILE);
            
            // Buscar desde el final (el más reciente)
            ObjectNode registroMasReciente = null;
            for (int i = infoFinanciera.size() - 1; i >= 0; i--) {
                ObjectNode registro = (ObjectNode) infoFinanciera.get(i);
                if (registro.get("email").asText().equals(email)) {
                    registroMasReciente = registro;
                    break; // Encontramos el más reciente
                }
            }
            
            if (registroMasReciente != null) {
                ObjectNode gastosObj = (ObjectNode) registroMasReciente.get("gastos");
                
                if (gastosObj != null) {
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("ingreso", gastosObj.path("ingreso").asDouble(0.0));
                    response.put("arriendoHipo", gastosObj.path("arriendoHipo").asDouble(0.0));
                    response.put("services", gastosObj.path("services").asDouble(0.0));
                    response.put("alimentacion", gastosObj.path("alimentacion").asDouble(0.0));
                    response.put("transporte", gastosObj.path("transporte").asDouble(0.0));
                    response.put("otros", gastosObj.path("otros").asDouble(0.0));
                    response.put("totalGastos", gastosObj.path("totalGastos").asDouble(0.0));
                    response.put("disponible", gastosObj.path("disponible").asDouble(0.0));
                    response.put("fuenteIngreso", registroMasReciente.path("fuenteIngreso").asText("No especificado"));
                    
                    response.put("categorias", java.util.Arrays.asList(
                        "Arriendo/Hipoteca", "Servicios", "Alimentación", 
                        "Transporte", "Otros"
                    ));
                    response.put("valores", java.util.Arrays.asList(
                        gastosObj.path("arriendoHipo").asDouble(0.0),
                        gastosObj.path("services").asDouble(0.0),
                        gastosObj.path("alimentacion").asDouble(0.0),
                        gastosObj.path("transporte").asDouble(0.0),
                        gastosObj.path("otros").asDouble(0.0)
                    ));
                    
                    System.out.println("📊 Datos encontrados para gráfica: " + response);
                    
                    return ResponseEntity.ok(response);
                }
            }

            System.out.println("⚠️ No se encontraron datos para el email: " + email);

            // Si no hay información, devolver valores por defecto
            Map<String, Object> defaultResponse = new java.util.HashMap<>();
            defaultResponse.put("ingreso", 0.0);
            defaultResponse.put("arriendoHipo", 0.0);
            defaultResponse.put("services", 0.0);
            defaultResponse.put("alimentacion", 0.0);
            defaultResponse.put("transporte", 0.0);
            defaultResponse.put("otros", 0.0);
            defaultResponse.put("totalGastos", 0.0);
            defaultResponse.put("disponible", 0.0);
            defaultResponse.put("fuenteIngreso", "");
            defaultResponse.put("categorias", java.util.Arrays.asList("Arriendo/Hipoteca", "Servicios", "Alimentación", "Transporte", "Otros"));
            defaultResponse.put("valores", java.util.Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0));

            return ResponseEntity.ok(defaultResponse);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al obtener la información: " + e.getMessage()));
        }
    }

    @GetMapping("/obtener_info_personal")
    public ResponseEntity<?> obtenerInfoPersonal(@RequestHeader("x-access-token") String token) {
        String email = validateToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token inválido"));
        }

        try {
            ArrayNode infoPersonal = readJsonArray(PERSONAL_INFO_FILE);
            ArrayNode userInfo = mapper.createArrayNode();
            
            for (int i = 0; i < infoPersonal.size(); i++) {
                if (infoPersonal.get(i).get("email").asText().equals(email)) {
                    userInfo.add(infoPersonal.get(i));
                }
            }

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener la información: " + e.getMessage()));
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}