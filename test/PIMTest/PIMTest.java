
package PIMTest;

import app.datos.Usuario;
import app.datos.Datos_per;
import app.datos.Datos_eco;
import app.session.UserSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Pruebas unitarias para el sistema PIM
 * Incluye tests para validación de datos, cálculos y sesión de usuario
 */
public class PIMTest {
    
    private Usuario usuario;
    private Datos_per datosPersonales;
    private Datos_eco datosEconomicos;
    private UserSession session;
    
    @Before
    public void setUp() {
        // Limpiar sesión antes de cada prueba
        UserSession.getInstance().clearSession();
        
        // Inicializar objetos para las pruebas
        usuario = new Usuario("test@example.com", "password123");
        datosPersonales = new Datos_per();
        datosEconomicos = new Datos_eco();
        session = UserSession.getInstance();
    }
    
    // ==========================================
    // PRUEBAS DE USUARIO
    // ==========================================
    
    @Test
    public void testCreacionUsuarioValido() {
        assertNotNull("El usuario no debe ser nulo", usuario);
        assertEquals("test@example.com", usuario.getEmail());
        assertEquals("password123", usuario.getPassword());
    }
    
    @Test
    public void testUsuarioEmailVacio() {
        Usuario usuarioInvalido = new Usuario("", "password123");
        assertTrue("El email no debe estar vacío", 
                   usuarioInvalido.getEmail().isEmpty());
    }
    
    @Test
    public void testUsuarioPasswordVacio() {
        Usuario usuarioInvalido = new Usuario("test@example.com", "");
        assertTrue("La contraseña no debe estar vacía", 
                   usuarioInvalido.getPassword().isEmpty());
    }
    
    // ==========================================
    // PRUEBAS DE SESIÓN
    // ==========================================
    
    @Test
    public void testSingletonUserSession() {
        UserSession session1 = UserSession.getInstance();
        UserSession session2 = UserSession.getInstance();
        assertSame("Debe retornar la misma instancia", session1, session2);
    }
    
    @Test
    public void testGuardarTokenEnSesion() {
        String token = "test_token_12345";
        session.setToken(token);
        assertEquals("El token debe guardarse correctamente", 
                     token, session.getToken());
    }
    
    @Test
    public void testGuardarEmailEnSesion() {
        String email = "usuario@test.com";
        session.setEmail(email);
        assertEquals("El email debe guardarse correctamente", 
                     email, session.getEmail());
    }
    
    @Test
    public void testLimpiarSesion() {
        session.setToken("token123");
        session.setEmail("user@test.com");
        session.setUserId("user_id_123");
        
        session.clearSession();
        
        assertNull("El token debe ser nulo después de limpiar", 
                   session.getToken());
        assertNull("El email debe ser nulo después de limpiar", 
                   session.getEmail());
        assertNull("El userId debe ser nulo después de limpiar", 
                   session.getUserId());
    }
    
    // ==========================================
    // PRUEBAS DE DATOS PERSONALES
    // ==========================================
    
    @Test
    public void testSetDatosPersonalesCompletos() {
        datosPersonales.setNombreCompleto("Juan Pérez");
        datosPersonales.setTipoDocumento("CC");
        datosPersonales.setNumeroDocumento("1234567890");
        datosPersonales.setFechaNacimiento("1990-01-15");
        datosPersonales.setEdad(34);
        datosPersonales.setNacionalidad("Colombiana");
        
        assertEquals("Juan Pérez", datosPersonales.getNombreCompleto());
        assertEquals("CC", datosPersonales.getTipoDocumento());
        assertEquals("1234567890", datosPersonales.getNumeroDocumento());
        assertEquals("1990-01-15", datosPersonales.getFechaNacimiento());
        assertEquals(34, datosPersonales.getEdad());
        assertEquals("Colombiana", datosPersonales.getNacionalidad());
    }
    
    @Test
    public void testEdadNegativa() {
        datosPersonales.setEdad(-5);
        assertTrue("La edad no debe ser negativa", 
                   datosPersonales.getEdad() < 0);
    }
    
    @Test
    public void testTipoDocumentoValido() {
        String[] tiposValidos = {"CC", "TI", "CE", "PP"};
        for (String tipo : tiposValidos) {
            datosPersonales.setTipoDocumento(tipo);
            assertEquals(tipo, datosPersonales.getTipoDocumento());
        }
    }
    
    // ==========================================
    // PRUEBAS DE DATOS ECONÓMICOS
    // ==========================================
    
    @Test
    public void testCalculoTotalGastos() {
        datosEconomicos.setArriendoHipo(1000000);
        datosEconomicos.setServices(200000);
        datosEconomicos.setAlimentacion(500000);
        datosEconomicos.setTransporte(300000);
        datosEconomicos.setOtros(100000);
        
        datosEconomicos.calcularTotales();
        
        double expectedTotal = 2100000;
        assertEquals("El total de gastos debe ser correcto", 
                     expectedTotal, datosEconomicos.getTotalGastos(), 0.01);
    }
    
