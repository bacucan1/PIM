package app.datos;

public class Datos_eco {
    private String fuenteIngreso; // Eliminar fuenteIngresos duplicado
    private double ingreso;
    private double arriendoHipo;
    private double services;
    private double alimentacion;
    private double transporte;
    private double otros;
    private double totalGastos;
    private double disponible;

    public Datos_eco() {
    }

    // Getters y Setters
    public String getFuenteIngreso() {
        return fuenteIngreso;
    }

    public void setFuenteIngreso(String fuenteIngreso) {
        this.fuenteIngreso = fuenteIngreso;
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

    public double getTotalGastos() {
        return totalGastos;
    }

    public void setTotalGastos(double totalGastos) {
        this.totalGastos = totalGastos;
    }

    public double getDisponible() {
        return disponible;
    }

    public void setDisponible(double disponible) {
        this.disponible = disponible;
    }

    public void calcularTotales() {
        this.totalGastos = arriendoHipo + services + alimentacion + transporte + otros;
        this.disponible = ingreso - totalGastos;
    }
}