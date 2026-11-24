package cine.servicios;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.excepciones.ValidacionException;
import cine.modelo.Pelicula;
import cine.persistencia.AlmacenCine;
import cine.persistencia.EstadoCine;
import cine.persistencia.Secuencia;
import java.util.List;

/**
 * Casos de uso de películas. Patrón de cada operación que modifica datos:
 * 1) validar sin modificar, 2) aplicar en memoria, 3) guardar,
 * 4) si el guardado falla, deshacer exactamente ese cambio y relanzar el error.
 */
public class PeliculaService {

    private final EstadoCine estado;
    private final AlmacenCine almacen;

    public PeliculaService(EstadoCine estado, AlmacenCine almacen) {
        this.estado = estado;
        this.almacen = almacen;
    }

    public Pelicula registrar(String titulo, int duracionMinutos)
            throws ReglaNegocioException, PersistenciaException {
        Secuencia secuencia = estado.getSecuenciaPeliculas();
        int id = secuencia.getProximo();
        Pelicula pelicula = new Pelicula(id, titulo, duracionMinutos);   // valida; si falla no se tocó nada
        secuencia.siguiente();
        estado.getPeliculas().agregar(pelicula);
        try {
            almacen.guardar(estado);
        } catch (PersistenciaException e) {
            estado.getPeliculas().quitar(id);
            secuencia.restaurar(id);
            throw e;
        }
        return pelicula;
    }

    /**
     * El título siempre puede cambiar. La duración solo si la película no tiene funciones,
     * porque cambiaría el horario de finalización y podría crear superposiciones.
     */
    public Pelicula modificar(int id, String titulo, int duracionMinutos)
            throws ReglaNegocioException, PersistenciaException {
        Pelicula pelicula = obtener(id);
        Pelicula.validar(titulo, duracionMinutos);
        String tituloAnterior = pelicula.getTitulo();
        int duracionAnterior = pelicula.getDuracionMinutos();
        if (duracionMinutos != duracionAnterior && tieneFunciones(pelicula)) {
            throw new ReglaNegocioException("No se puede cambiar la duración de '" + tituloAnterior
                + "': ya tiene funciones programadas y se alterarían sus horarios de finalización");
        }
        pelicula.modificar(titulo, duracionMinutos);
        try {
            almacen.guardar(estado);
        } catch (PersistenciaException e) {
            restaurar(pelicula, tituloAnterior, duracionAnterior);
            throw e;
        }
        return pelicula;
    }

    /** Baja lógica: la película y su historial se conservan. */
    public Pelicula desactivar(int id) throws ReglaNegocioException, PersistenciaException {
        Pelicula pelicula = obtener(id);
        if (!pelicula.isActiva()) {
            throw new ReglaNegocioException("La película '" + pelicula.getTitulo() + "' ya está desactivada");
        }
        pelicula.desactivar();
        try {
            almacen.guardar(estado);
        } catch (PersistenciaException e) {
            pelicula.reactivar();
            throw e;
        }
        return pelicula;
    }

    public Pelicula obtener(int id) throws ReglaNegocioException {
        return estado.getPeliculas().buscar(id)
            .orElseThrow(() -> new ReglaNegocioException("No existe la película con id " + id));
    }

    public List<Pelicula> listar() {
        return estado.getPeliculas().listar();
    }

    public List<Pelicula> listarActivas() {
        return estado.getPeliculas().listar().stream().filter(Pelicula::isActiva).toList();
    }

    private boolean tieneFunciones(Pelicula pelicula) {
        return estado.getFunciones().listar().stream().anyMatch(f -> f.getPelicula().equals(pelicula));
    }

    private void restaurar(Pelicula pelicula, String titulo, int duracion) {
        try {
            pelicula.modificar(titulo, duracion);
        } catch (ValidacionException e) {
            throw new IllegalStateException("Los valores anteriores de la película eran inválidos", e);
        }
    }
}
