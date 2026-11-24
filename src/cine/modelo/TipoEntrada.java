package cine.modelo;

import java.math.BigDecimal;

public enum TipoEntrada {
    GENERAL("General"),
    ESPECIAL("Especial");

    private final String etiqueta;

    TipoEntrada(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() { return etiqueta; }

    /** Crea la entrada concreta; el cálculo del precio lo resuelve cada subclase. */
    public Entrada crearEntrada(Butaca butaca, BigDecimal precioBase) {
        return switch (this) {
            case GENERAL -> new EntradaGeneral(butaca, precioBase);
            case ESPECIAL -> new EntradaEspecial(butaca, precioBase);
        };
    }
}
