package cine.modelo;

import java.math.BigDecimal;

/** Entrada general: paga el precio completo de la función. */
public class EntradaGeneral extends Entrada {

    private static final long serialVersionUID = 1L;

    public EntradaGeneral(Butaca butaca, BigDecimal precioBase) {
        super(butaca, precioBase);
    }

    @Override
    protected BigDecimal calcularPrecio(BigDecimal precioBase) {
        return precioBase;
    }

    @Override
    public TipoEntrada getTipo() { return TipoEntrada.GENERAL; }
}
