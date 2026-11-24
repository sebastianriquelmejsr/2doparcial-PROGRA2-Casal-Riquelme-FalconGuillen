package cine.consola;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.modelo.Pelicula;
import cine.servicios.PeliculaService;

public class MenuPeliculas {

    private final LectorConsola lector;
    private final PeliculaService servicio;

    public MenuPeliculas(LectorConsola lector, PeliculaService servicio) {
        this.lector = lector;
        this.servicio = servicio;
    }

    public void ejecutar() {
        while (true) {
            lector.mostrar("\n--- PELÍCULAS ---");
            lector.mostrar("1. Listar  2. Registrar  3. Modificar  4. Desactivar  0. Volver");
            switch (lector.leerEntero("Opción", null)) {
                case 1 -> listar();
                case 2 -> registrar();
                case 3 -> modificar();
                case 4 -> desactivar();
                case 0 -> { return; }
                default -> lector.mostrarError("Opción inválida");
            }
        }
    }

    private void listar() {
        if (servicio.listar().isEmpty()) {
            lector.mostrar("No hay películas registradas.");
            return;
        }
        for (Pelicula p : servicio.listar()) {
            lector.mostrar(String.format("[%d] %s | %d min | %s", p.getId(), p.getTitulo(),
                p.getDuracionMinutos(), p.isActiva() ? "Activa" : "Desactivada"));
        }
    }

    private void registrar() {
        String titulo = "";
        Integer duracion = null;
        while (true) {
            titulo = lector.leerTexto("Título", titulo);
            duracion = lector.leerEntero("Duración (minutos)", duracion);
            try {
                Pelicula p = servicio.registrar(titulo, duracion);
                lector.mostrar("Película registrada: [" + p.getId() + "] " + p);
                return;
            } catch (ReglaNegocioException | PersistenciaException e) {
                lector.reportar(e);
                if (!lector.confirmar("¿Corregir los datos?")) {
                    return;
                }
            }
        }
    }

    private void modificar() {
        Pelicula actual;
        try {
            actual = servicio.obtener(lector.leerEntero("Id de la película", null));
        } catch (ReglaNegocioException e) {
            lector.reportar(e);
            return;
        }
        String titulo = actual.getTitulo();
        Integer duracion = actual.getDuracionMinutos();
        while (true) {
            titulo = lector.leerTexto("Título", titulo);
            duracion = lector.leerEntero("Duración (minutos)", duracion);
            try {
                Pelicula p = servicio.modificar(actual.getId(), titulo, duracion);
                lector.mostrar("Película modificada: [" + p.getId() + "] " + p);
                return;
            } catch (ReglaNegocioException | PersistenciaException e) {
                lector.reportar(e);
                if (!lector.confirmar("¿Corregir los datos?")) {
                    return;
                }
            }
        }
    }

    private void desactivar() {
        try {
            Pelicula p = servicio.obtener(lector.leerEntero("Id de la película", null));
            if (!lector.confirmar("¿Desactivar '" + p.getTitulo() + "'? Sus reservas se conservan")) {
                lector.mostrar("No se desactivó.");
                return;
            }
            servicio.desactivar(p.getId());
            lector.mostrar("Película desactivada. Sus funciones dejan de aparecer en la cartelera.");
        } catch (ReglaNegocioException | PersistenciaException e) {
            lector.reportar(e);
        }
    }
}
