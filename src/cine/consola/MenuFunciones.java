package cine.consola;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.modelo.Funcion;
import cine.modelo.Pelicula;
import cine.modelo.Sala;
import cine.servicios.FuncionService;
import cine.servicios.PeliculaService;
import cine.servicios.ReservaService;
import cine.servicios.SalaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class MenuFunciones {

    private final LectorConsola lector;
    private final FuncionService funciones;
    private final PeliculaService peliculas;
    private final SalaService salas;
    private final ReservaService reservas;

    public MenuFunciones(LectorConsola lector, FuncionService funciones, PeliculaService peliculas,
                         SalaService salas, ReservaService reservas) {
        this.lector = lector;
        this.funciones = funciones;
        this.peliculas = peliculas;
        this.salas = salas;
        this.reservas = reservas;
    }

    public void ejecutar() {
        while (true) {
            lector.mostrar("\n--- FUNCIONES ---");
            lector.mostrar("1. Listar  2. Registrar  3. Modificar precio  4. Ver butacas  0. Volver");
            switch (lector.leerEntero("Opción", null)) {
                case 1 -> listar();
                case 2 -> registrar();
                case 3 -> modificarPrecio();
                case 4 -> verButacas();
                case 0 -> { return; }
                default -> lector.mostrarError("Opción inválida");
            }
        }
    }

    private void listar() {
        List<Funcion> todas = funciones.listar();
        if (todas.isEmpty()) {
            lector.mostrar("No hay funciones programadas.");
            return;
        }
        for (Funcion f : todas) {
            lector.mostrar(Formato.lineaFuncion(f) + (f.getPelicula().isActiva() ? "" : " | película desactivada"));
        }
    }

    private void registrar() {
        List<Pelicula> activas = peliculas.listarActivas();
        if (activas.isEmpty() || salas.listar().isEmpty()) {
            lector.mostrarError("Para programar una función hace falta al menos una película activa y una sala");
            return;
        }
        lector.mostrar("Películas activas:");
        for (Pelicula p : activas) {
            lector.mostrar("  [" + p.getId() + "] " + p);
        }
        lector.mostrar("Salas:");
        for (Sala s : salas.listar()) {
            lector.mostrar("  [" + s.getId() + "] " + s.getNombre() + " (capacidad " + s.getCapacidad() + ")");
        }
        Integer idPelicula = null;
        Integer idSala = null;
        LocalDate fecha = null;
        LocalTime hora = null;
        BigDecimal precio = null;
        while (true) {
            idPelicula = lector.leerEntero("Id de la película", idPelicula);
            idSala = lector.leerEntero("Id de la sala", idSala);
            fecha = lector.leerFecha("Fecha", fecha);
            hora = lector.leerHora("Hora de inicio", hora);
            precio = lector.leerPrecio("Precio general", precio);
            try {
                Funcion f = funciones.programar(idPelicula, idSala, fecha.atTime(hora), precio);
                lector.mostrar("Función registrada: " + Formato.lineaFuncion(f));
                return;
            } catch (ReglaNegocioException | PersistenciaException e) {
                lector.reportar(e);
                if (!lector.confirmar("¿Corregir los datos?")) {
                    return;
                }
            }
        }
    }

    private void modificarPrecio() {
        Funcion actual;
        try {
            actual = funciones.obtener(lector.leerEntero("Id de la función", null));
        } catch (ReglaNegocioException e) {
            lector.reportar(e);
            return;
        }
        lector.mostrar(Formato.lineaFuncion(actual));
        BigDecimal precio = actual.getPrecioGeneral();
        while (true) {
            precio = lector.leerPrecio("Nuevo precio general", precio);
            try {
                funciones.modificarPrecio(actual.getId(), precio);
                lector.mostrar("Precio actualizado. Las reservas ya confirmadas conservan su importe.");
                return;
            } catch (ReglaNegocioException | PersistenciaException e) {
                lector.reportar(e);
                if (!lector.confirmar("¿Corregir el precio?")) {
                    return;
                }
            }
        }
    }

    private void verButacas() {
        try {
            Funcion f = funciones.obtener(lector.leerEntero("Id de la función", null));
            lector.mostrar(Formato.funcion(f));
            lector.mostrarLineas(Formato.plano(f.getSala(), reservas.butacasOcupadas(f), List.of()));
        } catch (ReglaNegocioException e) {
            lector.reportar(e);
        }
    }
}
