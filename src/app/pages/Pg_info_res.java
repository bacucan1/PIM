package app.pages;

import app.session.UserSession;

import app.services.ApiConfig;
import app.datos.Datos_eco;
import com.google.gson.Gson;
import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import app.services.GeminiService;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

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
        // === ESTILO Y COLORES ===
        Color COLOR_FONDO = new Color(245, 247, 250);
        Color COLOR_BOTON = new Color(33, 95, 246);
        Color COLOR_TEXTO_BOT = new Color(50, 50, 50);
        Color COLOR_TEXTO_USUARIO = new Color(9, 87, 173);
        Font FUENTE_TEXTO = new Font("Segoe UI", Font.PLAIN, 13);

        // === PANEL PRINCIPAL ===
        panelChatBot.setVisible(false);
        panelChatBot.setLayout(new BorderLayout(2, 2));
        panelChatBot.setBackground(COLOR_FONDO);
        panelChatBot.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));

        // === ÁREA DE CHAT ===
        JTextPane textPaneChat = new JTextPane();
        textPaneChat.setEditable(false);
        textPaneChat.setFont(FUENTE_TEXTO);
        textPaneChat.setBackground(Color.WHITE);

        JScrollPane scrollChat = new JScrollPane(textPaneChat);
        scrollChat.setPreferredSize(new Dimension(400, 400));
        scrollChat.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true));
        panelChatBot.add(scrollChat, BorderLayout.CENTER);

        // === PANEL INFERIOR ===
        textFieldInput = new JTextField();
        textFieldInput.setFont(FUENTE_TEXTO);
        textFieldInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                new EmptyBorder(3, 4, 3, 4)
        ));

        btnEnviar = new JButton("Enviar");
        btnEnviar.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        btnEnviar.setBackground(COLOR_BOTON);
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.setFocusPainted(false);
        btnEnviar.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 6));
        btnEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel panelInput = new JPanel(new BorderLayout(5, 0));
        panelInput.setBackground(COLOR_FONDO);
        panelInput.add(textFieldInput, BorderLayout.CENTER);
        panelInput.add(btnEnviar, BorderLayout.EAST);
        panelChatBot.add(panelInput, BorderLayout.SOUTH);

        // === MENSAJE DE BIENVENIDA AUTOMÁTICO ===
        String bienvenida = "🤖 ASVI:\n¡Hola! 👋 Soy tu asesor financiero virtual.\n"
                + "Pregúntame lo que quieras sobre tus finanzas.\n\n";

        appendToPane(textPaneChat, bienvenida, COLOR_TEXTO_BOT);
        historialChat.append(bienvenida); // Guardar en historial
        
        // === BOTON DE ENVIAR === \\
        btnEnviar.addActionListener(e -> {
            String mensaje = textFieldInput.getText().trim();
            if (mensaje.isEmpty()) {
                return;
            }

            // Mostrar mensaje del usuario y guardarlo en historial
            appendToPane(textPaneChat, "👤 Tú:\n" + mensaje + "\n\n", COLOR_TEXTO_USUARIO);
            historialChat.append("👤 Tú:\n").append(mensaje).append("\n\n");
            textFieldInput.setText("");

            // Animación del botón
            btnEnviar.setEnabled(false);
            final String baseText = "Analizando";
            final Timer timerAnim = new Timer(500, null);
            timerAnim.addActionListener(new ActionListener() {
                private int puntos = 0;

                @Override
                public void actionPerformed(ActionEvent e2) {
                    puntos = (puntos + 1) % 4;
                    btnEnviar.setText(baseText + ".".repeat(puntos));
                }
            });
            timerAnim.start();

            // Hilo para generar respuesta
            new Thread(() -> {
                try {
                    if (datosEcoActual == null) {
                        SwingUtilities.invokeLater(() -> {
                            timerAnim.stop();
                            btnEnviar.setText("Enviar");
                            btnEnviar.setEnabled(true);
                            appendToPane(textPaneChat, "ASVI: No se encontraron tus datos económicos.\n", COLOR_TEXTO_BOT);
                        });
                        return;
                    }

                    // Crear prompt con historial + datos económicos
                    String prompt = "Actúa como asesor financiero experto.\n"
                            + "Estos son los datos financieros del usuario:\n"
                            + "- Arriendo/Hipoteca: " + datosEcoActual.getArriendoHipo() + "\n"
                            + "- Servicios: " + datosEcoActual.getServices() + "\n"
                            + "- Alimentación: " + datosEcoActual.getAlimentacion() + "\n"
                            + "- Transporte: " + datosEcoActual.getTransporte() + "\n"
                            + "- Otros gastos: " + datosEcoActual.getOtros() + "\n"
                            + "- Dinero disponible: " + datosEcoActual.getDisponible() + "\n\n"
                            + "Historial de conversación:\n"
                            + historialChat.toString()
                            + "\nDale un consejo práctico y personalizado basado en estos datos, "
                            + "en 3-4 oraciones cortas, claro, conciso y amigable. "
                            + "Usa solo texto simple, sin asteriscos ni guiones.";

                    // Llamada al servicio de IA
                    String respuestaBot = GeminiService.generarRespuesta(prompt);

                    // Mostrar respuesta y agregarla al historial
                    SwingUtilities.invokeLater(() -> {
                        timerAnim.stop();
                        btnEnviar.setText("Enviar");
                        btnEnviar.setEnabled(true);
                        escribirTextoLento("🤖 ASVI:\n" + respuestaBot + "\n\n", textPaneChat, COLOR_TEXTO_BOT);
                        historialChat.append("🤖 ASVI:\n").append(respuestaBot).append("\n\n");
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        timerAnim.stop();
                        btnEnviar.setText("Enviar");
                        btnEnviar.setEnabled(true);
                        appendToPane(textPaneChat, "ASVI: Ocurrió un error al procesar tu mensaje.\n", COLOR_TEXTO_BOT);
                    });
                }
            }).start();
        });

    }

    /**
     * Efecto de escritura lenta del texto del bot.
     */
    private void appendToPane(JTextPane tp, String msg, Color c) {
        StyledDocument doc = tp.getStyledDocument();
        Style style = tp.addStyle("style", null);
        StyleConstants.setForeground(style, c);
        StyleConstants.setFontFamily(style, "Segoe UI");
        StyleConstants.setFontSize(style, 12);
        try {
            doc.insertString(doc.getLength(), msg, style);
            tp.setCaretPosition(doc.getLength()); // Auto-scroll
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
        // === FUNCIONES AUXILIARES ===
    private void escribirTextoLento(String texto, JTextPane tp, Color c) {
        new Thread(() -> {
            for (char ch : texto.toCharArray()) {
                try {
                    SwingUtilities.invokeLater(() -> appendToPane(tp, String.valueOf(ch), c));
                    Thread.sleep(10); // Velocidad de escritura
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    private void actualizarResumen() {
        try {
            cargarDatos();

            Datos_eco eco = obtenerDatosEconomicos();
            if (eco != null) {
                datosEcoActual = eco; // Guardar los datos para el chatbot
                mostrarGrafico(eco);
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
            //System.out.println("📥 Respuesta API (info financiera): " + jsonResponse);

            // === PARSEAR JSON DIRECTAMENTE (nuevo formato) ===
            Gson gson = new Gson();
            java.util.Map<String, Object> datos = gson.fromJson(jsonResponse, java.util.Map.class);

            if (datos == null || datos.isEmpty()) {
                // System.out.println("⚠️ No hay datos económicos.");
                return null;
            }

            // Verificar si hay datos válidos
            double ingreso = getDoubleValue(datos, "ingreso");
            if (ingreso == 0.0) {
                //System.out.println("⚠️ No hay información económica registrada.");
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
            return eco;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al obtener datos económicos: " + e.getMessage());
            return null;
        }
    }

    // Método auxiliar para extraer valores double del Map
    private double getDoubleValue(java.util.Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0.0;
        }
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

    private String generarRespuestaBot(String mensaje) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

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
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaPersonas = new javax.swing.JTable();
        J_grafica = new javax.swing.JPanel();
        Decoration2 = new javax.swing.JLabel();
        Subtitle1 = new javax.swing.JLabel();
        j_as = new javax.swing.JLabel();
        panelChatBot = new javax.swing.JPanel();
        btn_asvi = new javax.swing.JButton();
        Decoration1 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));
        setPreferredSize(new java.awt.Dimension(900, 468));
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Title.setFont(new java.awt.Font("Overpass", 1, 48)); // NOI18N
        Title.setForeground(new java.awt.Color(68, 75, 89));
        Title.setText("Información General");
        add(Title, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 20, -1, -1));
        add(jSeparatorTitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 70, 900, -1));

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
            .addGap(0, 492, Short.MAX_VALUE)
        );
        J_graficaLayout.setVerticalGroup(
            J_graficaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 304, Short.MAX_VALUE)
        );

        add(J_grafica, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 380, 492, 304));

        Decoration2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/decoration5.png"))); // NOI18N
        Decoration2.setText("jLabel2");
        add(Decoration2, new org.netbeans.lib.awtextra.AbsoluteConstraints(-170, -80, -1, -1));

        Subtitle1.setFont(new java.awt.Font("Overpass", 1, 36)); // NOI18N
        Subtitle1.setForeground(new java.awt.Color(68, 75, 89));
        Subtitle1.setText("Estadísticas");
        add(Subtitle1, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 330, -1, -1));

        j_as.setFont(new java.awt.Font("Overpass", 1, 14)); // NOI18N
        j_as.setForeground(new java.awt.Color(68, 75, 89));
        j_as.setText("ASVI");
        add(j_as, new org.netbeans.lib.awtextra.AbsoluteConstraints(762, 670, 50, -1));

        javax.swing.GroupLayout panelChatBotLayout = new javax.swing.GroupLayout(panelChatBot);
        panelChatBot.setLayout(panelChatBotLayout);
        panelChatBotLayout.setHorizontalGroup(
            panelChatBotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 230, Short.MAX_VALUE)
        );
        panelChatBotLayout.setVerticalGroup(
            panelChatBotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 225, Short.MAX_VALUE)
        );

        add(panelChatBot, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 380, 230, 225));

        btn_asvi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/IA_50.png"))); // NOI18N
        btn_asvi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_asviActionPerformed(evt);
            }
        });
        add(btn_asvi, new org.netbeans.lib.awtextra.AbsoluteConstraints(738, 610, -1, -1));

        Decoration1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/Decorations.png"))); // NOI18N
        Decoration1.setMaximumSize(new java.awt.Dimension(2, 2));
        add(Decoration1, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 200, -1, -1));
    }// </editor-fold>//GEN-END:initComponents

    private void btn_asviActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_asviActionPerformed
        panelChatBot.setVisible(!panelChatBot.isVisible());
    }//GEN-LAST:event_btn_asviActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Decoration1;
    private javax.swing.JLabel Decoration2;
    private javax.swing.JPanel J_grafica;
    private javax.swing.JLabel Subtitle1;
    private javax.swing.JLabel Title;
    private javax.swing.JButton btn_asvi;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparatorTitle;
    private javax.swing.JLabel j_as;
    private javax.swing.JPanel panelChatBot;
    private javax.swing.JTable tablaPersonas;
    // End of variables declaration//GEN-END:variables

    private JTextArea textAreaChat;
    private JTextField textFieldInput;
    private JButton btnEnviar;
    private Datos_eco datosEcoActual; // Esto almacena los datos económicos reales
private StringBuilder historialChat = new StringBuilder();
}
