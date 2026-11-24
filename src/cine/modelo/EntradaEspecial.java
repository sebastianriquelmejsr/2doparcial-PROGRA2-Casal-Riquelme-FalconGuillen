package cine.modelo;

import java.math.BigDecimal;


public class EntradaEspecial extends Entrada {

    private static final long serialVersionUID = 1L;

    /** Entrada especial (jubilados,Discapacitado, menores, estudiantes, ex/combatientes): 20 % de descuento sobre el precio general. */
    public static final BigDecimal DESCUENTO = new BigDecimal("0.20");

    public EntradaEspecial(Butaca butaca, BigDecimal precioBase) {
        super(butaca, precioBase);
    }

    @Override
    protected BigDecimal calcularPrecio(BigDecimal precioBase) {
        return precioBase.multiply(BigDecimal.ONE.subtract(DESCUENTO));
    }

    @Override
    public TipoEntrada getTipo() { return TipoEntrada.ESPECIAL; }
}