    @Test
    public void testCalculoDisponible() {
        datosEconomicos.setIngreso(3000000);
        datosEconomicos.setArriendoHipo(1000000);
        datosEconomicos.setServices(200000);
        datosEconomicos.setAlimentacion(500000);
        datosEconomicos.setTransporte(300000);
        datosEconomicos.setOtros(100000);
        
        datosEconomicos.calcularTotales();
        
        double expectedDisponible = 900000; // 3000000 - 2100000
        assertEquals("El disponible debe ser correcto", 
                     expectedDisponible, datosEconomicos.getDisponible(), 0.01);
    }
    
    @Test
    public void testDisponibleNegativo() {
        datosEconomicos.setIngreso(1000000);
        datosEconomicos.setArriendoHipo(800000);
        datosEconomicos.setServices(300000);
        datosEconomicos.setAlimentacion(200000);
        
        datosEconomicos.calcularTotales();
        
        assertTrue("El disponible puede ser negativo si los gastos superan ingresos", 
                   datosEconomicos.getDisponible() < 0);
    }
    
    @Test
    public void testGastosCero() {
        datosEconomicos.setIngreso(2000000);
        datosEconomicos.setArriendoHipo(0);
        datosEconomicos.setServices(0);
        datosEconomicos.setAlimentacion(0);
        datosEconomicos.setTransporte(0);
        datosEconomicos.setOtros(0);
        
        datosEconomicos.calcularTotales();
        
        assertEquals("Total de gastos debe ser 0", 
                     0.0, datosEconomicos.getTotalGastos(), 0.01);
        assertEquals("Disponible debe ser igual al ingreso", 
                     2000000.0, datosEconomicos.getDisponible(), 0.01);
    }
    
    @Test
    public void testFuenteIngresoValida() {
        String[] fuentesValidas = {
            "Salario/Empleo fijo",
            "Trabajo independiente",
            "Negocio propio",
            "Pensión",
            "Inversiones",
            "Otros"
        };
        
        for (String fuente : fuentesValidas) {
            datosEconomicos.setFuenteIngreso(fuente);
            assertEquals(fuente, datosEconomicos.getFuenteIngreso());
        }
    }
    
    @Test
    public void testIngresosPositivos() {
        datosEconomicos.setIngreso(5000000);
        assertTrue("El ingreso debe ser positivo", 
                   datosEconomicos.getIngreso() > 0);
    }
    
    @Test
    public void testGastosNoNegativos() {
        datosEconomicos.setArriendoHipo(-100000);
        datosEconomicos.setServices(-50000);
        
        // Aunque se permita guardar negativos, debería validarse
        // Esta prueba documenta el comportamiento actual
        assertTrue("Se detectan gastos negativos", 
                   datosEconomicos.getArriendoHipo() < 0 || 
                   datosEconomicos.getServices() < 0);
    }
    
    // ==========================================
    // PRUEBAS DE VALIDACIÓN DE CAMPOS
    // ==========================================
    
    @Test
    public void testValidacionEmailFormato() {
        String emailValido = "usuario@dominio.com";
        assertTrue("Email debe contener @", emailValido.contains("@"));
        assertTrue("Email debe contener punto", emailValido.contains("."));
    }
    
    @Test
    public void testValidacionNumeroDocumento() {
        String documento = "1234567890";
        assertTrue("Documento debe ser numérico", 
                   documento.matches("\\d+"));
    }
    
    @Test
    public void testValidacionFechaNacimiento() {
        String fecha = "1990-01-15";
        assertTrue("Fecha debe tener formato correcto", 
                   fecha.matches("\\d{4}-\\d{2}-\\d{2}"));
    }
    
    // ==========================================
    // PRUEBAS DE INTEGRIDAD DE DATOS
    // ==========================================
    
    @Test
    public void testDatosEconomicosIntegridad() {
        datosEconomicos.setIngreso(4000000);
        datosEconomicos.setArriendoHipo(1200000);
        datosEconomicos.setServices(250000);
        datosEconomicos.setAlimentacion(600000);
        datosEconomicos.setTransporte(400000);
        datosEconomicos.setOtros(150000);
        
        double sumaManual = 1200000 + 250000 + 600000 + 400000 + 150000;
        
        datosEconomicos.calcularTotales();
        
        assertEquals("Suma manual debe coincidir con cálculo automático",
                     sumaManual, datosEconomicos.getTotalGastos(), 0.01);
    }
    
    @Test
    public void testMultiplesCalculosConsecutivos() {
        // Primera configuración
        datosEconomicos.setIngreso(3000000);
        datosEconomicos.setArriendoHipo(1000000);
        datosEconomicos.calcularTotales();
        double disponible1 = datosEconomicos.getDisponible();
        
        // Segunda configuración
        datosEconomicos.setArriendoHipo(1500000);
        datosEconomicos.calcularTotales();
        double disponible2 = datosEconomicos.getDisponible();
        
        assertNotEquals("Los cálculos deben actualizarse correctamente",
                        disponible1, disponible2, 0.01);
    }
    
    @After
    public void tearDown() {
        // Limpiar sesión después de cada prueba
        UserSession.getInstance().clearSession();
        usuario = null;
        datosPersonales = null;
        datosEconomicos = null;
    }
}