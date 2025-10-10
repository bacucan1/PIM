package app.pages;

import app.session.UserSession;

import app.config.ApiConfig;
import app.datos.Datos_eco;
import app.datos.RespuestaEconomica;
import com.google.gson.Gson;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;


/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
/**
 *
 * @author sergi
 */
public class Pg_info_res extends javax.swing.JPanel {

    /**
     * Creates new form pg_info_res
     */
    public Pg_info_res() {
        initComponents();

        // ACTUALIZACIÓN AUTOMÁTICA AL CREAR EL PANEL
        new Thread(() -> {
            try {
                // Espera breve para dejar que se cargue la UI
                Thread.sleep(50);
                actualizarResumen(); // carga todo automáticamente
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void actualizarResumen() {
        try {
            //1️⃣ Cargar datos personales
            cargarDatos();

            //2️⃣ Cargar datos económicos
            Datos_eco datosEco = obtenerDatosEconomicos();

            //3️⃣ Mostrar la gráfica
            if (datosEco != null) {
                mostrarGrafico(datosEco);
               // System.out.println("✅ Gráfico actualizado automáticamente.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al actualizar el resumen: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================
    // MÉTODO PARA TRAER DATOS DE LA API Y MOSTRARLOS
    // ================================================
    private Datos_eco obtenerDatosEconomicos() {
        try {
            String token = UserSession.getInstance().getToken();
            if (token == null || token.isEmpty()) {
                System.out.println("⚠️ No hay token, usando default");
                token = ""; // La API acepta sin token usando default@example.com
            }

            // === HACER GET A LA API ===
            URL url = new URL(ApiConfig.PERSONAS_URL + "obtener_info_financiera");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (!token.isEmpty()) {
                conn.setRequestProperty("x-access-token", token);
            }

            int status = conn.getResponseCode();
            BufferedReader br = (status >= 200 && status < 300)
                    ? new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))
                    : new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            conn.disconnect();

            String jsonResponse = response.toString();
            System.out.println("📥 Respuesta API (info financiera): " + jsonResponse);

            // === PARSEAR JSON DIRECTAMENTE (nuevo formato) ===
            Gson gson = new Gson();
            java.util.Map<String, Object> datos = gson.fromJson(jsonResponse, java.util.Map.class);

            if (datos == null || datos.isEmpty()) {
                System.out.println("⚠️ No hay datos económicos.");
                return null;
            }

            // Verificar si hay datos válidos
            double ingreso = getDoubleValue(datos, "ingreso");
            if (ingreso == 0.0) {
                System.out.println("⚠️ No hay información económica registrada.");
                return null;
            }

            // === CREAR OBJETO Datos_eco CON LOS DATOS ===
            Datos_eco eco = new Datos_eco();
            eco.setIngreso(ingreso);
            eco.setArriendoHipo(getDoubleValue(datos, "arriendoHipo"));
            eco.setServices(getDoubleValue(datos, "services"));
            eco.setAlimentacion(getDoubleValue(datos, "alimentacion"));
            eco.setTransporte(getDoubleValue(datos, "transporte"));
            eco.setOtros(getDoubleValue(datos, "otros"));
            eco.setDisponible(getDoubleValue(datos, "disponible"));
            eco.setFuenteIngreso(datos.getOrDefault("fuenteIngreso", "No especificado").toString());

            // Calcular totales por si acaso
            eco.calcularTotales();

            System.out.println("✅ Datos económicos cargados:");
            System.out.println("   Ingreso: " + eco.getIngreso());
            System.out.println("   Arriendo: " + eco.getArriendoHipo());
            System.out.println("   Servicios: " + eco.getServices());
            System.out.println("   Alimentación: " + eco.getAlimentacion());
            System.out.println("   Transporte: " + eco.getTransporte());
            System.out.println("   Otros: " + eco.getOtros());
            System.out.println("   Disponible: " + eco.getDisponible());

            return eco;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error al obtener datos económicos: " + e.getMessage());
            return null;
        }
    }

    // Método auxiliar para extraer valores double del Map
    private double getDoubleValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void cargarDatos() {
        try {
            // En el método cargarDatos, antes de hacer la conexión:
            String token = UserSession.getInstance().getToken();
            if (token == null) {
                JOptionPane.showMessageDialog(this, "No hay sesión activa. Por favor, inicie sesión.");
                return;
            }

// Modifica la conexión agregando el header del token:
            URL url = new URL(ApiConfig.PERSONAS_URL + "obtener_info_personal");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("x-access-token", token); // Agregar el token

            int status = conn.getResponseCode();

            BufferedReader br;
            if (status >= 200 && status < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            conn.disconnect();

            String jsonResponse = response.toString();
            System.out.println("Respuesta GET: " + jsonResponse);

            Gson gson = new Gson();

            if (jsonResponse.startsWith("{")) {
                // Caso: objeto con lista de personas adentro
                RespuestaPersonas respuesta = gson.fromJson(jsonResponse, RespuestaPersonas.class);
                mostrarEnTabla(respuesta.getPersonas().toArray(new Persona[0]));

                JOptionPane.showMessageDialog(this,
                        "Total de registros: " + respuesta.getTotal_registros());

            } else if (jsonResponse.startsWith("[")) {
                // Caso: array directo
                Persona[] personas = gson.fromJson(jsonResponse, Persona[].class);
                mostrarEnTabla(personas);

            } else {
                // Caso: solo mensaje en texto
                JOptionPane.showMessageDialog(this, jsonResponse);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al obtener datos: " + e.getMessage());
        }
    }
    // ================================================
    // MÉTODOS PARA MOSTRAR DATOS
    // ================================================

    private void mostrarEnTabla(Persona[] personas) {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Nombre completo");
        model.addColumn("Tipo de documento");
        model.addColumn("Número de documento");
        model.addColumn("Fecha de nacimiento");
        model.addColumn("Edad");
        model.addColumn("Nacionalidad");

        for (Persona p : personas) {
            model.addRow(new Object[]{
                p.getNombreCompleto(),
                p.getTipoDocumento(),
                p.getNumeroDocumento(),
                p.getFechaNacimiento(),
                p.getEdad(),
                p.getNacionalidad()
            });
        }

        tablaPersonas.setModel(model);
    }

    private void mostrarGrafico(Datos_eco datosEco) {
        // Crear dataset (los datos del pastel)
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Arriendo / Hipoteca", datosEco.getArriendoHipo());
        dataset.setValue("Servicios", datosEco.getServices());
        dataset.setValue("Alimentación", datosEco.getAlimentacion());
        dataset.setValue("Transporte", datosEco.getTransporte());
        dataset.setValue("Otros", datosEco.getOtros());
        dataset.setValue("Disponible", datosEco.getDisponible());

        // Crear el gráfico
        JFreeChart chart = ChartFactory.createPieChart(
                "Distribución de Gastos Mensuales", // título
                dataset, // datos
                true, // leyenda
                true, // tooltips
                false // URLs
        );

        // Crear panel para insertar el gráfico en tu JPanel
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(J_grafica.getWidth(), J_grafica.getHeight()));

        // Limpiar el panel antes de agregar el nuevo gráfico
        J_grafica.removeAll();
        J_grafica.setLayout(new java.awt.BorderLayout());
        J_grafica.add(chartPanel, java.awt.BorderLayout.CENTER);
        J_grafica.validate();

    }
// =====================================================
// Clase auxiliar Persona (ajustada a tu JSON real)
// =====================================================

    public class Persona {

        private String nombreCompleto;
        private String tipoDocumento;
        private String numeroDocumento;
        private String fechaNacimiento;
        private int edad;
        private String nacionalidad;

        public String getNombreCompleto() {
            return nombreCompleto;
        }

        public String getTipoDocumento() {
            return tipoDocumento;
        }

        public String getNumeroDocumento() {
            return numeroDocumento;
        }

        public String getFechaNacimiento() {
            return fechaNacimiento;
        }

        public int getEdad() {
            return edad;
        }

        public String getNacionalidad() {
            return nacionalidad;
        }

    }
    // =====================================================
// Clase auxiliar para mapear la respuesta del JSON
// =====================================================

    public class RespuestaPersonas {

        private List<Persona> personas;
        private int total_registros;

        public List<Persona> getPersonas() {
            return personas;
        }

        public int getTotal_registros() {
            return total_registros;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Title = new javax.swing.JLabel();
        jSeparatorTitle = new javax.swing.JSeparator();
        Subtitle = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaPersonas = new javax.swing.JTable();
        J_grafica = new javax.swing.JPanel();
        Decoration1 = new javax.swing.JLabel();
        Decoration2 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));
        setPreferredSize(new java.awt.Dimension(900, 468));
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Title.setFont(new java.awt.Font("Overpass", 1, 48)); // NOI18N
        Title.setForeground(new java.awt.Color(68, 75, 89));
        Title.setText("Información General");
        add(Title, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 20, -1, -1));
        add(jSeparatorTitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 70, 900, -1));

        Subtitle.setFont(new java.awt.Font("Overpass", 1, 36)); // NOI18N
        Subtitle.setForeground(new java.awt.Color(68, 75, 89));
        Subtitle.setText("Estadísticas");
        add(Subtitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 370, -1, -1));

        jPanel1.setBackground(new java.awt.Color(250, 250, 250));
        jPanel1.setPreferredSize(new java.awt.Dimension(900, 468));
        jPanel1.setVerifyInputWhenFocusTarget(false);
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tablaPersonas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, "", null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Nombre", "Documento", "Edad", "Nacionalidad", "Fecha de nacimiento"
            }
        ));
        tablaPersonas.setRowHeight(40);
        jScrollPane1.setViewportView(tablaPersonas);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 745, 230));

        add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 90, 770, 250));

        javax.swing.GroupLayout J_graficaLayout = new javax.swing.GroupLayout(J_grafica);
        J_grafica.setLayout(J_graficaLayout);
        J_graficaLayout.setHorizontalGroup(
            J_graficaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 490, Short.MAX_VALUE)
        );
        J_graficaLayout.setVerticalGroup(
            J_graficaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        add(J_grafica, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 400, 490, 300));

        Decoration1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/Decorations.png"))); // NOI18N
        add(Decoration1, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 250, -1, -1));

        Decoration2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/decoration5.png"))); // NOI18N
        Decoration2.setText("jLabel2");
        add(Decoration2, new org.netbeans.lib.awtextra.AbsoluteConstraints(-170, -80, -1, -1));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Decoration1;
    private javax.swing.JLabel Decoration2;
    private javax.swing.JPanel J_grafica;
    private javax.swing.JLabel Subtitle;
    private javax.swing.JLabel Title;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparatorTitle;
    private javax.swing.JTable tablaPersonas;
    // End of variables declaration//GEN-END:variables
}
