/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.model;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class ManejadorIngresos {

    // Fuente de ingresos
    private String fuenteSeleccionada = "";
    
    // Ingreso mensual
    private double ingresoMensual = 0.0;
    private NumberFormat formatoMoneda;

    public ManejadorIngresos() {
        // Formato moneda colombiana
        formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        formatoMoneda.setMaximumFractionDigits(0);
    }

    // --- Métodos para Fuente de Ingresos ---

    public void procesarSeleccion(JComboBox<String> comboBox) {
        String seleccion = (String) comboBox.getSelectedItem();
        if (seleccion != null && !seleccion.equals("Selecciona una opción")) {
            this.fuenteSeleccionada = seleccion;
            comboBox.setBackground(new Color(220, 255, 220));
            System.out.println("Fuente de ingresos seleccionada: " + this.fuenteSeleccionada);
        } else {
            this.fuenteSeleccionada = "";
            comboBox.setBackground(Color.WHITE);
            System.out.println("Selección limpiada");
        }
    }

    public String getFuenteSeleccionada() {
        return this.fuenteSeleccionada;
    }

    public String getFuenteParaJSON() {
        switch (this.fuenteSeleccionada) {
            case "Salario/Empleo fijo": return "salario";
            case "Trabajo independiente": return "freelance";
            case "Negocio propio": return "negocio";
            case "Pensión": return "pension";
            case "Inversiones": return "inversion";
            case "Otros": return "otros";
            default: return "";
        }
    }

    public boolean tieneSeleccionValida() {
        return !this.fuenteSeleccionada.isEmpty();
    }

    public void limpiarSeleccion() {
        this.fuenteSeleccionada = "";
    }

    public void establecerFuente(String fuente, JComboBox<String> comboBox) {
        String opcionComboBox = "";
        switch (fuente.toLowerCase()) {
            case "salario": opcionComboBox = "Salario/Empleo fijo"; break;
            case "freelance": opcionComboBox = "Trabajo independiente"; break;
            case "negocio": opcionComboBox = "Negocio propio"; break;
            case "pension": opcionComboBox = "Pensión"; break;
            case "inversion": opcionComboBox = "Inversiones"; break;
            case "otros": opcionComboBox = "Otros"; break;
        }
        if (!opcionComboBox.isEmpty()) {
            comboBox.setSelectedItem(opcionComboBox);
            this.fuenteSeleccionada = opcionComboBox;
            comboBox.setBackground(new Color(220, 255, 220));
        }
    }

    // --- Métodos para Ingreso Mensual ---

    public void procesarIngreso(JTextField textField) {
        String textoIngresado = textField.getText();
        try {
            String numeroLimpio = textoIngresado.replaceAll("[^0-9]", "");
            if (numeroLimpio.isEmpty()) {
                this.ingresoMensual = 0.0;
                textField.setText("$ 0");
                textField.setBackground(Color.WHITE);
                System.out.println("Ingreso limpiado: $ 0");
                return;
            }
            double ingreso = Double.parseDouble(numeroLimpio);
            if (ingreso <= 0) {
                mostrarError("El ingreso debe ser mayor a cero", textField);
                return;
            }
            if (ingreso > 100000000) {
                mostrarError("El ingreso parece muy alto. Verifica el monto.", textField);
                return;
            }
            this.ingresoMensual = ingreso;
            String montoFormateado = formatoMoneda.format(ingreso);
            textField.setText(montoFormateado);
            textField.setBackground(new Color(220, 255, 220));
            System.out.println("Ingreso mensual guardado: " + montoFormateado);
        } catch (NumberFormatException e) {
            mostrarError("Por favor ingresa solo números", textField);
        }
    }

    public void prepararParaEdicion(JTextField textField) {
        if (this.ingresoMensual > 0) {
            textField.setText(String.valueOf((long)this.ingresoMensual));
        } else {
            textField.setText("");
        }
        textField.setBackground(Color.WHITE);
    }

    public boolean validarEntrada(String texto) {
        return texto.matches("[0-9]*");
    }

    private void mostrarError(String mensaje, JTextField textField) {
        JOptionPane.showMessageDialog(null, mensaje, "Error de Validación", JOptionPane.ERROR_MESSAGE);
        if (this.ingresoMensual > 0) {
            textField.setText(formatoMoneda.format(this.ingresoMensual));
            textField.setBackground(new Color(220, 255, 220));
        } else {
            textField.setText("$ 0");
            textField.setBackground(Color.WHITE);
        }
    }

    public double getIngresoMensual() {
        return this.ingresoMensual;
    }

    public String getIngresoFormateado() {
        return formatoMoneda.format(this.ingresoMensual);
    }

    public boolean tieneIngresoValido() {
        return this.ingresoMensual > 0;
    }

    public void establecerIngreso(double ingreso, JTextField textField) {
        if (ingreso >= 0) {
            this.ingresoMensual = ingreso;
            if (ingreso > 0) {
                textField.setText(formatoMoneda.format(ingreso));
                textField.setBackground(new Color(220, 255, 220));
            } else {
                textField.setText("$ 0");
                textField.setBackground(Color.WHITE);
            }
        }
    }

    public void limpiarIngreso(JTextField textField) {
        this.ingresoMensual = 0.0;
        textField.setText("$ 0");
        textField.setBackground(Color.WHITE);
    }

    public String formatearComoMoneda(double cantidad) {
        return formatoMoneda.format(cantidad);
    }
}

