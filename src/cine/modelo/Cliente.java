package cine.modelo;

import cine.excepciones.ValidacionException;
import java.io.Serializable;
import java.util.Objects;

public class Cliente implements Identificable<String>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String identificacion;
    private final String nombre;

    public Cliente(String identificacion, String nombre) throws ValidacionException {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("El nombre del cliente es obligatorio");
        }
        if (!nombre.trim().matches("[\\p{L} ]+")) {
            throw new ValidacionException("El nombre solo puede contener letras y espacios (sin números ni símbolos)");
        }
        if (identificacion == null || identificacion.isBlank()) {
            throw new ValidacionException("La identificación del cliente es obligatoria");
        }
        String id = identificacion.trim();
        if (!id.matches("\\d{8}")) {
            throw new ValidacionException("El documento debe tener exactamente 8 números (sin puntos, letras ni espacios)");
        }
        this.identificacion = id;
        this.nombre = nombre.trim();
    }

    @Override
    public String getId() { return identificacion; }

    public String getIdentificacion() { return identificacion; }

    public String getNombre() { return nombre; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente)) return false;
        return identificacion.equals(((Cliente) o).identificacion);
    }

    @Override
    public int hashCode() { return Objects.hash(identificacion); }

    @Override
    public String toString() { return nombre + " (" + identificacion + ")"; }
}
