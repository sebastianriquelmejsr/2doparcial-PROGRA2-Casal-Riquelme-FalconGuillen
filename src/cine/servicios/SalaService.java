package cine.servicios;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.modelo.Sala;
import cine.persistencia.AlmacenCine;
import cine.persistencia.EstadoCine;
import cine.persistencia.Secuencia;
import java.util.List;

public class SalaService {

    private final EstadoCine estado;
    private final AlmacenCine almacen;

    public SalaService(EstadoCine estado, AlmacenCine almacen) {
        this.estado = estado;
        this.almacen = almacen;
    }

    /** Registra la sala y genera sus butacas. El nombre no puede repetirse (sin distinguir mayúsculas). */
    public Sala registrar(String nombre, int filas, int butacasPorFila)
            throws ReglaNegocioException, PersistenciaException {
        Secuencia secuencia = estado.getSecuenciaSalas();
        int id = secuencia.getProximo();
        Sala sala = new Sala(id, nombre, filas, butacasPorFila);   // valida; si falla no se tocó nada
        for (Sala existente : estado.getSalas().listar()) {
            if (existente.getNombre().equalsIgnoreCase(sala.getNombre())) {
                throw new ReglaNegocioException("Ya existe una sala con el nombre '" + existente.getNombre() + "'");
            }
        }
        secuencia.siguiente();
        estado.getSalas().agregar(sala);
        try {
            almacen.guardar(estado);
        } catch (PersistenciaException e) {
            estado.getSalas().quitar(id);
            secuencia.restaurar(id);
            throw e;
        }
        return sala;
    }

    public Sala obtener(int id) throws ReglaNegocioException {
        return estado.getSalas().buscar(id)
            .orElseThrow(() -> new ReglaNegocioException("No existe la sala con id " + id));
    }

    public List<Sala> listar() {
        return estado.getSalas().listar();
    }
}
