package cine.persistencia;

import cine.modelo.Cliente;
import cine.modelo.Funcion;
import cine.modelo.Pelicula;
import cine.modelo.Reserva;
import cine.modelo.Sala;
import java.io.Serializable;

/**
 * Objeto contenedor serializable con el estado completo del cine.
 * Al serializarlo se guardan también las relaciones entre los objetos.
 */
public class EstadoCine implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Repositorio<Pelicula, Integer> peliculas = new Repositorio<>();
    private final Repositorio<Sala, Integer> salas = new Repositorio<>();
    private final Repositorio<Funcion, Integer> funciones = new Repositorio<>();
    private final Repositorio<Cliente, String> clientes = new Repositorio<>();
    private final Repositorio<Reserva, String> reservas = new Repositorio<>();

    private final Secuencia secuenciaPeliculas = new Secuencia();
    private final Secuencia secuenciaSalas = new Secuencia();
    private final Secuencia secuenciaFunciones = new Secuencia();
    private final Secuencia secuenciaReservas = new Secuencia();

    public Repositorio<Pelicula, Integer> getPeliculas() { return peliculas; }

    public Repositorio<Sala, Integer> getSalas() { return salas; }

    public Repositorio<Funcion, Integer> getFunciones() { return funciones; }

    public Repositorio<Cliente, String> getClientes() { return clientes; }

    public Repositorio<Reserva, String> getReservas() { return reservas; }

    public Secuencia getSecuenciaPeliculas() { return secuenciaPeliculas; }

    public Secuencia getSecuenciaSalas() { return secuenciaSalas; }

    public Secuencia getSecuenciaFunciones() { return secuenciaFunciones; }

    public Secuencia getSecuenciaReservas() { return secuenciaReservas; }
}
