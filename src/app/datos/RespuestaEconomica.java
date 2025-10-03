package app.datos;

public class RespuestaEconomica {

    private InfoFinanciera info_financiera;
    private String mensaje;

    public InfoFinanciera getInfo_financiera() {
        return info_financiera;
    }

    public String getMensaje() {
        return mensaje;
    }

    public static class InfoFinanciera {

        private double ingreso_mensual;
        private Gastos gastos;
        private Totales totales;

        public double getIngreso_mensual() {
            return ingreso_mensual;
        }

        public Gastos getGastos() {
            return gastos;
        }

        public Totales getTotales() {
            return totales;
        }
    }

    public static class Gastos {

        private double alimentacion;
        private double arriendo_hipoteca;
        private double servicios;
        private double transporte;
        private double otros_gastos_fijos;

        public double getAlimentacion() {
            return alimentacion;
        }

        public double getArriendo_hipoteca() {
            return arriendo_hipoteca;
        }

        public double getServicios() {
            return servicios;
        }

        public double getTransporte() {
            return transporte;
        }

        public double getOtros_gastos_fijos() {
            return otros_gastos_fijos;
        }
    }

    public static class Totales {

        private double disponible;
        private double total_gastos;

        public double getDisponible() {
            return disponible;
        }

        public double getTotal_gastos() {
            return total_gastos;
        }
    }
}
