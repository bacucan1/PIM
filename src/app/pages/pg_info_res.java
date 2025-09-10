package app.pages;


import app.session.UserSession;
import app.config.ApiConfig;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */

/**
 *
 * @author sergi
 */
public class pg_info_res extends javax.swing.JPanel {

    /**
     * Creates new form pg_info_res
     */
    public pg_info_res() {
        initComponents();
        jButton1.setText("Cargar datos");
        jButton1.addActionListener(e -> cargarDatos());

    }
    // ================================================
    // MÉTODO PARA TRAER DATOS DE LA API Y MOSTRARLOS
    // ================================================

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
    // MÉTODO PARA MOSTRAR EN LA TABLA
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

        public String getNombreCompleto() { return nombreCompleto; }
        public String getTipoDocumento() { return tipoDocumento; }
        public String getNumeroDocumento() { return numeroDocumento; }
        public String getFechaNacimiento() { return fechaNacimiento; }
        public int getEdad() { return edad; }
        public String getNacionalidad() { return nacionalidad; }

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
        jButton1 = new javax.swing.JButton();

        jPanel1.setBackground(new java.awt.Color(153, 153, 153));

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

        jButton1.setText("jButton1");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(217, 217, 217)
                        .addComponent(jButton1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 474, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(231, 231, 231)
                        .addComponent(jLabel1)))
                .addContainerGap(244, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addGap(41, 41, 41))
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
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tablaPersonas;
    // End of variables declaration//GEN-END:variables
}
