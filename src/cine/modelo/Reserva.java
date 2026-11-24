package cine.modelo;

import cine.excepciones.ReglaNegocioException;
import cine.excepciones.ValidacionException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Reserva implements Identificable<String>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String codigo;
    private final Cliente cliente;
    private final Funcion funcion;
    private final List<Entrada> entradas;
    private final LocalDateTime fechaCreacion;
    private EstadoReserva estado;
    private LocalDateTime fechaCancelacion;

    public Reserva(String codigo, Cliente cliente, Funcion funcion, List<Entrada> entradas,
                   LocalDateTime fechaCreacion) throws ValidacionException {
        if (codigo == null || codigo.isBlank()) {
            throw new ValidacionException("La reserva necesita un código");
        }
        if (cliente == null) throw new ValidacionException("El cliente es obligatorio");
        if (funcion == null) throw new ValidacionException("La función es obligatoria");
        if (fechaCreacion == null) throw new ValidacionException("La fecha de creación es obligatoria");
        if (entradas == null || entradas.isEmpty()) {
            throw new ValidacionException("La reserva debe incluir al menos una butaca");
        }
        Set<Butaca> vistas = new HashSet<>();
        for (Entrada e : entradas) {
            if (!funcion.getSala().getButacas().contains(e.getButaca())) {
                throw new ValidacionException("La butaca " + e.getButaca().getCodigo()
                    + " no pertenece a " + funcion.getSala().getNombre());
            }
            if (!vistas.add(e.getButaca())) {
                throw new ValidacionException("La butaca " + e.getButaca().getCodigo()
                    + " está repetida en la reserva");
            }
        }
        this.codigo = codigo;
        this.cliente = cliente;
        this.funcion = funcion;
        this.entradas = List.copyOf(entradas);
        this.fechaCreacion = fechaCreacion;
        this.estado = EstadoReserva.CONFIRMADA;
    }

    @Override
    public String getId() { return codigo; }

    public String getCodigo() { return codigo; }

    public Cliente getCliente() { return cliente; }

    public Funcion getFuncion() { return funcion; }

    /** Lista inmutable de entradas (una por butaca). */
    public List<Entrada> getEntradas() { return entradas; }

    public EstadoReserva getEstado() { return estado; }

    public boolean isConfirmada() { return estado == EstadoReserva.CONFIRMADA; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    /** null mientras la reserva no esté cancelada. */
    public LocalDateTime getFechaCancelacion() { return fechaCancelacion; }

    /** Suma de los precios confirmados de las entradas. */
    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Entrada e : entradas) {
            total = total.add(e.getPrecio());
        }
        return total;
    }

    /** Una butaca está ocupada por esta reserva solo mientras la reserva esté confirmada. */
    public boolean ocupa(Butaca butaca) {
        if (!isConfirmada()) return false;
        for (Entrada e : entradas) {
            if (e.getButaca().equals(butaca)) return true;
        }
        return false;
    }

    /** Conserva todos los datos; al dejar de estar confirmada, libera las butacas. */
    public void cancelar(LocalDateTime ahora) throws ReglaNegocioException {
        if (estado == EstadoReserva.CANCELADA) {
            throw new ReglaNegocioException("La reserva " + codigo + " ya está cancelada");
        }
        this.estado = EstadoReserva.CANCELADA;
        this.fechaCancelacion = ahora;
    }

    /** Solo se usa para revertir una cancelación si falla el guardado. */
    public void deshacerCancelacion() {
        this.estado = EstadoReserva.CONFIRMADA;
        this.fechaCancelacion = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reserva)) return false;
        return codigo.equals(((Reserva) o).codigo);
    }

    @Override
    public int hashCode() { return Objects.hash(codigo); }

    @Override
    public String toString() { return codigo + " (" + estado.getEtiqueta() + ")"; }
}
