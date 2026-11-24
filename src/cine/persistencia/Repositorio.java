package cine.persistencia;

import cine.modelo.Identificable;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Colección con clave, reutilizada para películas, salas, funciones, clientes y reservas.
 * No contiene reglas de negocio: solo guarda, busca y lista.
 */
public class Repositorio<T extends Identificable<K>, K> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<K, T> elementos = new LinkedHashMap<>();

    public void agregar(T elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("El elemento no puede ser nulo");
        }
        K id = elemento.getId();
        if (id == null) {
            throw new IllegalArgumentException("El elemento no tiene identificador");
        }
        if (elementos.containsKey(id)) {
            throw new IllegalArgumentException("Ya existe un elemento con id " + id);
        }
        elementos.put(id, elemento);
    }

    public Optional<T> buscar(K id) {
        return Optional.ofNullable(elementos.get(id));
    }

    public List<T> listar() {
        return List.copyOf(elementos.values());
    }

    /** Solo lo usan los servicios para revertir un alta cuando falla el guardado. */
    public void quitar(K id) {
        elementos.remove(id);
    }
}
