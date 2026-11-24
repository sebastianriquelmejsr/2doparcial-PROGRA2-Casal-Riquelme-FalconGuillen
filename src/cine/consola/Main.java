package cine.consola;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.persistencia.AlmacenCine;
import cine.persistencia.EstadoCine;
import cine.servicios.DatosDemo;
import cine.servicios.FuncionService;
import cine.servicios.PeliculaService;
import cine.servicios.ReservaService;
import cine.servicios.SalaService;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Clock;

public final class Main {

    private Main() { }

    public static void main(String[] args) {
        PrintStream salida = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        LectorConsola lector = new LectorConsola(System.in, salida);
        try {
            AlmacenCine almacen = new AlmacenCine(Paths.get("datos", "cine.ser"));
            boolean primerInicio = almacen.esPrimerInicio();

            EstadoCine estado;
            try {
                estado = almacen.cargar();
            } catch (PersistenciaException e) {
                lector.mostrar("ERROR AL INICIAR: " + e.getMessage());
                lector.mostrar("El programa se cierra SIN modificar el archivo de datos.");
                return;
            }

            Clock reloj = Clock.systemDefaultZone();
            PeliculaService peliculas = new PeliculaService(estado, almacen);
            SalaService salas = new SalaService(estado, almacen);
            FuncionService funciones = new FuncionService(estado, almacen, reloj);
            ReservaService reservas = new ReservaService(estado, almacen, reloj);

            if (primerInicio) {
                lector.mostrar("Primer inicio: no hay datos guardados (se crearán en " + almacen.getArchivo().toAbsolutePath() + ").");
                if (lector.confirmar("¿Cargar los datos de demostración (3 películas, 2 salas, 6 funciones, 5 reservas)?")) {
                    try {
                        DatosDemo.cargar(peliculas, salas, funciones, reservas, reloj);
                        lector.mostrar("Datos de demostración cargados y guardados.");
                    } catch (ReglaNegocioException | PersistenciaException e) {
                        lector.mostrar("ERROR al cargar los datos de demostración: " + e.getMessage());
                        lector.mostrar("El programa se cierra. Revisá los permisos de la carpeta 'datos' y volvé a abrirlo.");
                        return;
                    }
                }
            }

            FlujoReserva flujo = new FlujoReserva(lector, reservas);
            new MenuPrincipal(lector,
                new MenuCartelera(lector, funciones, flujo),
                new MenuReservas(lector, reservas),
                new MenuPeliculas(lector, peliculas),
                new MenuSalas(lector, salas),
                new MenuFunciones(lector, funciones, peliculas, salas, reservas)).ejecutar();
        } catch (LectorConsola.EntradaCerradaException e) {
            salida.println("\nLa entrada se cerró. Fin del programa.");
        }
    }
}
