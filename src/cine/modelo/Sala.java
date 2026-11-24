package cine.modelo;

import cine.excepciones.ValidacionException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Sala implements Identificable<Integer>, Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String nombre;
    private final int filas;
    private final int butacasPorFila;
    private final List<Butaca> butacas;

    public Sala(int id, String nombre, int filas, int butacasPorFila) throws ValidacionException {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("El nombre de la sala es obligatorio");
        }
        if (filas <= 0) {
            throw new ValidacionException("La cantidad de filas debe ser un entero positivo");
        }
        if (butacasPorFila <= 0) {
            throw new ValidacionException("Las butacas por fila deben ser un entero positivo");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.filas = filas;
        this.butacasPorFila = butacasPorFila;

        List<Butaca> generadas = new ArrayList<>();
        for (int f = 0; f < filas; f++) {
            String nombreFila = nombreFila(f);
            for (int n = 1; n <= butacasPorFila; n++) {
                generadas.add(new Butaca(nombreFila, n));
            }
        }
        this.butacas = List.copyOf(generadas);
    }

    /** 0 → A, 25 → Z, 26 → AA, 27 → AB... (sin límite de filas). */
    private static String nombreFila(int indice) {
        StringBuilder sb = new StringBuilder();
        int n = indice;
        do {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    @Override
    public Integer getId() { return id; }

    public String getNombre() { return nombre; }

    public int getFilas() { return filas; }

    public int getButacasPorFila() { return butacasPorFila; }

    public int getCapacidad() { return butacas.size(); }

    /** Lista inmutable, ordenada por fila y número. */
    public List<Butaca> getButacas() { return butacas; }

    /** Busca una butaca de ESTA sala por su código ("B3" o "b3"). */
    public Optional<Butaca> buscarButaca(String codigo) {
        if (codigo == null) return Optional.empty();
        String buscado = codigo.trim();
        for (Butaca b : butacas) {
            if (b.getCodigo().equalsIgnoreCase(buscado)) return Optional.of(b);
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sala)) return false;
        return id == ((Sala) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return nombre; }
}
