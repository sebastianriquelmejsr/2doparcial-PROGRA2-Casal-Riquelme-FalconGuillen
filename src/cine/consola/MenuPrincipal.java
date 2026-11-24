package cine.consola;

public class MenuPrincipal {

    private final LectorConsola lector;
    private final MenuCartelera cartelera;
    private final MenuReservas reservas;
    private final MenuPeliculas peliculas;
    private final MenuSalas salas;
    private final MenuFunciones funciones;

    public MenuPrincipal(LectorConsola lector, MenuCartelera cartelera, MenuReservas reservas,
                         MenuPeliculas peliculas, MenuSalas salas, MenuFunciones funciones) {
        this.lector = lector;
        this.cartelera = cartelera;
        this.reservas = reservas;
        this.peliculas = peliculas;
        this.salas = salas;
        this.funciones = funciones;
    }

    public void ejecutar() {
        while (true) {
            lector.mostrar("\n===== CINE | Sistema de reservas =====");
            lector.mostrar("1. Cartelera y reservas nuevas");
            lector.mostrar("2. Consultar / cancelar / exportar reservas");
            lector.mostrar("3. Administrar películas");
            lector.mostrar("4. Administrar salas");
            lector.mostrar("5. Administrar funciones");
            lector.mostrar("0. Salir");
            try {
                switch (lector.leerEntero("Opción", null)) {
                    case 1 -> cartelera.ejecutar();
                    case 2 -> reservas.ejecutar();
                    case 3 -> peliculas.ejecutar();
                    case 4 -> salas.ejecutar();
                    case 5 -> funciones.ejecutar();
                    case 0 -> {
                        lector.mostrar("Hasta luego.");
                        return;
                    }
                    default -> lector.mostrarError("Opción inválida");
                }
            } catch (LectorConsola.EntradaCerradaException e) {
                throw e;
            } catch (RuntimeException e) {
                lector.mostrarError("Ocurrió un error inesperado (" + e + "). El programa continúa; los datos guardados no se alteraron.");
            }
        }
    }
}
