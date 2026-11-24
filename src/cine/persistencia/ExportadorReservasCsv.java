package cine.persistencia;

import cine.excepciones.PersistenciaException;
import cine.modelo.Entrada;
import cine.modelo.Reserva;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exporta las reservas a CSV (separador ";", UTF-8 con BOM, una fila por entrada).
 * Es solo lectura: no modifica reservas ni disponibilidad.
 */
public class ExportadorReservasCsv {

    private static final String SEPARADOR = ";";
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] ENCABEZADOS = {
        "codigo_reserva", "estado", "fecha_creacion", "fecha_cancelacion",
        "identificacion_cliente", "nombre_cliente", "pelicula", "sala", "inicio_funcion",
        "butaca", "tipo_entrada", "precio_entrada", "importe_total_reserva"
    };

    public void exportar(List<Reserva> reservas, Path destino) throws PersistenciaException {
        try {
            Path carpeta = destino.toAbsolutePath().getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            try (BufferedWriter salida = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
                salida.write('\uFEFF');
                salida.write(String.join(SEPARADOR, ENCABEZADOS));
                salida.newLine();
                for (Reserva r : reservas) {
                    for (Entrada e : r.getEntradas()) {
                        salida.write(fila(r, e));
                        salida.newLine();
                    }
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo exportar a " + destino + ": " + e.getMessage(), e);
        }
    }

    private String fila(Reserva r, Entrada e) {
        String cancelacion = r.getFechaCancelacion() == null ? "" : FECHA_HORA.format(r.getFechaCancelacion());
        return String.join(SEPARADOR,
            campo(r.getCodigo()),
            campo(r.getEstado().getEtiqueta()),
            campo(FECHA_HORA.format(r.getFechaCreacion())),
            campo(cancelacion),
            campo(r.getCliente().getIdentificacion()),
            campo(r.getCliente().getNombre()),
            campo(r.getFuncion().getPelicula().getTitulo()),
            campo(r.getFuncion().getSala().getNombre()),
            campo(FECHA_HORA.format(r.getFuncion().getInicio())),
            campo(e.getButaca().getCodigo()),
            campo(e.getTipo().getEtiqueta()),
            campo(e.getPrecio().toPlainString()),
            campo(r.getTotal().toPlainString()));
    }

    private String campo(String valor) {
        if (valor.contains(SEPARADOR) || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
