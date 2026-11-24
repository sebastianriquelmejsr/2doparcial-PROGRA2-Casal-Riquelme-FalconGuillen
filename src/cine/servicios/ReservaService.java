package cine.servicios;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.excepciones.ValidacionException;
import cine.modelo.Butaca;
import cine.modelo.Cliente;
import cine.modelo.Entrada;
import cine.modelo.Funcion;
import cine.modelo.Reserva;
import cine.modelo.Sala;
import cine.modelo.TipoEntrada;
import cine.persistencia.AlmacenCine;
import cine.persistencia.EstadoCine;
import cine.persistencia.ExportadorReservasCsv;
import cine.persistencia.Secuencia;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ReservaService {

    private final EstadoCine estado;
    private final AlmacenCine almacen;
    private final Clock reloj;

    public ReservaService(EstadoCine estado, AlmacenCine almacen, Clock reloj) {
        this.estado = estado;
        this.almacen = almacen;
        this.reloj = reloj;
    }

    /** Resultado de validar una solicitud: todo lo necesario para crear la reserva. */
    private record Validada(Funcion funcion, Cliente cliente, boolean clienteNuevo, List<Entrada> entradas) { }

    /**
     * Butacas ocupadas en una función: las de reservas CONFIRMADAS de esa función.
     * La disponibilidad se deduce de las reservas, no se guarda en la butaca.
     */
    public Set<Butaca> butacasOcupadas(Funcion funcion) {
        Set<Butaca> ocupadas = new HashSet<>();
        for (Reserva r : estado.getReservas().listar()) {
            if (r.isConfirmada() && r.getFuncion().equals(funcion)) {
                for (Entrada e : r.getEntradas()) {
                    ocupadas.add(e.getButaca());
                }
            }
        }
        return ocupadas;
    }

    /** Valida la solicitud completa y calcula las entradas con sus precios, sin reservar nada. */
    public List<Entrada> calcularEntradas(SolicitudReserva solicitud) throws ReglaNegocioException {
        return validar(solicitud).entradas();
    }

    /**
     * Confirma la reserva: vuelve a validar (disponibilidad incluida), crea la reserva con código
     * único, la guarda y, si el guardado falla, revierte todo en memoria.
     */
    public Reserva confirmar(SolicitudReserva solicitud) throws ReglaNegocioException, PersistenciaException {
        Validada v = validar(solicitud);
        Secuencia secuencia = estado.getSecuenciaReservas();
        int numero = secuencia.getProximo();
        String codigo = String.format(Locale.ROOT, "CIN-%03d", numero);
        Reserva reserva = new Reserva(codigo, v.cliente(), v.funcion(), v.entradas(), LocalDateTime.now(reloj));

        secuencia.siguiente();
        estado.getReservas().agregar(reserva);
        if (v.clienteNuevo()) {
            estado.getClientes().agregar(v.cliente());
        }
        try {
            almacen.guardar(estado);
        } catch (PersistenciaException e) {
            estado.getReservas().quitar(codigo);
            if (v.clienteNuevo()) {
                estado.getClientes().quitar(v.cliente().getId());
            }
            secuencia.restaurar(numero);
            throw e;
        }
        return reserva;
    }

    /** Cancela la reserva (libera las butacas y conserva el historial). No se puede cancelar dos veces. */
    public Reserva cancelar(String codigo) throws ReglaNegocioException, PersistenciaException {
        Reserva reserva = obtener(codigo);
        reserva.cancelar(LocalDateTime.now(reloj));
        try {
            almacen.guardar(estado);
        } catch (PersistenciaException e) {
            reserva.deshacerCancelacion();
            throw e;
        }
        return reserva;
    }

    public Reserva obtener(String codigo) throws ReglaNegocioException {
        String clave = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        return estado.getReservas().buscar(clave)
            .orElseThrow(() -> new ReglaNegocioException("No existe la reserva '" + clave + "'"));
    }

    /** Busca por código de reserva o por identificación del cliente; un criterio vacío devuelve todas. */
    public List<Reserva> buscar(String criterio) {
        String c = criterio == null ? "" : criterio.trim().toLowerCase();
        return estado.getReservas().listar().stream()
        .filter(r -> c.isEmpty()
            || r.getCodigo().toLowerCase().contains(c)
            || r.getCliente().getIdentificacion().toLowerCase().contains(c))
        .toList();
    }

    public List<Reserva> listar() {
        return estado.getReservas().listar();
    }

    /** Exporta todas las reservas (también las canceladas). No modifica ningún dato. */
    public void exportarCsv(Path destino) throws PersistenciaException {
        new ExportadorReservasCsv().exportar(listar(), destino);
    }

    private Validada validar(SolicitudReserva solicitud) throws ReglaNegocioException {
        if (solicitud == null || solicitud.getFuncion() == null) {
            throw new ValidacionException("Debe seleccionar una función");
        }
        Funcion funcion = estado.getFunciones().buscar(solicitud.getFuncion().getId())
            .orElseThrow(() -> new ReglaNegocioException("La función seleccionada no existe"));
        if (!funcion.getPelicula().isActiva()) {
            throw new ReglaNegocioException("La película '" + funcion.getPelicula().getTitulo()
                + "' está desactivada: no se pueden hacer reservas para sus funciones");
        }
        if (funcion.haComenzado(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("La función ya comenzó: solo se pueden reservar funciones futuras");
        }
        Map<String, TipoEntrada> seleccion = solicitud.getTipos();
        if (seleccion.isEmpty()) {
            throw new ValidacionException("Debe seleccionar al menos una butaca");
        }
        Cliente candidato = new Cliente(solicitud.getIdentificacion(), solicitud.getNombre());

        Sala sala = funcion.getSala();
        Set<Butaca> ocupadas = butacasOcupadas(funcion);
        List<String> inexistentes = new ArrayList<>();
        List<String> yaReservadas = new ArrayList<>();
        List<Entrada> entradas = new ArrayList<>();
        for (Map.Entry<String, TipoEntrada> elegida : seleccion.entrySet()) {
            Optional<Butaca> butaca = sala.buscarButaca(elegida.getKey());
            if (butaca.isEmpty()) {
                inexistentes.add(elegida.getKey());
            } else if (ocupadas.contains(butaca.get())) {
                yaReservadas.add(butaca.get().getCodigo());
            } else {
                entradas.add(elegida.getValue().crearEntrada(butaca.get(), funcion.getPrecioGeneral()));
            }
        }
        if (!inexistentes.isEmpty()) {
            throw new ReglaNegocioException((inexistentes.size() == 1 ? "La butaca " : "Las butacas ")
                + String.join(", ", inexistentes) + (inexistentes.size() == 1 ? " no existe" : " no existen")
                + " en " + sala.getNombre());
        }
        if (!yaReservadas.isEmpty()) {
            throw new ReglaNegocioException((yaReservadas.size() == 1 ? "La butaca " : "Las butacas ")
                + String.join(", ", yaReservadas)
                + (yaReservadas.size() == 1 ? " ya está reservada" : " ya están reservadas")
                + " para esta función");
        }

        Optional<Cliente> existente = estado.getClientes().buscar(candidato.getIdentificacion());
        Cliente cliente = existente.orElse(candidato);
        return new Validada(funcion, cliente, existente.isEmpty(), entradas);
    }
}
