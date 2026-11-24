package cine.modelo;

public enum EstadoReserva {
    CONFIRMADA("Confirmada"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() { return etiqueta; }
}
