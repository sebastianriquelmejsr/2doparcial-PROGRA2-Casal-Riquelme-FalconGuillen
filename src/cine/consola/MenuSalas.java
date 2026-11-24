package cine.consola;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.modelo.Butaca;
import cine.modelo.Sala;
import cine.servicios.SalaService;
import java.util.List;
import java.util.Set;

public class MenuSalas {

    /** Límite técnico de la consola para evitar agotar la memoria con valores absurdos (no es del modelo). */
    private static final long CAPACIDAD_MAXIMA_CONSOLA = 2000;

    private final LectorConsola lector;
    private final SalaService servicio;

    public MenuSalas(LectorConsola lector, SalaService servicio) {
        this.lector = lector;
        this.servicio = servicio;
    }

    public void ejecutar() {
        while (true) {
            lector.mostrar("\n--- SALAS ---");
            lector.mostrar("1. Listar  2. Registrar  3. Ver distribución  0. Volver");
            switch (lector.leerEntero("Opción", null)) {
                case 1 -> listar();
                case 2 -> registrar();
                case 3 -> verDistribucion();
                case 0 -> { return; }
                default -> lector.mostrarError("Opción inválida");
            }
        }
    }

    private void listar() {
        if (servicio.listar().isEmpty()) {
            lector.mostrar("No hay salas registradas.");
            return;
        }
        for (Sala s : servicio.listar()) {
            lector.mostrar(String.format("[%d] %s | %d filas | %d butacas/fila | capacidad %d", s.getId(),
                s.getNombre(), s.getFilas(), s.getButacasPorFila(), s.getCapacidad()));
        }
    }

    private void registrar() {
        String nombre = "";
        Integer filas = null;
        Integer porFila = null;
        while (true) {
            nombre = lector.leerTexto("Nombre", nombre);
            filas = lector.leerEntero("Cantidad de filas", filas);
            porFila = lector.leerEntero("Butacas por fila", porFila);
            if ((long) filas * porFila > CAPACIDAD_MAXIMA_CONSOLA) {
                lector.mostrarError("La consola admite hasta " + CAPACIDAD_MAXIMA_CONSOLA + " butacas por sala");
                if (!lector.confirmar("¿Corregir los datos?")) {
                    return;
                }
                continue;
            }
            try {
                Sala s = servicio.registrar(nombre, filas, porFila);
                lector.mostrar("Sala registrada: [" + s.getId() + "] " + s.getNombre()
                    + " | capacidad " + s.getCapacidad() + " | filas "
                    + s.getButacas().get(0).getFila() + " a "
                    + s.getButacas().get(s.getCapacidad() - 1).getFila());
                return;
            } catch (ReglaNegocioException | PersistenciaException e) {
                lector.reportar(e);
                if (!lector.confirmar("¿Corregir los datos?")) {
                    return;
                }
            }
        }
    }

    private void verDistribucion() {
        try {
            Sala sala = servicio.obtener(lector.leerEntero("Id de la sala", null));
            lector.mostrar(sala.getNombre() + " | capacidad " + sala.getCapacidad());
            List<String> plano = Formato.plano(sala, Set.<Butaca>of(), List.of());
            lector.mostrarLineas(plano);
            lector.mostrar("(La ocupación se consulta por función.)");
        } catch (ReglaNegocioException e) {
            lector.reportar(e);
        }
    }
}
