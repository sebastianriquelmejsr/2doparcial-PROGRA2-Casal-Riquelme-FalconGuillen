package cine.servicios;

import cine.modelo.Funcion;
import cine.modelo.TipoEntrada;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Selección en curso: función, butacas elegidas con su tipo de entrada y datos del cliente.
 * Todavía NO es una reserva: solo ReservaService.confirmar() la convierte en una.
 * Permite corregir datos sin perder lo ya ingresado.
 */
public class SolicitudReserva {

    private final Funcion funcion;
    private final Map<String, TipoEntrada> tipos = new LinkedHashMap<>();
    private String identificacion = "";
    private String nombre = "";

    public SolicitudReserva(Funcion funcion) {
        this.funcion = funcion;
    }

    public Funcion getFuncion() { return funcion; }

    /** Reemplaza las butacas elegidas; conserva el tipo de las que ya estaban y usa GENERAL en las nuevas. */
    public void setButacas(Collection<String> codigos) {
        Map<String, TipoEntrada> nuevo = new LinkedHashMap<>();
        for (String codigo : codigos) {
            String clave = normalizar(codigo);
            if (!clave.isEmpty()) {
                nuevo.put(clave, tipos.getOrDefault(clave, TipoEntrada.GENERAL));
            }
        }
        tipos.clear();
        tipos.putAll(nuevo);
    }

    public void setTipo(String codigo, TipoEntrada tipo) {
        String clave = normalizar(codigo);
        if (tipos.containsKey(clave)) {
            tipos.put(clave, tipo);
        }
    }

    public TipoEntrada getTipo(String codigo) {
        return tipos.getOrDefault(normalizar(codigo), TipoEntrada.GENERAL);
    }

    public List<String> getCodigosButacas() { return List.copyOf(tipos.keySet()); }

    /** Butaca → tipo de entrada, en el orden elegido. */
    public Map<String, TipoEntrada> getTipos() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(tipos));
    }

    public void setCliente(String identificacion, String nombre) {
        this.identificacion = identificacion == null ? "" : identificacion;
        this.nombre = nombre == null ? "" : nombre;
    }

    public String getIdentificacion() { return identificacion; }

    public String getNombre() { return nombre; }

    private static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }
}
