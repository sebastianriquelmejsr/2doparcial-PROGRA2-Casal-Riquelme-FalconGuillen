package cine.consola;

import cine.excepciones.ReglaNegocioException;
import cine.modelo.Funcion;
import cine.servicios.FuncionService;
import java.time.LocalDate;
import java.util.List;

public class MenuCartelera {

    private final LectorConsola lector;
    private final FuncionService funciones;
    private final FlujoReserva flujoReserva;

    public MenuCartelera(LectorConsola lector, FuncionService funciones, FlujoReserva flujoReserva) {
        this.lector = lector;
        this.funciones = funciones;
        this.flujoReserva = flujoReserva;
    }

    public void ejecutar() {
        String texto = "";
        LocalDate fecha = null;
        while (true) {
            lector.mostrar("\n--- CARTELERA ---");
            mostrar(texto, fecha);
            lector.mostrar("1. Reservar una función  2. Buscar (película / fecha)  3. Limpiar filtros  0. Volver");
            switch (lector.leerEntero("Opción", null)) {
                case 1 -> reservar();
                case 2 -> {
                    texto = lector.leerTexto("Película (texto contenido en el título, vacío = todas)", "");
                    fecha = lector.leerFechaOpcional("Fecha");
                }
                case 3 -> {
                    texto = "";
                    fecha = null;
                }
                case 0 -> { return; }
                default -> lector.mostrarError("Opción inválida");
            }
        }
    }

    private void mostrar(String texto, LocalDate fecha) {
        List<Funcion> lista = funciones.cartelera(texto, fecha);
        if (!texto.isEmpty() || fecha != null) {
            lector.mostrar("Filtros: película='" + texto + "' fecha="
                + (fecha == null ? "todas" : Formato.FECHA.format(fecha)));
        }
        if (lista.isEmpty()) {
            lector.mostrar("No hay funciones para estos filtros");
            return;
        }
        for (Funcion f : lista) {
            lector.mostrar(Formato.lineaFuncion(f));
        }
    }

    private void reservar() {
        try {
            Funcion funcion = funciones.obtener(lector.leerEntero("Id de la función a reservar", null));
            flujoReserva.reservar(funcion);
        } catch (ReglaNegocioException e) {
            lector.reportar(e);
        }
    }
}
