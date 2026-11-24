package cine.modelo;

import cine.excepciones.ValidacionException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

public class Funcion implements Identificable<Integer>, Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final Pelicula pelicula;
    private final Sala sala;
    private final LocalDateTime inicio;
    private BigDecimal precioGeneral;

    public Funcion(int id, Pelicula pelicula, Sala sala, LocalDateTime inicio,
                   BigDecimal precioGeneral) throws ValidacionException {
        if (pelicula == null) throw new ValidacionException("La película es obligatoria");
        if (sala == null) throw new ValidacionException("La sala es obligatoria");
        if (inicio == null) throw new ValidacionException("La fecha y hora de inicio son obligatorias");
        this.id = id;
        this.pelicula = pelicula;
        this.sala = sala;
        this.inicio = inicio;
        this.precioGeneral = validarPrecio(precioGeneral);
    }

    private static BigDecimal validarPrecio(BigDecimal precio) throws ValidacionException {
        if (precio == null || precio.signum() <= 0) {
            throw new ValidacionException("El precio debe ser mayor a cero");
        }
        return precio.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Integer getId() { return id; }

    public Pelicula getPelicula() { return pelicula; }

    public Sala getSala() { return sala; }

    public LocalDateTime getInicio() { return inicio; }

    public BigDecimal getPrecioGeneral() { return precioGeneral; }

    /** Se calcula, no se guarda: no puede quedar desactualizado respecto de la película. */
    public LocalDateTime getFin() {
        return inicio.plusMinutes(pelicula.getDuracionMinutos());
    }

    /** Único dato modificable de una función. No afecta las entradas ya confirmadas. */
    public void setPrecioGeneral(BigDecimal nuevoPrecio) throws ValidacionException {
        this.precioGeneral = validarPrecio(nuevoPrecio);
    }

    public boolean haComenzado(LocalDateTime ahora) {
        return !ahora.isBefore(inicio);
    }

    /** Misma sala y los intervalos [inicio, fin) se cruzan. */
    public boolean seSuperponeCon(Funcion otra) {
        return sala.equals(otra.sala)
            && inicio.isBefore(otra.getFin())
            && otra.inicio.isBefore(getFin());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Funcion)) return false;
        return id == ((Funcion) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return pelicula.getTitulo() + " · " + sala.getNombre() + " · " + inicio;
    }
}
