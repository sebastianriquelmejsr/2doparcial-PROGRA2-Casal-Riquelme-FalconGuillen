package cine.consola;

import cine.modelo.Butaca;
import cine.modelo.Entrada;
import cine.modelo.Funcion;
import cine.modelo.Reserva;
import cine.modelo.Sala;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Presentación de datos en texto para la consola. No contiene reglas de negocio. */
public final class Formato {

    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() { }

    /** "$ 5.000" o "$ 4.500,50". */
    public static String dinero(BigDecimal valor) {
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(Locale.of("es", "AR"));
        boolean entero = valor.stripTrailingZeros().scale() <= 0;
        DecimalFormat formato = new DecimalFormat(entero ? "#,##0" : "#,##0.00", simbolos);
        return "$ " + formato.format(valor);
    }

    /** "Órbita · Sala 1 · 04/12/2026 · 18:00". */
    public static String funcion(Funcion f) {
        return f.getPelicula().getTitulo() + " · " + f.getSala().getNombre() + " · "
            + FECHA.format(f.getInicio()) + " · " + HORA.format(f.getInicio());
    }

    public static String lineaFuncion(Funcion f) {
        return String.format("[%d] %s | %s | %s %s-%s | %s", f.getId(), f.getPelicula().getTitulo(),
            f.getSala().getNombre(), FECHA.format(f.getInicio()), HORA.format(f.getInicio()),
            HORA.format(f.getFin()), dinero(f.getPrecioGeneral()));
    }

    public static String lineaReserva(Reserva r) {
        return String.format("%s | %s (%s) | %s %s | %s | %s", r.getCodigo(), r.getCliente().getNombre(),
            r.getCliente().getIdentificacion(), r.getFuncion().getPelicula().getTitulo(),
            HORA.format(r.getFuncion().getInicio()), r.getEstado().getEtiqueta(), dinero(r.getTotal()));
    }

    /**
     * Plano de la sala por filas. Libre: A1 | seleccionada: A1* | ocupada: A1x.
     * Con un conjunto de ocupadas vacío es la distribución estructural de la sala.
     */
    public static List<String> plano(Sala sala, Set<Butaca> ocupadas, Collection<String> seleccionadas) {
        Set<String> elegidas = new HashSet<>();
        for (String codigo : seleccionadas) {
            elegidas.add(codigo.toUpperCase(Locale.ROOT));
        }
        List<Butaca> todas = sala.getButacas();
        int ancho = 0;
        for (Butaca b : todas) {
            ancho = Math.max(ancho, b.getCodigo().length());
        }
        ancho += 2;
        int porFila = sala.getButacasPorFila();
        List<String> lineas = new ArrayList<>();
        lineas.add("=== PANTALLA ===");
        for (int i = 0; i < todas.size(); i += porFila) {
            StringBuilder linea = new StringBuilder();
            for (Butaca b : todas.subList(i, i + porFila)) {
                String marca = ocupadas.contains(b) ? "x" : elegidas.contains(b.getCodigo()) ? "*" : "";
                linea.append(String.format("%-" + ancho + "s", b.getCodigo() + marca));
            }
            lineas.add(linea.toString().stripTrailing());
        }
        lineas.add("Leyenda: A1 = libre | A1* = seleccionada | A1x = ocupada");
        return lineas;
    }

    /** Comprobante y detalle de una reserva (también muestra si está cancelada). */
    public static String comprobante(Reserva r) {
        StringBuilder sb = new StringBuilder();
        sb.append("======================= COMPROBANTE =======================\n");
        sb.append("Reserva:   ").append(r.getCodigo()).append(" (").append(r.getEstado().getEtiqueta()).append(")\n");
        sb.append("Cliente:   ").append(r.getCliente().getNombre())
          .append(" · Identificación: ").append(r.getCliente().getIdentificacion()).append('\n');
        sb.append("Película:  ").append(r.getFuncion().getPelicula().getTitulo()).append('\n');
        sb.append("Función:   ").append(r.getFuncion().getSala().getNombre()).append(" · ")
          .append(FECHA.format(r.getFuncion().getInicio())).append(" · ")
          .append(HORA.format(r.getFuncion().getInicio())).append('\n');
        sb.append("Entradas:\n");
        for (Entrada e : r.getEntradas()) {
            sb.append(String.format("  %-5s %-11s %s%n", e.getButaca().getCodigo(),
                e.getTipo().getEtiqueta(), dinero(e.getPrecio())));
        }
        sb.append("Importe total: ").append(dinero(r.getTotal())).append('\n');
        sb.append("Creada: ").append(FECHA_HORA.format(r.getFechaCreacion())).append('\n');
        if (r.getFechaCancelacion() != null) {
            sb.append("Cancelada: ").append(FECHA_HORA.format(r.getFechaCancelacion())).append('\n');
        }
        sb.append("===========================================================");
        return sb.toString();
    }
}
