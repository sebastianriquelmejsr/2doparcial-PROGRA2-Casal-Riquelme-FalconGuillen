package cine.consola;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.excepciones.ValidacionException;
import cine.modelo.Entrada;
import cine.modelo.EntradaEspecial;
import cine.modelo.EntradaGeneral;
import cine.modelo.Funcion;
import cine.modelo.Pelicula;
import cine.modelo.Reserva;
import cine.modelo.Sala;
import cine.modelo.TipoEntrada;
import cine.persistencia.AlmacenCine;
import cine.persistencia.EstadoCine;
import cine.servicios.FuncionService;
import cine.servicios.PeliculaService;
import cine.servicios.ReservaService;
import cine.servicios.SalaService;
import cine.servicios.SolicitudReserva;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/**
 * Verificación automática de las reglas de negocio, la persistencia y los errores de escritura.
 * Trabaja en una carpeta temporal: no toca la carpeta 'datos' del programa.
 * Ejecutar con: java -cp build/classes cine.consola.VerificacionAutomatica
 */
public final class VerificacionAutomatica {

    private interface Accion {
        void ejecutar() throws Exception;
    }

    /** Reloj controlable para probar "funciones que ya comenzaron". */
    private static final class RelojMutable extends Clock {
        private final ZoneId zona = ZoneId.systemDefault();
        private Instant instante;

        RelojMutable(LocalDateTime inicio) {
            this.instante = inicio.atZone(zona).toInstant();
        }

        void irA(LocalDateTime momento) {
            this.instante = momento.atZone(zona).toInstant();
        }

        @Override
        public ZoneId getZone() { return zona; }

        @Override
        public Clock withZone(ZoneId z) { throw new UnsupportedOperationException(); }

        @Override
        public Instant instant() { return instante; }
    }

    private static int correctos = 0;
    private static int fallidos = 0;

    private static Path carpeta;
    private static Path archivo;
    private static RelojMutable reloj;
    private static AlmacenCine almacen;
    private static EstadoCine estado;
    private static PeliculaService peliculas;
    private static SalaService salas;
    private static FuncionService funciones;
    private static ReservaService reservas;

    private VerificacionAutomatica() { }

