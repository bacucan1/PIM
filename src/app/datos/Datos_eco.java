/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app.datos;

public class Datos_eco {

    private String fuenteIngresos;
    private double ingreso;
    private double arriendoHipo;
    private double services;
    private double alimentacion;
    private double transporte;
    private double otros;
    private double totalGastos;
    private double disponible;
    private String fuenteIngreso;

    // Constructor vacío
    public Datos_eco() {
    }

    // Constructor con parámetros
    public Datos_eco(String fuenteIngresos, double ingreso, double arriendoHipo,
            double services, double alimentacion, double transporte) {
        this.fuenteIngresos = fuenteIngresos;
        this.ingreso = ingreso;
        this.arriendoHipo = arriendoHipo;
        this.services = services;
        this.alimentacion = alimentacion;
        this.transporte = transporte;
    }

    // Getters y Setters
    public String getFuenteIngresos() {
        return fuenteIngresos;
    }

    public void setFuenteIngresos(String fuenteIngresos) {
        this.fuenteIngresos = fuenteIngresos;
    }

    public double getIngreso() {
        return ingreso;
    }

    public void setIngreso(double ingreso) {
        this.ingreso = ingreso;
    }

    public double getArriendoHipo() {
        return arriendoHipo;
    }

    public void setArriendoHipo(double arriendoHipo) {
        this.arriendoHipo = arriendoHipo;
    }

    public double getServices() {
        return services;
    }

    public void setServices(double services) {
        this.services = services;
    }

    public double getAlimentacion() {
        return alimentacion;
    }

    public void setAlimentacion(double alimentacion) {
        this.alimentacion = alimentacion;
    }

    public double getTransporte() {
        return transporte;
    }

    public void setTransporte(double transporte) {
        this.transporte = transporte;
    }

    public double getOtros() {
        return otros;
    }

    public void setOtros(double otros) {
        this.otros = otros;
    }

    public void calcularTotales() {
        this.totalGastos = arriendoHipo + services + alimentacion + transporte + otros;
        this.disponible = ingreso - totalGastos;
    }

    // Métodos corregidos
    public double getTotalGastos() {
        return totalGastos;
    }

    public double getDisponible() {
        return disponible;
    }    
// Getters y Setters
    public String getFuenteIngreso() {
        return fuenteIngreso;
    }

    public void setFuenteIngreso(String fuenteIngreso) {
        this.fuenteIngreso = fuenteIngreso;
    }
}
