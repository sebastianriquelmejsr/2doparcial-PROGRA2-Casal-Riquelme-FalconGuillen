package cine.modelo;

import java.io.Serializable;
import java.util.Objects;

/** Ubicación física de una sala. No guarda si está ocupada: eso depende de la función. */
public class Butaca implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fila;
    private final int numero;

    // Visibilidad de paquete: solo la Sala crea sus butacas (composición).
    Butaca(String fila, int numero) {
        this.fila = fila;
        this.numero = numero;
    }

    public String getFila() { return fila; }

    public int getNumero() { return numero; }

    /** Identificador legible, por ejemplo "B3". */
    public String getCodigo() { return fila + numero; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Butaca)) return false;
        Butaca otra = (Butaca) o;
        return numero == otra.numero && fila.equals(otra.fila);
    }

    @Override
    public int hashCode() { return Objects.hash(fila, numero); }

    @Override
    public String toString() { return getCodigo(); }
}
