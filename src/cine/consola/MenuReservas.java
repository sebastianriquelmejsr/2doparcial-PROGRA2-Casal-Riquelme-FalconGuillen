package cine.consola;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.modelo.Reserva;
import cine.servicios.ReservaService;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MenuReservas {

    private static final String RUTA_CSV_POR_DEFECTO = Paths.get("datos", "reservas.csv").toString();

    private final LectorConsola lector;
    private final ReservaService servicio;

    public MenuReservas(LectorConsola lector, ReservaService servicio) {
        this.lector = lector;
        this.servicio = servicio;
    }

    public void ejecutar() {
        while (true) {
            lector.mostrar("\n--- RESERVAS ---");
            lector.mostrar("1. Buscar (código o identificación)  2. Detalle / cancelar  3. Exportar CSV  0. Volver");
            switch (lector.leerEntero("Opción", null)) {
                case 1 -> buscar();
                case 2 -> detalle();
                case 3 -> exportar();
                case 0 -> { return; }
                default -> lector.mostrarError("Opción inválida");
            }
        }
    }

    private void buscar() {
        String criterio = lector.leerTexto("Código de reserva o identificación del cliente (vacío = todas)", "");
        List<Reserva> encontradas = servicio.buscar(criterio);
        if (encontradas.isEmpty()) {
            lector.mostrar("Sin resultados para '" + criterio + "'. Probá con otro criterio o dejalo vacío para ver todas.");
            return;
        }
        for (Reserva r : encontradas) {
            lector.mostrar(Formato.lineaReserva(r));
        }
    }

    private void detalle() {
        try {
            Reserva reserva = servicio.obtener(lector.leerTexto("Código de la reserva (ej.: CIN-002)", ""));
            lector.mostrar(Formato.comprobante(reserva));
            if (!reserva.isConfirmada()) {
                lector.mostrar("La reserva ya está cancelada: no se puede cancelar de nuevo.");
                return;
            }
            if (!lector.confirmar("¿Cancelar la reserva " + reserva.getCodigo() + "? Se liberarán sus butacas")) {
                lector.mostrar("La reserva se conservó sin cambios.");
                return;
            }
            servicio.cancelar(reserva.getCodigo());
            lector.mostrar("Reserva " + reserva.getCodigo() + " cancelada. Butacas liberadas; el historial se conserva.");
        } catch (ReglaNegocioException | PersistenciaException e) {
            lector.reportar(e);
        }
    }

    private void exportar() {
        String ruta = RUTA_CSV_POR_DEFECTO;
        while (true) {
            ruta = lector.leerTexto("Archivo de destino", ruta);
            try {
                Path destino = Paths.get(ruta);
                servicio.exportarCsv(destino);
                lector.mostrar("Reservas exportadas a " + destino.toAbsolutePath()
                    + " (solo consulta: no se modificó ninguna reserva).");
                return;
            } catch (PersistenciaException | java.nio.file.InvalidPathException e) {
                lector.mostrarError("No se pudo exportar: " + e.getMessage());
                if (!lector.confirmar("¿Probar con otra ruta?")) {
                    return;
                }
            }
        }
    }
}
