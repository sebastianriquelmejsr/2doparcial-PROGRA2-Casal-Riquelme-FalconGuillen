package cine.interfaz;

import cine.excepciones.PersistenciaException;
import cine.excepciones.ReglaNegocioException;
import cine.modelo.Butaca;
import cine.modelo.Entrada;
import cine.modelo.Funcion;
import cine.modelo.Pelicula;
import cine.modelo.Sala;
import cine.modelo.Reserva;
import cine.modelo.TipoEntrada;
import cine.persistencia.AlmacenCine;
import cine.persistencia.EstadoCine;
import cine.servicios.DatosDemo;
import cine.servicios.FuncionService;
import cine.servicios.PeliculaService;
import cine.servicios.ReservaService;
import cine.servicios.SalaService;
import cine.servicios.SolicitudReserva;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Interfaz JavaFX. No tiene reglas de negocio: todo lo que reserva, cancela o consulta
 * se delega en los servicios, que además guardan el estado en datos/cine.ser.
 */
public class MainApp extends Application {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));

    private static final String ESTILO_LIBRE = "-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-font-weight: bold; -fx-cursor: hand;";
    private static final String ESTILO_SELECCIONADA = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;";
    private static final String ESTILO_OCUPADA = "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-opacity: 1;";

    private BorderPane root;

    // Servicios (la fuente de verdad es el estado persistido, no esta clase)
    private PeliculaService peliculaService;
    private SalaService salaService;
    private FuncionService funcionService;
    private ReservaService reservaService;

    // Reserva en curso: todavía NO es una reserva hasta que se confirma
    private SolicitudReserva solicitud;

    @Override
    public void start(Stage primaryStage) {
        if (!inicializarServicios()) {
            Platform.exit();
            return;
        }

        primaryStage.setTitle("CINE | Sistema de Reservas - UTN FRA");
        root = new BorderPane();
        root.setPrefSize(1000, 650);

        VBox menuLateral = new VBox(12);
        menuLateral.setPadding(new Insets(15));
        menuLateral.setStyle("-fx-background-color: #1a252f;");

        Label lblTituloMenu = new Label("CINE UTN");
        lblTituloMenu.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Button btnCartelera = crearBotonMenu("📅 Cartelera");
        Button btnPeliculas = crearBotonMenu("🎬 Películas");
        Button btnSalas = crearBotonMenu("🏢 Salas");
        Button btnFunciones = crearBotonMenu("🕒 Funciones");
        Button btnReservas = crearBotonMenu("🎫 Mis Reservas");

        menuLateral.getChildren().addAll(lblTituloMenu, new Separator(), btnCartelera, btnPeliculas, btnSalas, btnFunciones, btnReservas);
        root.setLeft(menuLateral);
        root.setCenter(crearVistaCartelera());

        btnCartelera.setOnAction(e -> root.setCenter(crearVistaCartelera()));
        btnPeliculas.setOnAction(e -> root.setCenter(crearVistaPeliculas()));
        btnSalas.setOnAction(e -> root.setCenter(crearVistaSalas()));
        btnFunciones.setOnAction(e -> root.setCenter(crearVistaFunciones()));
        btnReservas.setOnAction(e -> root.setCenter(crearVistaReservas()));

        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        
        //imagen icono
        primaryStage.getIcons().add(new Image ("file:src/cine/media/movie.png"));
        
    }
   
    /** Carga el estado guardado y arma los servicios. Si el archivo no se puede leer, no se toca y se cierra. */
    private boolean inicializarServicios() {
        AlmacenCine almacen = new AlmacenCine(Paths.get("datos", "cine.ser"));
        boolean primerInicio = almacen.esPrimerInicio();
        EstadoCine estado;
        try {
            estado = almacen.cargar();
        } catch (PersistenciaException e) {
            mostrarError("Error al iniciar", e.getMessage() + "\n\nLa aplicación se cierra sin modificar el archivo de datos.");
            return false;
        }

        Clock reloj = Clock.systemDefaultZone();
        PeliculaService peliculas = new PeliculaService(estado, almacen);
        SalaService salas = new SalaService(estado, almacen);
        peliculaService = peliculas;
        salaService = salas;
        funcionService = new FuncionService(estado, almacen, reloj);
        reservaService = new ReservaService(estado, almacen, reloj);

        if (primerInicio) {
            Alert pregunta = new Alert(Alert.AlertType.CONFIRMATION,
                "No hay datos guardados. ¿Cargar los datos de demostración?", ButtonType.YES, ButtonType.NO);
            pregunta.setHeaderText("Primer inicio");
            if (pregunta.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                try {
                    DatosDemo.cargar(peliculas, salas, funcionService, reservaService, reloj);
                } catch (ReglaNegocioException | PersistenciaException e) {
                    mostrarError("No se pudieron cargar los datos de demostración", e.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    private Button crearBotonMenu(String texto) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        return btn;
    }

    // ==========================================
    // VISTA 1: CARTELERA (funciones reales)
    // ==========================================
    private VBox crearVistaCartelera() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #ecf0f1;");

        Label titulo = new Label("Cartelera de Funciones Disponibles");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por película...");

        TableView<Funcion> tabla = new TableView<>();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.setPlaceholder(new Label("No hay funciones disponibles (todas comenzaron o no hay datos cargados)."));

        TableColumn<Funcion, String> colPelicula = new TableColumn<>("Película");
        colPelicula.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPelicula().getTitulo()));
        colPelicula.setPrefWidth(220);
        TableColumn<Funcion, String> colSala = new TableColumn<>("Sala");
        colSala.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSala().getNombre()));
        colSala.setPrefWidth(100);
        TableColumn<Funcion, String> colHorario = new TableColumn<>("Horario");
        colHorario.setCellValueFactory(d -> new SimpleStringProperty(FORMATO_FECHA_HORA.format(d.getValue().getInicio())));
        colHorario.setPrefWidth(180);
        TableColumn<Funcion, String> colPrecio = new TableColumn<>("Precio General");
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(MONEDA.format(d.getValue().getPrecioGeneral())));
        colPrecio.setPrefWidth(130);
        tabla.getColumns().addAll(colPelicula, colSala, colHorario, colPrecio);

        Runnable refrescar = () -> tabla.getItems().setAll(funcionService.cartelera(txtBuscar.getText(), null));
        refrescar.run();
        txtBuscar.textProperty().addListener((obs, a, b) -> refrescar.run());
        
        ImageView poster = new ImageView();
        poster.setFitWidth(160);
        poster.setFitHeight(230);
        poster.setPreserveRatio(true);

        tabla.setOnMouseClicked(e -> {
            Funcion seleccionada = tabla.getSelectionModel().getSelectedItem();
            if (seleccionada != null) {
                poster.setImage(ImagenPelicula(seleccionada.getPelicula()));
            }
        });
        
        HBox filaCentral = new HBox(15, tabla, poster);
        HBox.setHgrow(tabla, Priority.ALWAYS);
        VBox.setVgrow(filaCentral, Priority.ALWAYS);
        

        Button btnReservar = new Button("Seleccionar Butacas →");
        btnReservar.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-cursor: hand;");
        btnReservar.setOnAction(e -> {
            Funcion elegida = tabla.getSelectionModel().getSelectedItem();
            if (elegida == null) {
                mostrarAdvertencia("Seleccioná una función de la tabla.");
                return;
            }
            solicitud = new SolicitudReserva(elegida);   // selección nueva: sin butacas ni datos previos
            root.setCenter(crearVistaSeleccionButacas());
        });

        vbox.getChildren().addAll(titulo, txtBuscar, filaCentral, btnReservar);
        return vbox;
    }

    // ==========================================
    // VISTA 2: SELECCIÓN DE BUTACAS (ocupadas leídas de las reservas guardadas)
    // ==========================================
    private VBox crearVistaSeleccionButacas() {
        Funcion funcion = solicitud.getFuncion();

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #ecf0f1;");

        Label titulo = new Label("Selección de Butacas - " + funcion.getPelicula().getTitulo()
            + " (" + funcion.getSala().getNombre() + ", " + FORMATO_FECHA_HORA.format(funcion.getInicio()) + ")");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label pantalla = new Label("Seleccione Su Asiento");
        pantalla.setStyle("-fx-background-color: #bdc3c7; -fx-padding: 4px; -fx-alignment: center; -fx-font-weight: bold;");
        pantalla.setMaxWidth(Double.MAX_VALUE);

        // Se consulta cada vez que se arma la vista: así un cliente nuevo ve lo que reservó el anterior
        Set<Butaca> ocupadas = reservaService.butacasOcupadas(funcion);
        // Si alguna butaca elegida se ocupó mientras tanto, se descarta de la selección
        List<String> vigentes = new ArrayList<>();
        for (String codigo : solicitud.getCodigosButacas()) {
            boolean ocupada = ocupadas.stream().anyMatch(b -> b.getCodigo().equals(codigo));
            if (!ocupada) vigentes.add(codigo);
        }
        solicitud.setButacas(vigentes);
        List<String> seleccion = new ArrayList<>(vigentes);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        String filaActual = null;
        int filaGrid = -1;
        for (Butaca butaca : funcion.getSala().getButacas()) {
            if (!butaca.getFila().equals(filaActual)) {
                filaActual = butaca.getFila();
                filaGrid++;
                Label lblFila = new Label(filaActual);
                lblFila.setStyle("-fx-font-weight: bold;");
                grid.add(lblFila, 0, filaGrid);
            }
            String codigo = butaca.getCodigo();
            Button btn = new Button(codigo);
            btn.setPrefSize(55, 45);
            grid.add(btn, butaca.getNumero(), filaGrid);

            if (ocupadas.contains(butaca)) {
                btn.setStyle(ESTILO_OCUPADA);
                btn.setDisable(true);
            } else {
                btn.setStyle(seleccion.contains(codigo) ? ESTILO_SELECCIONADA : ESTILO_LIBRE);
                btn.setOnAction(e -> {
                    if (seleccion.contains(codigo)) {
                        seleccion.remove(codigo);
                        btn.setStyle(ESTILO_LIBRE);
                    } else {
                        seleccion.add(codigo);
                        btn.setStyle(ESTILO_SELECCIONADA);
                    }
                    solicitud.setButacas(seleccion);
                });
            }
        }

        HBox leyenda = new HBox(20, itemLeyenda("Disponible", "white"), itemLeyenda("Seleccionada", "#27ae60"), itemLeyenda("Ocupada", "#c0392b"));
        leyenda.setAlignment(Pos.CENTER);

        HBox panelBotones = new HBox(15);
        Button btnVolver = new Button("← Volver a Cartelera");
        Button btnContinuar = new Button("Continuar al Cliente →");
        btnContinuar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnVolver.setOnAction(e -> root.setCenter(crearVistaCartelera()));
        btnContinuar.setOnAction(e -> {
            if (solicitud.getCodigosButacas().isEmpty()) {
                mostrarAdvertencia("Debe seleccionar al menos una butaca para continuar.");
            } else {
                root.setCenter(crearVistaCliente());
            }
        });
        panelBotones.getChildren().addAll(btnVolver, btnContinuar);

        vbox.getChildren().addAll(titulo, pantalla, grid, leyenda, panelBotones);
        return vbox;
    }

    private HBox itemLeyenda(String texto, String color) {
        Label cuadro = new Label("   ");
        cuadro.setStyle("-fx-background-color: " + color + "; -fx-border-color: #7f8c8d;");
        HBox h = new HBox(6, cuadro, new Label(texto));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    // ==========================================
    // VISTA 3: CLIENTE, TIPOS DE ENTRADA Y CONFIRMACIÓN
    // ==========================================
    private VBox crearVistaCliente() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #ecf0f1;");

        Label titulo = new Label("Datos del Cliente y Tipos de Entrada");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        TextField txtNombre = new TextField(solicitud.getNombre());
        txtNombre.setPromptText("Ej: Martín Gómez");
        TextField txtIdentificacion = new TextField(solicitud.getIdentificacion());
        txtIdentificacion.setPromptText("Ej: 28456789");
        txtNombre.setTextFormatter(new TextFormatter<String>(c ->
            c.getControlNewText().matches("[\\p{L} ]*") ? c : null));
        txtIdentificacion.setTextFormatter(new TextFormatter<String>(c ->
            c.getControlNewText().matches("\\d{0,8}") ? c : null));
        form.add(new Label("Nombre y Apellido:"), 0, 0);
        form.add(txtNombre, 1, 0);
        form.add(new Label("Documento (8 números):"), 0, 1);
        form.add(txtIdentificacion, 1, 1);

        Label lblEntradas = new Label("Tipo de entrada por butaca:");
        lblEntradas.setStyle("-fx-font-weight: bold;");
        Label lblBeneficio = new Label("ESPECIAL: BENEFICIO DEL 20 % A JUBILADOS,DISCAPACITADOS, MENORES, ESTUDIANTES Y EX/COMBATIENTES");
        lblBeneficio.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
        lblBeneficio.setWrapText(true);
        Label lblTotal = new Label();
        lblTotal.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Map<String, ComboBox<TipoEntrada>> combos = new HashMap<>();
        VBox contenedor = new VBox(10);
        for (String codigo : solicitud.getCodigosButacas()) {
            ComboBox<TipoEntrada> cmb = new ComboBox<>();
            cmb.getItems().addAll(TipoEntrada.values());
            cmb.setValue(solicitud.getTipo(codigo));
            cmb.setOnAction(e -> {
                solicitud.setTipo(codigo, cmb.getValue());
                actualizarTotal(lblTotal);
            });
            combos.put(codigo, cmb);
            contenedor.getChildren().add(new HBox(10, new Label("Butaca " + codigo + ":"), cmb));
        }
        actualizarTotal(lblTotal);

        Button btnVolver = new Button("← Volver a Butacas");
        Button btnConfirmar = new Button("Confirmar Reserva");
        btnConfirmar.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        btnVolver.setOnAction(e -> {
            solicitud.setCliente(txtIdentificacion.getText(), txtNombre.getText());   // no se pierde lo ingresado
            root.setCenter(crearVistaSeleccionButacas());
        });

        btnConfirmar.setOnAction(e -> {
            solicitud.setCliente(txtIdentificacion.getText(), txtNombre.getText());
            if (!txtNombre.getText().trim().matches("[\\p{L} ]+")) {
                mostrarAdvertencia("El nombre solo puede contener letras y espacios (sin números ni símbolos).");
                return;   // no se pierde lo ingresado
            }
            if (!txtIdentificacion.getText().trim().matches("\\d{8}")) {
                mostrarAdvertencia("El documento debe tener exactamente 8 números (sin puntos, letras ni espacios).");
                return;   // no se pierde lo ingresado
            }
            try {
                Reserva reserva = reservaService.confirmar(solicitud);   // valida, reserva y GUARDA en el archivo
                mostrarComprobante(reserva);
                solicitud = null;
                root.setCenter(crearVistaCartelera());
            } catch (PersistenciaException ex) {
                mostrarError("Error de almacenamiento", ex.getMessage() + "\n\nLa reserva NO se guardó.");
            } catch (ReglaNegocioException ex) {
                mostrarError("No se pudo reservar", ex.getMessage());
                // Si el problema es de disponibilidad, se vuelve al mapa para mostrar lo que está ocupado
                if (ex.getMessage() != null && ex.getMessage().contains("reservad")) {
                    root.setCenter(crearVistaSeleccionButacas());
                }
            }
        });

        HBox panelBotones = new HBox(15, btnVolver, btnConfirmar);
        vbox.getChildren().addAll(titulo, form, new Separator(), lblEntradas, lblBeneficio, contenedor, lblTotal, new Separator(), panelBotones);
        //vbox.getChildren().addAll(titulo, form, new Separator(), lblEntradas, contenedor, lblTotal, new Separator(), panelBotones);
        return vbox;
    }

    private void actualizarTotal(Label lblTotal) {
        try {
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (Entrada en : reservaService.calcularEntradas(prepararParaCalculo())) {
                total = total.add(en.getPrecio());
            }
            lblTotal.setText("Total estimado: " + MONEDA.format(total));
        } catch (ReglaNegocioException ex) {
            lblTotal.setText("Total estimado: (" + ex.getMessage() + ")");
        }
    }

    /** El cálculo exige datos de cliente; para estimar el total se usa una copia con datos provisorios. */
    private SolicitudReserva prepararParaCalculo() {
        SolicitudReserva copia = new SolicitudReserva(solicitud.getFuncion());
        copia.setButacas(solicitud.getCodigosButacas());
        for (Map.Entry<String, TipoEntrada> t : solicitud.getTipos().entrySet()) {
            copia.setTipo(t.getKey(), t.getValue());
        }
        copia.setCliente("00000000", "Provisorio");
        return copia;
    }

    private void mostrarComprobante(Reserva r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Código: ").append(r.getCodigo()).append('\n');
        sb.append("Cliente: ").append(r.getCliente()).append('\n');
        sb.append("Película: ").append(r.getFuncion().getPelicula().getTitulo()).append('\n');
        sb.append("Función: ").append(r.getFuncion().getSala().getNombre()).append(" - ")
          .append(FORMATO_FECHA_HORA.format(r.getFuncion().getInicio())).append("\n\n");
        for (Entrada e : r.getEntradas()) {
            sb.append("  Butaca ").append(e.getButaca().getCodigo()).append(" - ")
              .append(e.getTipo().getEtiqueta()).append(": ").append(MONEDA.format(e.getPrecio())).append('\n');
        }
        sb.append("\nTOTAL: ").append(MONEDA.format(r.getTotal()));
        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString());
        a.setTitle("Comprobante");
        a.setHeaderText("¡Reserva confirmada y guardada!");
        a.showAndWait();
    }

    // ==========================================
    // VISTA: CONSULTA Y CANCELACIÓN DE RESERVAS
    // ==========================================
    private VBox crearVistaReservas() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #ecf0f1;");

        Label titulo = new Label("Consulta y Cancelación de Reservas");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Código de reserva o DNI del cliente");

        TableView<Reserva> tabla = new TableView<>();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.getColumns().add(columna("Código", 90, r -> r.getCodigo()));
        tabla.getColumns().add(columna("Cliente", 170, r -> r.getCliente().toString()));
        tabla.getColumns().add(columna("Película", 150, r -> r.getFuncion().getPelicula().getTitulo()));
        tabla.getColumns().add(columna("Función", 150, r -> FORMATO_FECHA_HORA.format(r.getFuncion().getInicio())));
        tabla.getColumns().add(columna("Butacas", 110, r -> {
            List<String> codigos = new ArrayList<>();
            for (Entrada e : r.getEntradas()) codigos.add(e.getButaca().getCodigo());
            return String.join(", ", codigos);
        }));
        tabla.getColumns().add(columna("Total", 90, r -> MONEDA.format(r.getTotal())));
        tabla.getColumns().add(columna("Estado", 100, r -> r.getEstado().getEtiqueta()));

        Runnable refrescar = () -> tabla.getItems().setAll(reservaService.buscar(txtBuscar.getText()));
        refrescar.run();
        txtBuscar.textProperty().addListener((obs, a, b) -> refrescar.run());

        Button btnCancelar = new Button("Cancelar reserva seleccionada");
        btnCancelar.setOnAction(e -> {
            Reserva sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) {
                mostrarAdvertencia("Seleccioná una reserva.");
                return;
            }
            try {
                reservaService.cancelar(sel.getCodigo());   // libera las butacas y guarda
                refrescar.run();
            } catch (PersistenciaException ex) {
                mostrarError("Error de almacenamiento", ex.getMessage() + "\n\nLa cancelación NO se guardó.");
            } catch (ReglaNegocioException ex) {
                mostrarError("No se pudo cancelar", ex.getMessage());
            }
        });

        Button btnExportar = new Button("Exportar a CSV...");
        btnExportar.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("reservas.csv");
            File destino = fc.showSaveDialog(root.getScene().getWindow());
            if (destino == null) return;
            try {
                reservaService.exportarCsv(destino.toPath());
                new Alert(Alert.AlertType.INFORMATION, "Reservas exportadas a:\n" + destino).showAndWait();
            } catch (PersistenciaException ex) {
                mostrarError("No se pudo exportar", ex.getMessage());
            }
        });

        vbox.getChildren().addAll(titulo, txtBuscar, tabla, new HBox(10, btnCancelar, btnExportar));
        return vbox;
    }

    private TableColumn<Reserva, String> columna(String nombre, double ancho, java.util.function.Function<Reserva, String> f) {
        TableColumn<Reserva, String> c = new TableColumn<>(nombre);
        c.setCellValueFactory(d -> new SimpleStringProperty(f.apply(d.getValue())));
        c.setPrefWidth(ancho);
        return c;
    }

    // ==========================================
    // VISTA: PELÍCULAS (alta, modificación y baja lógica)
    // ==========================================
    private VBox crearVistaPeliculas() {
        VBox vbox = vistaBase("Administración de Películas");

        TableView<Pelicula> tabla = new TableView<>();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.getColumns().add(columnaGenerica("Id", 50, p -> String.valueOf(p.getId())));
        tabla.getColumns().add(columnaGenerica("Título", 280, Pelicula::getTitulo));
        tabla.getColumns().add(columnaGenerica("Duración (min)", 120, p -> String.valueOf(p.getDuracionMinutos())));
        tabla.getColumns().add(columnaGenerica("Estado", 100, p -> p.isActiva() ? "Activa" : "Desactivada"));
        Runnable refrescar = () -> tabla.getItems().setAll(peliculaService.listar());
        refrescar.run();

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Título");
        TextField txtDuracion = new TextField();
        txtDuracion.setPromptText("Minutos");
        // Al elegir una fila se cargan sus datos para poder modificarla
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, a, p) -> {
            if (p != null) {
                txtTitulo.setText(p.getTitulo());
                txtDuracion.setText(String.valueOf(p.getDuracionMinutos()));
            }
        });

        Button btnRegistrar = new Button("Registrar nueva");
        btnRegistrar.setOnAction(e -> {
            Integer duracion = leerEntero(txtDuracion.getText(), "La duración");
            if (duracion == null) return;
            try {
                peliculaService.registrar(txtTitulo.getText(), duracion);
                txtTitulo.clear();
                txtDuracion.clear();
                refrescar.run();
            } catch (PersistenciaException ex) {
                mostrarError("Error de almacenamiento", ex.getMessage() + "\n\nLa película NO se guardó.");
            } catch (ReglaNegocioException ex) {
                mostrarError("No se pudo registrar", ex.getMessage());
            }
        });

        Button btnModificar = new Button("Modificar seleccionada");
        btnModificar.setOnAction(e -> {
            Pelicula sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) { mostrarAdvertencia("Seleccioná una película de la tabla."); return; }
            Integer duracion = leerEntero(txtDuracion.getText(), "La duración");
            if (duracion == null) return;
            try {
                peliculaService.modificar(sel.getId(), txtTitulo.getText(), duracion);
                refrescar.run();
            } catch (PersistenciaException ex) {
                mostrarError("Error de almacenamiento", ex.getMessage() + "\n\nEl cambio NO se guardó.");
            } catch (ReglaNegocioException ex) {
                mostrarError("No se pudo modificar", ex.getMessage());
            }
        });

        Button btnDesactivar = new Button("Desactivar seleccionada");
        btnDesactivar.setOnAction(e -> {
            Pelicula sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) { mostrarAdvertencia("Seleccioná una película de la tabla."); return; }
            try {
                peliculaService.desactivar(sel.getId());
                refrescar.run();
            } catch (PersistenciaException ex) {
                mostrarError("Error de almacenamiento", ex.getMessage() + "\n\nEl cambio NO se guardó.");
            } catch (ReglaNegocioException ex) {
                mostrarError("No se pudo desactivar", ex.getMessage());
            }
        });

        HBox form = new HBox(10, new Label("Título:"), txtTitulo, new Label("Duración:"), txtDuracion);
        form.setAlignment(Pos.CENTER_LEFT);
        vbox.getChildren().addAll(tabla, form, new HBox(10, btnRegistrar, btnModificar, btnDesactivar));
        return vbox;
    }

    // ==========================================
    // VISTA: SALAS (alta con distribución de filas y butacas)
    // ==========================================
    private VBox crearVistaSalas() {
        VBox vbox = vistaBase("Administración de Salas");

        TableView<Sala> tabla = new TableView<>();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.getColumns().add(columnaGenerica("Id", 50, s -> String.valueOf(s.getId())));
        tabla.getColumns().add(columnaGenerica("Nombre", 200, Sala::getNombre));
        tabla.getColumns().add(columnaGenerica("Filas", 80, s -> String.valueOf(s.getFilas())));
        tabla.getColumns().add(columnaGenerica("Butacas por fila", 130, s -> String.valueOf(s.getButacasPorFila())));
        tabla.getColumns().add(columnaGenerica("Capacidad", 100, s -> String.valueOf(s.getCapacidad())));
        Runnable refrescar = () -> tabla.getItems().setAll(salaService.listar());
        refrescar.run();

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej: Sala 3");
        TextField txtFilas = new TextField();
        txtFilas.setPromptText("Filas");
        txtFilas.setPrefWidth(70);
        TextField txtPorFila = new TextField();
        txtPorFila.setPromptText("Por fila");
        txtPorFila.setPrefWidth(70);

        Button btnRegistrar = new Button("Registrar sala");
        btnRegistrar.setOnAction(e -> {
            Integer filas = leerEntero(txtFilas.getText(), "La cantidad de filas");
            if (filas == null) return;
            Integer porFila = leerEntero(txtPorFila.getText(), "Las butacas por fila");
            if (porFila == null) return;
            try {
                salaService.registrar(txtNombre.getText(), filas, porFila);
                txtNombre.clear();
                txtFilas.clear();
                txtPorFila.clear();
                refrescar.run();
            } catch (PersistenciaException ex) {
                mostrarError("Error de almacenamiento", ex.getMessage() + "\n\nLa sala NO se guardó.");
            } catch (ReglaNegocioException ex) {
                mostrarError("No se pudo registrar", ex.getMessage());
            }
        });

        HBox form = new HBox(10, new Label("Nombre:"), txtNombre, new Label("Filas:"), txtFilas,
            new Label("Butacas por fila:"), txtPorFila, btnRegistrar);
        form.setAlignment(Pos.CENTER_LEFT);
        vbox.getChildren().addAll(tabla, form);
        return vbox;
    }

    // ==========================================
    // VISTA: FUNCIONES (programación y cambio de precio)
    // ==========================================
    private VBox crearVistaFunciones() {
        VBox vbox = vistaBase("Programación de Funciones");

        TableView<Funcion> tabla = new TableView<>();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tabla.getColumns().add(columnaGenerica("Id", 50, f -> String.valueOf(f.getId())));
        tabla.getColumns().add(columnaGenerica("Película", 200, f -> f.getPelicula().getTitulo()));
        tabla.getColumns().add(columnaGenerica("Sala", 90, f -> f.getSala().getNombre()));
        tabla.getColumns().add(columnaGenerica("Inicio", 150, f -> FORMATO_FECHA_HORA.format(f.getInicio())));
        tabla.getColumns().add(columnaGenerica("Fin", 150, f -> FORMATO_FECHA_HORA.format(f.getFin())));
        tabla.getColumns().add(columnaGenerica("Precio general", 120, f -> MONEDA.format(f.getPrecioGeneral())));
        Runnable refrescar = () -> tabla.getItems().setAll(funcionService.listar());
        refrescar.run();

        ComboBox<Pelicula> cmbPelicula = new ComboBox<>();
        cmbPelicula.getItems().setAll(peliculaService.listarActivas());
        cmbPelicula.setPromptText("Película");
        ComboBox<Sala> cmbSala = new ComboBox<>();
        cmbSala.getItems().setAll(salaService.listar());
        cmbSala.setPromptText("Sala");
        DatePicker fecha = new DatePicker();
        fecha.setPromptText("Fecha");
        TextField txtHora = new TextField();
        txtHora.setPromptText("HH:mm");
        txtHora.setPrefWidth(70);
        TextField txtPrecio = new TextField();
        txtPrecio.setPromptText("Precio general");
        txtPrecio.setPrefWidth(110);
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, a, f) -> {
            if (f != null) txtPrecio.setText(f.getPrecioGeneral().toPlainString());
        });

        Button btnProgramar = new Button("Programar función");
        btnProgramar.setOnAction(e -> {
            if (cmbPelicula.getValue() == null || cmbSala.getValue() == null) {
                mostrarAdvertencia("Elegí una película y una sala."); return;
            }
            LocalDate dia = fecha.getValue();
            if (dia == null) { mostrarAdvertencia("Elegí una fecha."); return; }
            LocalTime hora;
            try {
                hora = LocalTime.parse(txtHora.getText().trim());
            } catch (DateTimeParseException ex) {
                mostrarAdvertencia("La hora debe tener el formato HH:mm (por ejemplo 18:30)."); return;
            }
            BigDecimal precio = leerPrecio(txtPrecio.getText());
            if (precio == null) return;
            try {
                funcionService.programar(cmbPelicula.getValue().getId(), cmbSala.getValue().getId(),
                    LocalDateTime.of(dia, hora), precio);
                refrescar.run();
            } catch (PersistenciaException ex) {
                mostrarError("Error de almacenamiento", ex.getMessage() + "\n\nLa función NO se guardó.");
            } catch (ReglaNegocioException ex) {
                mostrarError("No se pudo programar", ex.getMessage());   // incluye superposición de horarios
            }
        });

        Button btnPrecio = new Button("Cambiar precio de la seleccionada");
        btnPrecio.setOnAction(e -> {
            Funcion sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) { mostrarAdvertencia("Seleccioná una función de la tabla."); return; }
            BigDecimal precio = leerPrecio(txtPrecio.getText());
            if (precio == null) return;
            try {
                funcionService.modificarPrecio(sel.getId(), precio);
                refrescar.run();
            } catch (PersistenciaException ex) {
                mostrarError("Error de almacenamiento", ex.getMessage() + "\n\nEl cambio NO se guardó.");
            } catch (ReglaNegocioException ex) {
                mostrarError("No se pudo modificar el precio", ex.getMessage());
            }
        });

        HBox fila1 = new HBox(10, new Label("Película:"), cmbPelicula, new Label("Sala:"), cmbSala);
        HBox fila2 = new HBox(10, new Label("Fecha:"), fecha, new Label("Hora:"), txtHora, new Label("Precio:"), txtPrecio);
        fila1.setAlignment(Pos.CENTER_LEFT);
        fila2.setAlignment(Pos.CENTER_LEFT);
        vbox.getChildren().addAll(tabla, fila1, fila2, new HBox(10, btnProgramar, btnPrecio));
        return vbox;
    }

    private VBox vistaBase(String tituloTexto) {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #ecf0f1;");
        Label titulo = new Label(tituloTexto);
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        vbox.getChildren().add(titulo);
        return vbox;
    }

    private <T> TableColumn<T, String> columnaGenerica(String nombre, double ancho, java.util.function.Function<T, String> f) {
        TableColumn<T, String> c = new TableColumn<>(nombre);
        c.setCellValueFactory(d -> new SimpleStringProperty(f.apply(d.getValue())));
        c.setPrefWidth(ancho);
        return c;
    }

    /** Devuelve null (y avisa) si el texto no es un entero: así no se pierde lo ingresado. */
    private Integer leerEntero(String texto, String campo) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            mostrarAdvertencia(campo + " debe ser un número entero.");
            return null;
        }
    }

    private BigDecimal leerPrecio(String texto) {
        try {
            return new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            mostrarAdvertencia("El precio debe ser un número (por ejemplo 5000).");
            return null;
        }
    }

    // ==========================================
    // Mensajes al usuario (nunca solo por consola)
    // ==========================================
    private void mostrarError(String cabecera, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR, mensaje);
        a.setHeaderText(cabecera);
        a.showAndWait();
    }

    private void mostrarAdvertencia(String mensaje) {
        new Alert(Alert.AlertType.WARNING, mensaje).showAndWait();
    }
    
    private Image ImagenPelicula(Pelicula pelicula) {
        if (pelicula == null || pelicula.getTitulo() == null) {
            return null;
        }

        String nombre = pelicula.getTitulo().trim().toLowerCase().replace(" ", "_") + ".jpg";
        String ruta = "src/cine/media/" + nombre;
        File archivo = new File(ruta);

        if (archivo.exists()) {
            return new Image("file:" + ruta);
        }

        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }

}