package cine.consola;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.excepciones.ValidacionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Entrada y salida de la consola. Todas las lecturas reintentan ante datos mal escritos
 * y aceptan un valor por defecto (Enter lo conserva), para corregir sin perder lo ingresado.
 */
public class LectorConsola {

    /** Se lanza cuando la entrada se cierra (fin de archivo): permite terminar sin errores. */
    public static class EntradaCerradaException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EntradaCerradaException() {
            super("La entrada se cerró");
        }
    }

    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter HORA =
        DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private final BufferedReader entrada;
    private final PrintStream salida;

    public LectorConsola(InputStream entrada, PrintStream salida) {
        this.entrada = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        this.salida = salida;
    }

    public void mostrar(String texto) {
        salida.println(texto);
    }

    public void mostrar() {
        salida.println();
    }

    public void mostrarLineas(Iterable<String> lineas) {
        for (String linea : lineas) {
            salida.println(linea);
        }
    }

    /** Informa un error indicando de qué tipo es: validación, regla de negocio o almacenamiento. */
    public void reportar(Exception e) {
        if (e instanceof ValidacionException) {
            salida.println("DATO INVÁLIDO: " + e.getMessage());
        } else if (e instanceof ReglaNegocioException) {
            salida.println("NO PERMITIDO: " + e.getMessage());
        } else if (e instanceof PersistenciaException) {
            salida.println("ERROR DE ARCHIVO: " + e.getMessage());
            salida.println("El cambio NO se guardó y se revirtió en memoria.");
        } else {
            salida.println("ERROR: " + e.getMessage());
        }
    }

    public void mostrarError(String mensaje) {
        salida.println("ERROR: " + mensaje);
    }

    private String leerLinea(String pregunta) {
        salida.print(pregunta + " ");
        salida.flush();
        try {
            String linea = entrada.readLine();
            if (linea == null) {
                throw new EntradaCerradaException();
            }
            return linea.trim();
        } catch (IOException e) {
            throw new EntradaCerradaException();
        }
    }

    private static String conDefecto(String etiqueta, Object defecto) {
        return defecto == null || defecto.toString().isEmpty()
            ? etiqueta + ":" : etiqueta + " [" + defecto + "]:";
    }

    /** Texto libre. Si se deja vacío devuelve el valor por defecto (que puede ser ""). */
    public String leerTexto(String etiqueta, String defecto) {
        String linea = leerLinea(conDefecto(etiqueta, defecto));
        return linea.isEmpty() && defecto != null ? defecto : linea;
    }

    public int leerEntero(String etiqueta, Integer defecto) {
        while (true) {
            String linea = leerLinea(conDefecto(etiqueta, defecto));
            if (linea.isEmpty() && defecto != null) {
                return defecto;
            }
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                mostrarError("Ingresá un número entero (por ejemplo 120)");
            }
        }
    }

    public LocalDate leerFecha(String etiqueta, LocalDate defecto) {
        String texto = defecto == null ? null : FECHA.format(defecto);
        while (true) {
            String linea = leerLinea(conDefecto(etiqueta + " (dd/mm/aaaa)", texto));
            if (linea.isEmpty() && defecto != null) {
                return defecto;
            }
            try {
                return LocalDate.parse(linea, FECHA);
            } catch (DateTimeParseException e) {
                mostrarError("Fecha inválida. Usá el formato dd/mm/aaaa (por ejemplo 04/12/2026)");
            }
        }
    }

    /** Fecha opcional: vacío devuelve null. */
    public LocalDate leerFechaOpcional(String etiqueta) {
        while (true) {
            String linea = leerLinea(etiqueta + " (dd/mm/aaaa, vacío = todas):");
            if (linea.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(linea, FECHA);
            } catch (DateTimeParseException e) {
                mostrarError("Fecha inválida. Usá el formato dd/mm/aaaa (por ejemplo 04/12/2026)");
            }
        }
    }

    public LocalTime leerHora(String etiqueta, LocalTime defecto) {
        String texto = defecto == null ? null : HORA.format(defecto);
        while (true) {
            String linea = leerLinea(conDefecto(etiqueta + " (hh:mm)", texto));
            if (linea.isEmpty() && defecto != null) {
                return defecto;
            }
            try {
                return LocalTime.parse(linea, HORA);
            } catch (DateTimeParseException e) {
                mostrarError("Hora inválida. Usá el formato hh:mm (por ejemplo 18:30)");
            }
        }
    }

    /**
     * Precio: sin separador de miles; coma o punto para decimales (5000 o 4500,50).
     * "5.000" se rechaza por ambiguo (podría ser 5 o 5000).
     */
    public BigDecimal leerPrecio(String etiqueta, BigDecimal defecto) {
        String texto = defecto == null ? null : defecto.toPlainString();
        while (true) {
            String linea = leerLinea(conDefecto(etiqueta, texto));
            if (linea.isEmpty() && defecto != null) {
                return defecto;
            }
            String limpio = linea.replace("$", "").replace(" ", "");
            if (limpio.matches("-?\\d{1,3}(\\.\\d{3})+")) {
                mostrarError("Escribí el precio sin separador de miles (por ejemplo 5000)");
            } else if (limpio.matches("-?\\d+([.,]\\d{1,2})?")) {
                return new BigDecimal(limpio.replace(',', '.'));
            } else {
                mostrarError("Precio inválido. Ejemplos: 5000 o 4500,50");
            }
        }
    }

    /** Pregunta Sí/No y repite hasta recibir una respuesta válida. */
    public boolean confirmar(String pregunta) {
        while (true) {
            String linea = leerLinea(pregunta + " (S/N):").toLowerCase();
            if (linea.equals("s") || linea.equals("si") || linea.equals("sí")) {
                return true;
            }
            if (linea.equals("n") || linea.equals("no")) {
                return false;
            }
            mostrarError("Respondé S o N");
        }
    }
}
