package cine.consola;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.modelo.Entrada;
import cine.modelo.Funcion;
import cine.modelo.Reserva;
import cine.modelo.TipoEntrada;
import cine.servicios.ReservaService;
import cine.servicios.SolicitudReserva;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Recorrido de una reserva: butacas → cliente y entradas → revisión → confirmación → comprobante.
 * Todas las reglas están en ReservaService; acá solo se piden datos y se muestran resultados.
 * La SolicitudReserva se conserva entre intentos para corregir sin volver a escribir todo.
 */
public class FlujoReserva {

    private final LectorConsola lector;
    private final ReservaService reservas;

    public FlujoReserva(LectorConsola lector, ReservaService reservas) {
        this.lector = lector;
        this.reservas = reservas;
    }

    public void reservar(Funcion funcion) {
        SolicitudReserva solicitud = new SolicitudReserva(funcion);
        lector.mostrar("\nReserva para: " + Formato.funcion(funcion)
            + " | Precio general: " + Formato.dinero(funcion.getPrecioGeneral()));
        while (true) {
            List<Entrada> entradas = pedirDatosValidos(funcion, solicitud);
            if (entradas == null) {
                lector.mostrar("Reserva cancelada. No se guardó nada.");
                return;
            }
            mostrarResumen(funcion, solicitud, entradas);
            int opcion = lector.leerEntero("1 = Confirmar reserva, 2 = Corregir datos, 0 = Cancelar", null);
            if (opcion == 0) {
                lector.mostrar("Reserva cancelada. No se guardó nada.");
                return;
            }
            if (opcion == 1) {
                try {
                    Reserva reserva = reservas.confirmar(solicitud);
                    lector.mostrar("\nReserva confirmada y guardada.");
                    lector.mostrar(Formato.comprobante(reserva));
                    return;
                } catch (ReglaNegocioException | PersistenciaException e) {
                    lector.reportar(e);
                    lector.mostrar("La reserva NO fue confirmada. Tus datos se conservaron: podés corregirlos.");
                }
            } else if (opcion != 2) {
                lector.mostrarError("Opción inválida");
            }
        }
    }

    /** Pide butacas, cliente y tipos; repite con los datos anteriores como valores por defecto hasta que sean válidos. */
    private List<Entrada> pedirDatosValidos(Funcion funcion, SolicitudReserva solicitud) {
        while (true) {
            lector.mostrar();
            lector.mostrarLineas(Formato.plano(funcion.getSala(), reservas.butacasOcupadas(funcion),
                solicitud.getCodigosButacas()));
            String actuales = String.join(", ", solicitud.getCodigosButacas());
            String ingreso = lector.leerTexto("Butacas a reservar (ej.: B3, B4; vacío = cancelar)", actuales);
            if (ingreso.isBlank()) {
                return null;
            }
            solicitud.setButacas(Arrays.asList(ingreso.split("[,;\\s]+")));
            String nombre = lector.leerTexto("Nombre y apellido", solicitud.getNombre());
            String identificacion = lector.leerTexto("Identificación", solicitud.getIdentificacion());
            solicitud.setCliente(identificacion, nombre);
            for (String codigo : solicitud.getCodigosButacas()) {
                solicitud.setTipo(codigo, pedirTipo(codigo, solicitud.getTipo(codigo)));
            }
            try {
                return reservas.calcularEntradas(solicitud);
            } catch (ReglaNegocioException e) {
                lector.reportar(e);
                if (!lector.confirmar("¿Corregir los datos?")) {
                    return null;
                }
            }
        }
    }

    private TipoEntrada pedirTipo(String codigo, TipoEntrada actual) {
        int defecto = actual == TipoEntrada.ESPECIAL ? 2 : 1;
        while (true) {
            int opcion = lector.leerEntero("Tipo de entrada para " + codigo + " (1 = General, 2 = Estudiante)", defecto);
            if (opcion == 1) {
                return TipoEntrada.GENERAL;
            }
            if (opcion == 2) {
                return TipoEntrada.ESPECIAL;
            }
            lector.mostrarError("Elegí 1 (General) o 2 (Estudiante)");
        }
    }

    private void mostrarResumen(Funcion funcion, SolicitudReserva solicitud, List<Entrada> entradas) {
        lector.mostrar("\n--- REVISAR ANTES DE CONFIRMAR ---");
        lector.mostrar(Formato.funcion(funcion));
        lector.mostrar("Cliente: " + solicitud.getNombre() + " · Identificación: " + solicitud.getIdentificacion());
        BigDecimal total = BigDecimal.ZERO;
        for (Entrada e : entradas) {
            lector.mostrar(String.format("  %-5s %-11s %s", e.getButaca().getCodigo(),
                e.getTipo().getEtiqueta(), Formato.dinero(e.getPrecio())));
            total = total.add(e.getPrecio());
        }
        lector.mostrar("Total: " + Formato.dinero(total));
        lector.mostrar("(La reserva se concreta al confirmar; la disponibilidad se verifica otra vez.)");
    }
}
