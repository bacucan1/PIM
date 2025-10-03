/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.config;

import app.session.UserSession;
import app.datos.Datos_eco;
import com.google.gson.Gson;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ApiEconomicosService {

    public static void enviarDatosEconomicos(Datos_eco eco, Runnable onSuccess, Runnable onFailure) {
        try {
            String token = UserSession.getInstance().getToken();
            if (token == null || token.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "No hay sesión activa. Por favor inicia sesión.",
                        "Error de Sesión",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Convertir el objeto a JSON
            Gson gson = new Gson();
            String jsonEco = gson.toJson(eco);
            System.out.println("📤 JSON enviado a la API:");
            System.out.println(jsonEco);

            // Enviar de forma asíncrona
            ApiClient.enviarDatos(ApiConfig.PERSONAS_URL + "info_financiera", jsonEco, token)
                    .onSuccess(status -> {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null,
                                    "Datos económicos enviados correctamente.",
                                    "✅ Éxito",
                                    JOptionPane.INFORMATION_MESSAGE);

                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                        });
                    })
                    .onFailure(e -> {
                        SwingUtilities.invokeLater(() -> {
                            String mensaje = "Error al enviar datos: " + e.getMessage();

                            if (e.getMessage().contains("401")) {
                                mensaje = "Sesión expirada. Por favor inicia sesión nuevamente.";
                                UserSession.getInstance().clearSession();
                            } else if (e.getMessage().contains("400")) {
                                mensaje = "Los datos enviados son inválidos. Verifica la información.";
                            } else if (e.getMessage().contains("500")) {
                                mensaje = "Error en el servidor. Intenta más tarde.";
                            }

                            JOptionPane.showMessageDialog(null, mensaje, "❌ Error", JOptionPane.ERROR_MESSAGE);

                            if (onFailure != null) {
                                onFailure.run();
                            }
                        });
                    });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error inesperado: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);

        }
    }

}
