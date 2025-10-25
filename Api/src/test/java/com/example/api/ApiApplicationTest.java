package com.example.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para ApiApplication
 * Ejecuta el servidor Spring Boot completo y prueba los endpoints
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
  }
)

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static String testToken;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String DATA_DIR = "data";

    @BeforeAll
    static void setupClass() {
        // Crear directorio de datos si no existe
        new File(DATA_DIR).mkdirs();
    }

    @AfterAll
    static void cleanupClass() {
        // Limpiar archivos de prueba
        try {
            Files.deleteIfExists(Paths.get(DATA_DIR + "/users.json"));
            Files.deleteIfExists(Paths.get(DATA_DIR + "/personal_info.json"));
            Files.deleteIfExists(Paths.get(DATA_DIR + "/financial_info.json"));
        } catch (IOException e) {
            System.err.println("Error al limpiar archivos de prueba: " + e.getMessage());
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    // =====================================================
    // PRUEBAS DE REGISTRO
    // =====================================================

    @Test
    @Order(1)
    @DisplayName("01 - Registro exitoso de nuevo usuario")
    void testRegistroExitoso() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", TEST_EMAIL);
        credentials.put("password", TEST_PASSWORD);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/registro",
            credentials,
            Map.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Usuario registrado exitosamente", response.getBody().get("mensaje"));
        assertNotNull(response.getBody().get("id"));
    }

    @Test
    @Order(2)
    @DisplayName("02 - Registro con email duplicado debe fallar")
    void testRegistroEmailDuplicado() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", TEST_EMAIL);
        credentials.put("password", "otherpassword");

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/registro",
            credentials,
            Map.class
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("El email ya está registrado", response.getBody().get("error"));
    }

    @Test
    @Order(3)
    @DisplayName("03 - Registro sin email debe fallar")
    void testRegistroSinEmail() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("password", TEST_PASSWORD);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/registro",
            credentials,
            Map.class
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Email y password son requeridos", response.getBody().get("error"));
    }

    @Test
    @Order(4)
    @DisplayName("04 - Registro sin password debe fallar")
    void testRegistroSinPassword() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", "nuevo@example.com");

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/registro",
            credentials,
            Map.class
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Email y password son requeridos", response.getBody().get("error"));
    }

    // =====================================================
    // PRUEBAS DE LOGIN
    // =====================================================

    @Test
    @Order(5)
    @DisplayName("05 - Login exitoso con credenciales correctas")
    void testLoginExitoso() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", TEST_EMAIL);
        credentials.put("password", TEST_PASSWORD);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/login",
            credentials,
            Map.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Login exitoso", response.getBody().get("mensaje"));
        assertNotNull(response.getBody().get("token"));
        
        // Guardar token para pruebas posteriores
        testToken = (String) response.getBody().get("token");
    }

    @Test
    @Order(8)
    @DisplayName("08 - Login sin credenciales debe fallar")
    void testLoginSinCredenciales() {
        Map<String, String> credentials = new HashMap<>();

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/login",
            credentials,
            Map.class
        );

        assertTrue(response.getStatusCode().is4xxClientError());
    }

    // =====================================================
    // PRUEBAS DE INFORMACIÓN PERSONAL
    // =====================================================

    @Test
    @Order(9)
    @DisplayName("09 - Guardar información personal con token válido")
    void testGuardarInfoPersonalExitoso() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Juan");
        datos.put("apellido", "Pérez");
        datos.put("edad", 30);
        datos.put("telefono", "1234567890");

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-access-token", testToken);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/info_personal",
            request,
            Map.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Información personal guardada correctamente", response.getBody().get("mensaje"));
        assertNotNull(response.getBody().get("id"));
    }

    @Test
    @Order(10)
    @DisplayName("10 - Guardar información personal sin token debe fallar")
    void testGuardarInfoPersonalSinToken() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "María");
        datos.put("apellido", "García");

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/info_personal",
            datos,
            Map.class
        );

        assertTrue(response.getStatusCode().is4xxClientError());
    }

   

    // =====================================================
    // PRUEBAS DE INFORMACIÓN FINANCIERA
    // =====================================================

    @Test
    @Order(12)
    @DisplayName("12 - Guardar información financiera con datos válidos")
    void testGuardarInfoFinancieraExitoso() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("ingreso", 5000000.0);
        datos.put("arriendoHipo", 1500000.0);
        datos.put("services", 300000.0);
        datos.put("alimentacion", 800000.0);
        datos.put("transporte", 400000.0);
        datos.put("otros", 200000.0);
        datos.put("fuenteIngreso", "Salario");

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-access-token", testToken);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/info_financiera",
            request,
            Map.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Información financiera guardada correctamente", response.getBody().get("mensaje"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> gastos = (Map<String, Object>) response.getBody().get("gastos");
        assertNotNull(gastos);
        assertEquals(5000000.0, ((Number) gastos.get("ingreso")).doubleValue(), 0.01);
        assertEquals(3200000.0, ((Number) gastos.get("totalGastos")).doubleValue(), 0.01);
        assertEquals(1800000.0, ((Number) gastos.get("disponible")).doubleValue(), 0.01);
    }

    @Test
    @Order(13)
    @DisplayName("13 - Guardar información financiera con valores como strings")
    void testGuardarInfoFinancieraConStrings() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("ingreso", "$3,000,000");
        datos.put("arriendoHipo", "$1,000,000");
        datos.put("services", "$200,000");
        datos.put("alimentacion", "$500,000");
        datos.put("transporte", "$300,000");
        datos.put("otros", "$100,000");
        datos.put("fuenteIngreso", "Freelance");

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-access-token", testToken);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/info_financiera",
            request,
            Map.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> gastos = (Map<String, Object>) response.getBody().get("gastos");
        assertNotNull(gastos);
        assertEquals(3000000.0, ((Number) gastos.get("ingreso")).doubleValue(), 0.01);
    }

    @Test
    @Order(14)
    @DisplayName("14 - Guardar información financiera sin token")
    void testGuardarInfoFinancieraSinToken() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("ingreso", 2000000.0);
        datos.put("arriendoHipo", 800000.0);
        datos.put("services", 150000.0);
        datos.put("alimentacion", 400000.0);
        datos.put("transporte", 200000.0);
        datos.put("otros", 100000.0);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/info_financiera",
            datos,
            Map.class
        );

        // Debe permitir guardado sin token (usa email por defecto)
        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    }

    @Test
    @Order(15)
    @DisplayName("15 - Obtener información financiera existente")
    void testObtenerInfoFinancieraExitosa() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-access-token", testToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/obtener_info_financiera",
            HttpMethod.GET,
            request,
            Map.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("ingreso"));
        assertNotNull(response.getBody().get("categorias"));
        assertNotNull(response.getBody().get("valores"));
    }

    @Test
    @Order(16)
    @DisplayName("16 - Obtener información financiera sin token")
    void testObtenerInfoFinancieraSinToken() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getBaseUrl() + "/obtener_info_financiera",
            Map.class
        );

        // Debe devolver valores por defecto
        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    // =====================================================
    // PRUEBAS DE OBTENER INFORMACIÓN PERSONAL
    // =====================================================

    @Test
    @Order(17)
    @DisplayName("17 - Obtener información personal con token válido")
    void testObtenerInfoPersonalExitoso() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-access-token", testToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            getBaseUrl() + "/obtener_info_personal",
            HttpMethod.GET,
            request,
            String.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    @Order(18)
    @DisplayName("18 - Obtener información personal sin token debe fallar")
    void testObtenerInfoPersonalSinToken() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getBaseUrl() + "/obtener_info_personal",
            Map.class
        );

        assertTrue(response.getStatusCode().is4xxClientError());
    }

    // =====================================================
    // PRUEBAS DE CÁLCULOS FINANCIEROS
    // =====================================================

    @Test
    @Order(19)
    @DisplayName("19 - Verificar cálculos financieros correctos")
    void testCalculosFinancierosCorrectos() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("ingreso", 10000000.0);
        datos.put("arriendoHipo", 2000000.0);
        datos.put("services", 500000.0);
        datos.put("alimentacion", 1500000.0);
        datos.put("transporte", 800000.0);
        datos.put("otros", 300000.0);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-access-token", testToken);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/info_financiera",
            request,
            Map.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> gastos = (Map<String, Object>) response.getBody().get("gastos");
        
        double totalEsperado = 2000000.0 + 500000.0 + 1500000.0 + 800000.0 + 300000.0;
        double disponibleEsperado = 10000000.0 - totalEsperado;
        
        assertEquals(totalEsperado, ((Number) gastos.get("totalGastos")).doubleValue(), 0.01);
        assertEquals(disponibleEsperado, ((Number) gastos.get("disponible")).doubleValue(), 0.01);
    }

    @Test
    @Order(20)
    @DisplayName("20 - Actualizar información financiera existente")
    void testActualizarInfoFinanciera() {
        // Primera inserción
        Map<String, Object> datos1 = new HashMap<>();
        datos1.put("ingreso", 4000000.0);
        datos1.put("arriendoHipo", 1000000.0);
        datos1.put("services", 200000.0);
        datos1.put("alimentacion", 600000.0);
        datos1.put("transporte", 300000.0);
        datos1.put("otros", 100000.0);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-access-token", testToken);
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(datos1, headers);

        restTemplate.postForEntity(
            getBaseUrl() + "/info_financiera",
            request1,
            Map.class
        );

        // Segunda inserción (actualización)
        Map<String, Object> datos2 = new HashMap<>();
        datos2.put("ingreso", 5000000.0);
        datos2.put("arriendoHipo", 1500000.0);
        datos2.put("services", 300000.0);
        datos2.put("alimentacion", 800000.0);
        datos2.put("transporte", 400000.0);
        datos2.put("otros", 200000.0);

        HttpEntity<Map<String, Object>> request2 = new HttpEntity<>(datos2, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            getBaseUrl() + "/info_financiera",
            request2,
            Map.class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> gastos = (Map<String, Object>) response.getBody().get("gastos");
        assertEquals(5000000.0, ((Number) gastos.get("ingreso")).doubleValue(), 0.01);
    }
}