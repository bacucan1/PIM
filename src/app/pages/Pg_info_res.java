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
                JOptionPane.showMessageDialog(this, "No hay sesión activa. Por favor, inicie sesión.");
                return null;
            }

            // === VERIFICAR SI EXISTE INFORMACIÓN FINANCIERA ===
            URL checkUrl = new URL(ApiConfig.PERSONAS_URL + "obtener_info_financiera");
            HttpURLConnection checkConn = (HttpURLConnection) checkUrl.openConnection();
            checkConn.setRequestMethod("GET");
            checkConn.setRequestProperty("Accept", "application/json");
            checkConn.setRequestProperty("x-access-token", token);

            int checkStatus = checkConn.getResponseCode();
            BufferedReader checkBr = (checkStatus >= 200 && checkStatus < 300)
                    ? new BufferedReader(new InputStreamReader(checkConn.getInputStream(), "utf-8"))
                    : new BufferedReader(new InputStreamReader(checkConn.getErrorStream(), "utf-8"));

            StringBuilder checkResponse = new StringBuilder();
            String line;
            while ((line = checkBr.readLine()) != null) {
                checkResponse.append(line.trim());
            }
            checkConn.disconnect();

            String jsonResponse = checkResponse.toString();
            System.out.println("📥 Respuesta API (info financiera): " + jsonResponse);

            // Si no hay información registrada, no continuar
            if (jsonResponse.contains("\"info_financiera\":null")
                    || jsonResponse.contains("No se encontró información financiera")
                    || jsonResponse.trim().equals("{}")) {

                System.out.println("⚠️ No hay información económica registrada para este usuario. Se omite el GET.");
                return null; // <- evita error, no muestra mensaje
            }

            // === SI HAY INFORMACIÓN, PROCESAR NORMALMENTE ===
            Gson gson = new Gson();
            RespuestaEconomica respuesta = gson.fromJson(jsonResponse, RespuestaEconomica.class);

            if (respuesta == null || respuesta.getInfo_financiera() == null) {
                System.out.println("⚠️ No hay datos económicos válidos.");
                return null;
            }

            Datos_eco eco = new Datos_eco();
            eco.setArriendoHipo(respuesta.getInfo_financiera().getGastos().getArriendo_hipoteca());
            eco.setServices(respuesta.getInfo_financiera().getGastos().getServicios());
            eco.setAlimentacion(respuesta.getInfo_financiera().getGastos().getAlimentacion());
            eco.setTransporte(respuesta.getInfo_financiera().getGastos().getTransporte());
            eco.setOtros(respuesta.getInfo_financiera().getGastos().getOtros_gastos_fijos());
            eco.setDisponible(respuesta.getInfo_financiera().getTotales().getDisponible());
            eco.setIngreso(respuesta.getInfo_financiera().getIngreso_mensual());
            eco.calcularTotales();

            return eco;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al obtener datos económicos: " + e.getMessage());
            return null;
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

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaPersonas = new javax.swing.JTable();
        J_grafica = new javax.swing.JPanel();

        setPreferredSize(new java.awt.Dimension(900, 468));

        jPanel1.setBackground(new java.awt.Color(153, 153, 153));
        jPanel1.setPreferredSize(new java.awt.Dimension(900, 468));
        jPanel1.setVerifyInputWhenFocusTarget(false);

        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Resumen Principal");

        tablaPersonas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Nombre", "Documento", "Edad", "Nacionalidad", "Fecha de nacimiento"
            }
        ));
        tablaPersonas.setRowHeight(40);
        jScrollPane1.setViewportView(tablaPersonas);

        javax.swing.GroupLayout J_graficaLayout = new javax.swing.GroupLayout(J_grafica);
        J_grafica.setLayout(J_graficaLayout);
        J_graficaLayout.setHorizontalGroup(
            J_graficaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        J_graficaLayout.setVerticalGroup(
            J_graficaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 314, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(231, 231, 231)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(J_grafica, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 505, Short.MAX_VALUE))))
                .addContainerGap(105, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addComponent(J_grafica, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(23, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel J_grafica;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tablaPersonas;
    // End of variables declaration//GEN-END:variables
}
