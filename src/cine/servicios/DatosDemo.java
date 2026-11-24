package cine.servicios;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.modelo.Funcion;
import cine.modelo.Pelicula;
import cine.modelo.Reserva;
import cine.modelo.Sala;
import cine.modelo.TipoEntrada;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Carga de datos de demostración: 2 salas, 3 películas, 6 funciones y 5 reservas (una cancelada).
 * Usa los servicios, así que los datos cumplen las mismas reglas que cualquier alta.
 * Las fechas son relativas a hoy (mañana y pasado mañana) para que siempre sean futuras.
 */
public final class DatosDemo {

    private DatosDemo() { }

    public static void cargar(PeliculaService peliculas, SalaService salas, FuncionService funciones,
                              ReservaService reservas, Clock reloj)
            throws ReglaNegocioException, PersistenciaException {
        Pelicula orbita = peliculas.registrar("Órbita", 120);
        Pelicula ultimoViaje = peliculas.registrar("El ultimo viaje", 105);
        Pelicula bosque = peliculas.registrar("Bosque de papel", 90);

        Sala sala1 = salas.registrar("Sala 1", 4, 6);
        Sala sala2 = salas.registrar("Sala 2", 3, 6);

        LocalDate manana = LocalDate.now(reloj).plusDays(1);
        LocalDate pasado = LocalDate.now(reloj).plusDays(2);

        Funcion f1 = funciones.programar(orbita.getId(), sala1.getId(), manana.atTime(18, 0), new BigDecimal("5000"));
        Funcion f2 = funciones.programar(orbita.getId(), sala1.getId(), manana.atTime(21, 0), new BigDecimal("5000"));
        Funcion f3 = funciones.programar(ultimoViaje.getId(), sala2.getId(), manana.atTime(19, 0), new BigDecimal("4500"));
        funciones.programar(bosque.getId(), sala2.getId(), pasado.atTime(17, 0), new BigDecimal("4000"));
        Funcion f5 = funciones.programar(orbita.getId(), sala1.getId(), pasado.atTime(18, 0), new BigDecimal("5000"));
        funciones.programar(ultimoViaje.getId(), sala2.getId(), pasado.atTime(20, 0), new BigDecimal("4500"));

        reservar(reservas, f1, "30111222", "Lucía Pérez",
            new String[] {"A1", "A2"}, new TipoEntrada[] {TipoEntrada.GENERAL, TipoEntrada.GENERAL});
        reservar(reservas, f1, "28456789", "Martín Gómez",
            new String[] {"B3", "B4"}, new TipoEntrada[] {TipoEntrada.GENERAL, TipoEntrada.ESPECIAL});
        Reserva aCancelar = reservar(reservas, f2, "31222333", "Ana López",
            new String[] {"C1"}, new TipoEntrada[] {TipoEntrada.GENERAL});
        reservar(reservas, f3, "30111222", "Lucía Pérez",
            new String[] {"A1", "A2"}, new TipoEntrada[] {TipoEntrada.GENERAL, TipoEntrada.ESPECIAL});
        reservar(reservas, f5, "28456789", "Martín Gómez",
            new String[] {"B3"}, new TipoEntrada[] {TipoEntrada.GENERAL});

        reservas.cancelar(aCancelar.getCodigo());
    }

    private static Reserva reservar(ReservaService reservas, Funcion funcion, String identificacion,
                                    String nombre, String[] butacas, TipoEntrada[] tipos)
            throws ReglaNegocioException, PersistenciaException {
        SolicitudReserva solicitud = new SolicitudReserva(funcion);
        solicitud.setCliente(identificacion, nombre);
        solicitud.setButacas(Arrays.asList(butacas));
        for (int i = 0; i < butacas.length; i++) {
            solicitud.setTipo(butacas[i], tipos[i]);
        }
        return reservas.confirmar(solicitud);
    }
}
