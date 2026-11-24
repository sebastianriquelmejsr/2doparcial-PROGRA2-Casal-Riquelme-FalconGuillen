package cine.modelo;

import cine.excepciones.ValidacionException;
import java.io.Serializable;
import java.util.Objects;

public class Pelicula implements Identificable<Integer>, Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private String titulo;
    private int duracionMinutos;
    private boolean activa;

    public Pelicula(int id, String titulo, int duracionMinutos) throws ValidacionException {
        this.id = id;
        this.titulo = validarTitulo(titulo);
        this.duracionMinutos = validarDuracion(duracionMinutos);
        this.activa = true;
    }

    /** Valida título y duración sin crear ni modificar nada. */
    public static void validar(String titulo, int duracionMinutos) throws ValidacionException {
        validarTitulo(titulo);
        validarDuracion(duracionMinutos);
    }

    private static String validarTitulo(String titulo) throws ValidacionException {
        if (titulo == null || titulo.isBlank()) {
            throw new ValidacionException("El título es obligatorio");
        }
        return titulo.trim();
    }

    private static int validarDuracion(int minutos) throws ValidacionException {
        if (minutos <= 0) {
            throw new ValidacionException("La duración debe ser un número positivo de minutos");
        }
        return minutos;
    }

    @Override
    public Integer getId() { return id; }

    public String getTitulo() { return titulo; }

    public int getDuracionMinutos() { return duracionMinutos; }

    public boolean isActiva() { return activa; }

    /** Cambia título y duración juntos: si alguno es inválido, no se cambia ninguno. */
    public void modificar(String nuevoTitulo, int nuevaDuracion) throws ValidacionException {
        String t = validarTitulo(nuevoTitulo);
        int d = validarDuracion(nuevaDuracion);
        this.titulo = t;
        this.duracionMinutos = d;
    }

    /** Baja lógica: la película se conserva para el historial. */
    public void desactivar() { this.activa = false; }

    /** Solo se usa para revertir una desactivación si falla el guardado. */
    public void reactivar() { this.activa = true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pelicula)) return false;
        return id == ((Pelicula) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return titulo + " (" + duracionMinutos + " min)"; }
}
