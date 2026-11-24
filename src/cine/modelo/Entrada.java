package cine.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Entrada de una reserva: una butaca con su precio confirmado.
 * El precio se calcula una sola vez, al crearla, delegando en la subclase (polimorfismo),
 * y queda guardado: si después cambia el precio de la función, no se altera.
 */
public abstract class Entrada implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Butaca butaca;
    private final BigDecimal precio;

    /** Las subclases no deben depender de campos propios en calcularPrecio (se llama desde acá). */
    protected Entrada(Butaca butaca, BigDecimal precioBase) {
        this.butaca = Objects.requireNonNull(butaca, "La butaca es obligatoria");
        Objects.requireNonNull(precioBase, "El precio base es obligatorio");
        this.precio = calcularPrecio(precioBase).setScale(2, RoundingMode.HALF_UP);
    }

    /** Regla de precio propia de cada tipo de entrada. */
    protected abstract BigDecimal calcularPrecio(BigDecimal precioBase);

    public abstract TipoEntrada getTipo();

    public Butaca getButaca() { return butaca; }

    /** Precio confirmado de esta entrada. */
    public BigDecimal getPrecio() { return precio; }

    @Override
    public String toString() {
        return butaca.getCodigo() + " · " + getTipo().getEtiqueta() + " · " + precio.toPlainString();
    }
}