    public static void main(String[] args) throws Exception {
        carpeta = Files.createTempDirectory("cine-verificacion");
        archivo = carpeta.resolve("cine.ser");
        reloj = new RelojMutable(LocalDateTime.of(2026, 12, 1, 10, 0));
        System.out.println("Carpeta temporal de la verificación: " + carpeta);

        arrancar();
        titulo("Primer inicio");
        verificar("Sin archivo: es primer inicio", almacen.esPrimerInicio());
        verificar("Estado inicial vacío y válido", estado.getPeliculas().listar().isEmpty());

        Pelicula orbita = peliculas.registrar("Órbita", 120);
        Pelicula viaje = peliculas.registrar("El último viaje", 105);
        Pelicula bosque = peliculas.registrar("Bosque de papel", 90);
        Sala sala1 = salas.registrar("Sala 1", 4, 6);
        Sala sala2 = salas.registrar("Sala 2", 3, 6);
        LocalDateTime d4 = LocalDateTime.of(2026, 12, 4, 0, 0);
        LocalDateTime d5 = LocalDateTime.of(2026, 12, 5, 0, 0);
        Funcion f1 = funciones.programar(orbita.getId(), sala1.getId(), d4.withHour(18), new BigDecimal("5000"));
        Funcion f2 = funciones.programar(orbita.getId(), sala1.getId(), d4.withHour(21), new BigDecimal("5000"));
        Funcion f3 = funciones.programar(viaje.getId(), sala2.getId(), d4.withHour(19), new BigDecimal("4500"));
        Funcion f4 = funciones.programar(orbita.getId(), sala1.getId(), d5.withHour(18), new BigDecimal("5000"));
        funciones.programar(viaje.getId(), sala2.getId(), d5.withHour(19), new BigDecimal("4500"));
        funciones.programar(bosque.getId(), sala2.getId(), d5.withHour(17), new BigDecimal("4000"));
        verificar("Datos base: 3 películas, 2 salas, 6 funciones",
            estado.getPeliculas().listar().size() == 3 && estado.getSalas().listar().size() == 2
            && estado.getFunciones().listar().size() == 6);
        verificar("Sala 1 genera 24 butacas (A1..D6)", sala1.getCapacidad() == 24
            && sala1.buscarButaca("D6").isPresent());

        titulo("Prueba 5: funciones superpuestas");
        esperaError("Solapa dentro de la función existente (19:00 en Sala 1)", ReglaNegocioException.class,
            () -> funciones.programar(orbita.getId(), sala1.getId(), d4.withHour(19), new BigDecimal("5000")));
        esperaError("Solapa por la duración (Bosque 20:00-21:30 pisa la de las 21:00)", ReglaNegocioException.class,
            () -> funciones.programar(bosque.getId(), sala1.getId(), d4.withHour(20), new BigDecimal("4000")));
        Funcion contigua = funciones.programar(bosque.getId(), sala1.getId(), d5.withHour(20), new BigDecimal("4000"));
        verificar("Función que empieza justo cuando termina otra: aceptada", contigua != null);
        Funcion otraSala = funciones.programar(bosque.getId(), sala2.getId(), d4.withHour(21), new BigDecimal("4000"));
        verificar("Mismo horario en otra sala: aceptado", otraSala != null);

        titulo("Pruebas 1 y 7: reservar varias butacas, precios por polimorfismo");
        Reserva r1 = reservar(f1, "28456789", "Martín Gómez", new String[] {"B3", "B4"},
            new TipoEntrada[] {TipoEntrada.GENERAL, TipoEntrada.ESPECIAL});
        verificar("Código CIN-001 y 2 butacas reservadas", r1.getCodigo().equals("CIN-001") && r1.getEntradas().size() == 2);
        Entrada e0 = r1.getEntradas().get(0);
        Entrada e1 = r1.getEntradas().get(1);
        verificar("B3 es EntradaGeneral y cuesta 5000.00",
            e0 instanceof EntradaGeneral && e0.getPrecio().compareTo(new BigDecimal("5000")) == 0);
        verificar("B4 es EntradaEstudiante y cuesta 4000.00 (20 % de descuento)",
            e1 instanceof EntradaEspecial && e1.getPrecio().compareTo(new BigDecimal("4000")) == 0);
        verificar("Total 9000.00", r1.getTotal().compareTo(new BigDecimal("9000")) == 0);
        verificar("Butacas ocupadas en f1: B3 y B4", reservas.butacasOcupadas(f1).size() == 2);

        titulo("Prueba 2: butaca ocupada rechazada");
        esperaError("Reservar B4 (ocupada) junto con A1", ReglaNegocioException.class,
            () -> reservar(f1, "30111222", "Lucía Pérez", new String[] {"A1", "B4"},
                new TipoEntrada[] {TipoEntrada.GENERAL, TipoEntrada.GENERAL}));
        verificar("Tras el rechazo A1 sigue libre y no se creó reserva ni cliente",
            reservas.butacasOcupadas(f1).size() == 2 && reservas.listar().size() == 1
            && estado.getClientes().buscar("30111222").isEmpty());

        titulo("Prueba 3: misma ubicación en otra función");
        Reserva r2 = reservar(f2, "30111222", "Lucía Pérez", new String[] {"B3", "B4"},
            new TipoEntrada[] {TipoEntrada.GENERAL, TipoEntrada.GENERAL});
        verificar("B3 y B4 se reservan en la función de las 21:00 (misma sala)", r2.getCodigo().equals("CIN-002"));

        titulo("Prueba 4: cancelar y reutilizar butacas");
        reservas.cancelar("CIN-001");
        verificar("CIN-001 queda CANCELADA con fecha de cancelación", !r1.isConfirmada() && r1.getFechaCancelacion() != null);
        verificar("Se liberan B3 y B4 en f1", reservas.butacasOcupadas(f1).isEmpty());
        verificar("El historial se conserva (entradas y total)",
            reservas.obtener("CIN-001").getEntradas().size() == 2
            && reservas.obtener("CIN-001").getTotal().compareTo(new BigDecimal("9000")) == 0);
        esperaError("Cancelar de nuevo CIN-001", ReglaNegocioException.class, () -> reservas.cancelar("CIN-001"));
        esperaError("Cancelar una reserva inexistente", ReglaNegocioException.class, () -> reservas.cancelar("CIN-999"));
        Reserva r3 = reservar(f1, "29333444", "Ana López", new String[] {"B3", "B4"},
            new TipoEntrada[] {TipoEntrada.GENERAL, TipoEntrada.GENERAL});
        verificar("B3 y B4 se vuelven a reservar tras la cancelación", r3.getCodigo().equals("CIN-003"));

        titulo("Precio congelado al modificar el precio de la función");
        BigDecimal totalAntes = r3.getTotal();
        funciones.modificarPrecio(f1.getId(), new BigDecimal("6000"));
        verificar("Reserva anterior mantiene su importe (10000.00)",
            r3.getTotal().compareTo(totalAntes) == 0 && totalAntes.compareTo(new BigDecimal("10000")) == 0);
        Reserva r4 = reservar(f1, "29333444", "Ana López", new String[] {"C1", "C2"},
            new TipoEntrada[] {TipoEntrada.GENERAL, TipoEntrada.ESPECIAL});
        verificar("La nueva reserva usa 6000 y 4800 (total 10800.00)", r4.getTotal().compareTo(new BigDecimal("10800")) == 0);
        verificar("El cliente existente se reutilizó (sin duplicar)", estado.getClientes().listar().size() == 3);
        esperaError("Precio 0", ValidacionException.class, () -> funciones.modificarPrecio(f1.getId(), BigDecimal.ZERO));
        esperaError("Precio negativo", ValidacionException.class, () -> funciones.modificarPrecio(f1.getId(), new BigDecimal("-5")));

        titulo("Reglas de películas");
        esperaError("Cambiar duración de película con funciones", ReglaNegocioException.class,
            () -> peliculas.modificar(orbita.getId(), "Órbita", 130));
        peliculas.modificar(orbita.getId(), "Órbita (v.o.)", 120);
        verificar("Cambiar solo el título está permitido", orbita.getTitulo().equals("Órbita (v.o.)"));
        Pelicula sinFunciones = peliculas.registrar("Nueva", 80);
        peliculas.modificar(sinFunciones.getId(), "Nueva", 95);
        verificar("Película sin funciones: la duración se puede cambiar", sinFunciones.getDuracionMinutos() == 95);

        titulo("Reglas de tiempo");
        esperaError("Programar una función en el pasado", ReglaNegocioException.class,
            () -> funciones.programar(bosque.getId(), sala2.getId(), LocalDateTime.of(2026, 11, 30, 18, 0), new BigDecimal("4000")));
        verificar("La cartelera muestra funciones futuras de películas activas", funciones.cartelera("", null).size() == 8);
        verificar("Buscar por película (texto, sin distinguir mayúsculas)", funciones.cartelera("BOSQUE", null).size() == 3);
        verificar("Buscar por fecha", funciones.cartelera("", d5.toLocalDate()).size() == 4);
        reloj.irA(LocalDateTime.of(2026, 12, 4, 18, 30));
        esperaError("Reservar una función que ya comenzó (18:00 con reloj 18:30)", ReglaNegocioException.class,
            () -> reservar(f1, "11111111", "Test", new String[] {"D1"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        verificar("La cartelera deja de mostrar la función comenzada",
            funciones.cartelera("", null).stream().noneMatch(f -> f.equals(f1)));
        reloj.irA(LocalDateTime.of(2026, 12, 1, 10, 0));

        titulo("Película desactivada");
        Reserva reservaDeViaje = reservar(f3, "26000999", "Cliente Viaje", new String[] {"A1"},
            new TipoEntrada[] {TipoEntrada.GENERAL});
        peliculas.desactivar(viaje.getId());
        verificar("Sus funciones desaparecen de la cartelera",
            funciones.cartelera("", null).stream().noneMatch(f -> f.getPelicula().equals(viaje)));
        esperaError("No se puede reservar una función de película desactivada", ReglaNegocioException.class,
            () -> reservar(f3, "11111111", "Test", new String[] {"D1"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        esperaError("No se programan funciones nuevas con ella", ReglaNegocioException.class,
            () -> funciones.programar(viaje.getId(), sala2.getId(), d5.withHour(23), new BigDecimal("4500")));
        esperaError("Desactivar dos veces", ReglaNegocioException.class, () -> peliculas.desactivar(viaje.getId()));
        verificar("Las reservas existentes siguen consultables por identificación", !reservas.buscar("26000999").isEmpty());
        verificar("Una reserva de la película desactivada sigue consultable y cancelable",
            reservas.obtener(reservaDeViaje.getCodigo()).isConfirmada()
            && reservas.cancelar(reservaDeViaje.getCodigo()).getEstado().getEtiqueta().equals("Cancelada"));

        titulo("Prueba 10: datos incompletos o incorrectos (sin cierres)");
        esperaError("Película sin título", ValidacionException.class, () -> peliculas.registrar("  ", 90));
        esperaError("Película con duración 0", ValidacionException.class, () -> peliculas.registrar("X", 0));
        esperaError("Sala con 0 filas", ValidacionException.class, () -> salas.registrar("Sala X", 0, 5));
        esperaError("Sala con nombre repetido (otra capitalización)", ReglaNegocioException.class, () -> salas.registrar("sala 1", 2, 2));
        esperaError("Función con precio nulo", ValidacionException.class,
            () -> funciones.programar(bosque.getId(), sala2.getId(), d5.withHour(22), null));
        esperaError("Función con película inexistente", ReglaNegocioException.class,
            () -> funciones.programar(99, sala2.getId(), d5.withHour(22), new BigDecimal("1")));
        esperaError("Reserva sin butacas", ValidacionException.class,
            () -> reservar(f4, "11111111", "Test", new String[] {}, new TipoEntrada[] {}));
        esperaError("Reserva con butaca inexistente (Z9)", ReglaNegocioException.class,
            () -> reservar(f4, "11111111", "Test", new String[] {"Z9"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        esperaError("Cliente sin nombre", ValidacionException.class,
            () -> reservar(f4, "11111111", " ", new String[] {"A1"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        esperaError("Nombre con números", ValidacionException.class,
            () -> reservar(f4, "11111111", "Test 123", new String[] {"A1"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        esperaError("Cliente sin identificación", ValidacionException.class,
            () -> reservar(f4, "", "Test", new String[] {"A1"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        esperaError("Identificación con espacios", ValidacionException.class,
            () -> reservar(f4, "12 34", "Test", new String[] {"A1"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        esperaError("Documento con menos de 8 números", ValidacionException.class,
            () -> reservar(f4, "1234567", "Test", new String[] {"A1"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        esperaError("Documento con más de 8 números", ValidacionException.class,
            () -> reservar(f4, "123456789", "Test", new String[] {"A1"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        esperaError("Documento con letras", ValidacionException.class,
            () -> reservar(f4, "AB-123.4", "Test", new String[] {"A1"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        Reserva conDocumento = reservar(f4, "12345678", "Cliente Ocho", new String[] {"A1"}, new TipoEntrada[] {TipoEntrada.GENERAL});
        verificar("Documento de exactamente 8 números aceptado", conDocumento != null);
        reservas.cancelar(conDocumento.getCodigo());

        titulo("Prueba 8: exportación");
        int reservasAntes = reservas.listar().size();
        int ocupadasAntes = reservas.butacasOcupadas(f1).size();
        Path csv = carpeta.resolve("reservas.csv");
        reservas.exportarCsv(csv);
        List<String> lineas = Files.readAllLines(csv, StandardCharsets.UTF_8);
        int entradasTotales = reservas.listar().stream().mapToInt(r -> r.getEntradas().size()).sum();
        verificar("El CSV tiene encabezado + una fila por entrada (" + entradasTotales + ")", lineas.size() == entradasTotales + 1);
        verificar("El encabezado trae los campos esperados", lineas.get(0).contains("codigo_reserva;estado;")
            && lineas.get(0).contains("importe_total_reserva"));
        verificar("Incluye reservas canceladas con su estado",
            lineas.stream().anyMatch(l -> l.contains("CIN-001") && l.contains("Cancelada")));
        verificar("Exportar no modifica reservas ni disponibilidad",
            reservas.listar().size() == reservasAntes && reservas.butacasOcupadas(f1).size() == ocupadasAntes);
        esperaErrorPersistencia("Exportar a una ruta inválida informa el error", () -> reservas.exportarCsv(carpeta));

        titulo("Pruebas 6 y 9: guardar, cerrar y recuperar el estado completo");
        int nPel = estado.getPeliculas().listar().size();
        int nFun = estado.getFunciones().listar().size();
        int nRes = estado.getReservas().listar().size();
        int nCli = estado.getClientes().listar().size();
        int ocupadasF1 = reservas.butacasOcupadas(f1).size();
        arrancar();   // simula cerrar y volver a abrir el programa
        verificar("El archivo existe: ya no es primer inicio", !almacen.esPrimerInicio());
        verificar("Se recuperan películas, salas, funciones, clientes y reservas",
            estado.getPeliculas().listar().size() == nPel && estado.getSalas().listar().size() == 2
            && estado.getFunciones().listar().size() == nFun && estado.getClientes().listar().size() == nCli
            && estado.getReservas().listar().size() == nRes);
        Reserva r1b = reservas.obtener("CIN-001");
        verificar("CIN-001 sigue cancelada y conserva fecha y entradas", !r1b.isConfirmada()
            && r1b.getFechaCancelacion() != null && r1b.getEntradas().size() == 2);
        Reserva r4b = reservas.obtener("CIN-004");
        verificar("Los precios congelados se recuperan (CIN-004 = 10800.00; CIN-003 = 10000.00)",
            r4b.getTotal().compareTo(new BigDecimal("10800")) == 0
            && reservas.obtener("CIN-003").getTotal().compareTo(new BigDecimal("10000")) == 0);
        verificar("Se conservan las relaciones: la reserva apunta a la MISMA función del repositorio",
            r4b.getFuncion() == estado.getFunciones().buscar(r4b.getFuncion().getId()).orElseThrow()
            && r4b.getFuncion().getSala() == estado.getSalas().buscar(r4b.getFuncion().getSala().getId()).orElseThrow()
            && r4b.getCliente() == estado.getClientes().buscar("29333444").orElseThrow());
        Funcion f1b = estado.getFunciones().buscar(f1.getId()).orElseThrow();
        verificar("La ocupación de butacas se recupera", reservas.butacasOcupadas(f1b).size() == ocupadasF1);
        verificar("La película desactivada sigue desactivada", !estado.getPeliculas().buscar(viaje.getId()).orElseThrow().isActiva());
        Reserva r5b = reservar(f4b(), "27000111", "Post Reinicio", new String[] {"D6"}, new TipoEntrada[] {TipoEntrada.GENERAL});
        verificar("Los códigos continúan sin repetirse tras reiniciar (" + r5b.getCodigo() + ")",
            r5b.getCodigo().equals(String.format("CIN-%03d", nRes + 1)));

        titulo("Prueba 11: fallo de escritura y rollback en memoria");
        Path carpetaTmp = carpeta.resolve("cine.ser.tmp");
        Files.createDirectory(carpetaTmp);   // ocupa el nombre del archivo temporal: no se puede crear
        byte[] bytesAntes = Files.readAllBytes(archivo);
        int peliculasAntes = estado.getPeliculas().listar().size();
        int reservasPrev = estado.getReservas().listar().size();
        int clientesPrev = estado.getClientes().listar().size();
        Funcion fx = estado.getFunciones().buscar(f4.getId()).orElseThrow();
        Funcion fAlta = estado.getFunciones().buscar(f1.getId()).orElseThrow();

        esperaErrorPersistencia("Registrar película con fallo de escritura", () -> peliculas.registrar("Fallida", 100));
        verificar("La película no quedó en memoria", estado.getPeliculas().listar().size() == peliculasAntes);

        esperaErrorPersistencia("Confirmar reserva con fallo de escritura",
            () -> reservar(fx, "55555555", "Cliente Nuevo", new String[] {"A5"}, new TipoEntrada[] {TipoEntrada.GENERAL}));
        verificar("La reserva, el cliente nuevo y la butaca ocupada no quedaron en memoria",
            estado.getReservas().listar().size() == reservasPrev && estado.getClientes().listar().size() == clientesPrev
            && reservas.butacasOcupadas(fx).stream().noneMatch(b -> b.getCodigo().equals("A5")));

        Reserva vigente = reservas.obtener("CIN-003");
        esperaErrorPersistencia("Cancelar reserva con fallo de escritura", () -> reservas.cancelar("CIN-003"));
        verificar("CIN-003 sigue CONFIRMADA y sin fecha de cancelación",
            vigente.isConfirmada() && vigente.getFechaCancelacion() == null);

        BigDecimal precioPrev = fAlta.getPrecioGeneral();
        esperaErrorPersistencia("Modificar precio con fallo de escritura",
            () -> funciones.modificarPrecio(fAlta.getId(), new BigDecimal("9999")));
        verificar("El precio de la función volvió al anterior", fAlta.getPrecioGeneral().compareTo(precioPrev) == 0);

        String tituloPrev = estado.getPeliculas().buscar(orbita.getId()).orElseThrow().getTitulo();
        esperaErrorPersistencia("Modificar película con fallo de escritura",
            () -> peliculas.modificar(orbita.getId(), "Otro título", 120));
        verificar("El título de la película volvió al anterior",
            estado.getPeliculas().buscar(orbita.getId()).orElseThrow().getTitulo().equals(tituloPrev));

        esperaErrorPersistencia("Desactivar película con fallo de escritura", () -> peliculas.desactivar(bosque.getId()));
        verificar("La película sigue activa", estado.getPeliculas().buscar(bosque.getId()).orElseThrow().isActiva());

        esperaErrorPersistencia("Programar función con fallo de escritura",
            () -> funciones.programar(bosque.getId(), sala2.getId(), d5.withHour(22), new BigDecimal("4000")));
        verificar("La función no quedó en memoria", estado.getFunciones().listar().size() == nFun);

        verificar("El archivo de datos quedó idéntico byte a byte", Arrays.equals(bytesAntes, Files.readAllBytes(archivo)));
        verificar("La limpieza no borró la carpeta que ocupaba el nombre temporal", Files.isDirectory(carpetaTmp));

        Files.delete(carpetaTmp);
        Pelicula recuperada = peliculas.registrar("Ahora sí", 100);
        verificar("Quitado el obstáculo, el guardado funciona y el id no se perdió (rollback del contador)",
            recuperada.getId() == peliculasAntes + 1 && !Arrays.equals(bytesAntes, Files.readAllBytes(archivo)));
        Reserva reservaOk = reservar(fx, "55555555", "Cliente Nuevo", new String[] {"A5"}, new TipoEntrada[] {TipoEntrada.GENERAL});
        verificar("El código de reserva no se consumió con el intento fallido",
            reservaOk.getCodigo().equals(String.format("CIN-%03d", reservasPrev + 1)));
        verificar("No quedan archivos temporales ni de respaldo",
            !Files.exists(carpeta.resolve("cine.ser.tmp")) && !Files.exists(carpeta.resolve("cine.ser.bak")));

        titulo("Lectura: archivo ilegible y respaldo huérfano");
        Path corrupto = carpeta.resolve("corrupto.ser");
        byte[] basura = "esto no es un archivo serializado".getBytes(StandardCharsets.UTF_8);
        Files.write(corrupto, basura);
        AlmacenCine almacenCorrupto = new AlmacenCine(corrupto);
        verificar("Un archivo existente no es primer inicio", !almacenCorrupto.esPrimerInicio());
        esperaErrorPersistencia("Leer un archivo corrupto informa el error", almacenCorrupto::cargar);
        verificar("El archivo corrupto no se modificó", Arrays.equals(basura, Files.readAllBytes(corrupto)));
        Path huerfano = carpeta.resolve("huerfano.ser");
        Files.write(carpeta.resolve("huerfano.ser.bak"), basura);
        AlmacenCine almacenHuerfano = new AlmacenCine(huerfano);
        verificar("Respaldo sin archivo principal: no es primer inicio", !almacenHuerfano.esPrimerInicio());
        esperaErrorPersistencia("Respaldo huérfano: no se crea un estado nuevo", almacenHuerfano::cargar);
        verificar("No se creó el archivo principal", !Files.exists(huerfano));

        System.out.println();
        System.out.println("RESULTADO: " + correctos + " correctas, " + fallidos + " fallidas");
        if (fallidos > 0) {
            System.exit(1);
        }
    }

    private static Funcion f4b() throws Exception {
        return estado.getFunciones().buscar(4).orElseThrow();
    }

    private static void arrancar() throws PersistenciaException {
        almacen = new AlmacenCine(archivo);
        estado = almacen.cargar();
        peliculas = new PeliculaService(estado, almacen);
        salas = new SalaService(estado, almacen);
        funciones = new FuncionService(estado, almacen, reloj);
        reservas = new ReservaService(estado, almacen, reloj);
    }

    private static Reserva reservar(Funcion f, String id, String nombre, String[] butacas, TipoEntrada[] tipos)
            throws ReglaNegocioException, PersistenciaException {
        SolicitudReserva s = new SolicitudReserva(f);
        s.setCliente(id, nombre);
        s.setButacas(Arrays.asList(butacas));
        for (int i = 0; i < butacas.length; i++) {
            s.setTipo(butacas[i], tipos[i]);
        }
        return reservas.confirmar(s);
    }

    private static void titulo(String texto) {
        System.out.println("\n== " + texto);
    }

    private static void verificar(String descripcion, boolean condicion) {
        if (condicion) {
            correctos++;
            System.out.println("  [OK]    " + descripcion);
        } else {
            fallidos++;
            System.out.println("  [FALLO] " + descripcion);
        }
    }

    private static void esperaError(String descripcion, Class<? extends Exception> tipo, Accion accion) {
        try {
            accion.ejecutar();
            fallidos++;
            System.out.println("  [FALLO] " + descripcion + " (no lanzó excepción)");
        } catch (Exception e) {
            if (tipo.isInstance(e)) {
                correctos++;
                System.out.println("  [OK]    " + descripcion + " → " + e.getMessage());
            } else {
                fallidos++;
                System.out.println("  [FALLO] " + descripcion + " (lanzó " + e.getClass().getSimpleName() + ")");
            }
        }
    }

    private static void esperaErrorPersistencia(String descripcion, Accion accion) {
        esperaError(descripcion, PersistenciaException.class, accion);
    }
}
