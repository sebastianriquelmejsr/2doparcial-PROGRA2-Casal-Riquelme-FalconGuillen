package cine.servicios;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.excepciones.ValidacionException;
import cine.modelo.Funcion;
import cine.modelo.Pelicula;
import cine.modelo.Sala;
import cine.persistencia.AlmacenCine;
import cine.persistencia.EstadoCine;
import cine.persistencia.Secuencia;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FuncionService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EstadoCine estado;
    private final AlmacenCine almacen;
    private final Clock reloj;

    public FuncionService(EstadoCine estado, AlmacenCine almacen, Clock reloj) {
        this.estado = estado;
        this.almacen = almacen;
        this.reloj = reloj;
    }

    /**
     * Programa una función. Una función nunca cambia de película, sala ni horario, así que
     * esta es la única oportunidad de crear una superposición y se controla acá.
     */
    public Funcion programar(int idPelicula, int idSala, LocalDateTime inicio, BigDecimal precioGeneral)
            throws ReglaNegocioException, PersistenciaException {
        Pelicula pelicula = estado.getPeliculas().buscar(idPelicula)
            .orElseThrow(() -> new ReglaNegocioException("No existe la película con id " + idPelicula));
        Sala sala = estado.getSalas().buscar(idSala)
            .orElseThrow(() -> new ReglaNegocioException("No existe la sala con id " + idSala));
        if (!pelicula.isActiva()) {
            throw new ReglaNegocioException("La película '" + pelicula.getTitulo()
                + "' está desactivada: no se pueden programar nuevas funciones");
        }
        if (inicio == null) {
            throw new ValidacionException("La fecha y hora de inicio son obligatorias");
        }
        if (!inicio.isAfter(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("La función debe comenzar en una fecha y hora futuras");
        }
        Secuencia secuencia = estado.getSecuenciaFunciones();
        int id = secuencia.getProximo();
        Funcion candidata = new Funcion(id, pelicula, sala, inicio, precioGeneral);   // valida el precio
        for (Funcion existente : estado.getFunciones().listar()) {
            if (candidata.seSuperponeCon(existente)) {
                throw new ReglaNegocioException("La sala " + sala.getNombre() + " ya tiene una función entre las "
                    + HORA.format(existente.getInicio()) + " y las " + HORA.format(existente.getFin())
                    + " del " + FECHA.format(existente.getInicio()));
            }
        }
        secuencia.siguiente();
        estado.getFunciones().agregar(candidata);
        try {
            almacen.guardar(estado);
        } catch (PersistenciaException e) {
            estado.getFunciones().quitar(id);
            secuencia.restaurar(id);
            throw e;
        }
        return candidata;
    }

    /**
     * Cambia el precio general de la función. Las reservas ya confirmadas conservan sus
     * importes, porque cada entrada guarda su propio precio.
     */
    public Funcion modificarPrecio(int idFuncion, BigDecimal nuevoPrecio)
            throws ReglaNegocioException, PersistenciaException {
        Funcion funcion = obtener(idFuncion);
        BigDecimal anterior = funcion.getPrecioGeneral();
        funcion.setPrecioGeneral(nuevoPrecio);   // valida; si falla no se cambió nada
        try {
            almacen.guardar(estado);
        } catch (PersistenciaException e) {
            try {
                funcion.setPrecioGeneral(anterior);
            } catch (ValidacionException ex) {
                throw new IllegalStateException("El precio anterior de la función era inválido", ex);
            }
            throw e;
        }
        return funcion;
    }

    public Funcion obtener(int id) throws ReglaNegocioException {
        return estado.getFunciones().buscar(id)
            .orElseThrow(() -> new ReglaNegocioException("No existe la función con id " + id));
    }

    /** Todas las funciones (pasadas y futuras, de películas activas o no), ordenadas por inicio. */
    public List<Funcion> listar() {
        return estado.getFunciones().listar().stream()
            .sorted(Comparator.comparing(Funcion::getInicio))
            .toList();
    }

    /**
     * Funciones disponibles: no comenzadas y de películas activas.
     * Filtros opcionales: texto contenido en el título de la película y fecha exacta.
     */
    public List<Funcion> cartelera(String textoPelicula, LocalDate fecha) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String filtro = textoPelicula == null ? "" : textoPelicula.trim().toLowerCase(Locale.ROOT);
        return estado.getFunciones().listar().stream()
            .filter(f -> !f.haComenzado(ahora))
            .filter(f -> f.getPelicula().isActiva())
            .filter(f -> filtro.isEmpty()
                || f.getPelicula().getTitulo().toLowerCase(Locale.ROOT).contains(filtro))
            .filter(f -> fecha == null || f.getInicio().toLocalDate().equals(fecha))
            .sorted(Comparator.comparing(Funcion::getInicio))
            .toList();
    }
}
