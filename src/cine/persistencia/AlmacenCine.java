package cine.persistencia;

import cine.excepciones.PersistenciaException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Lee y escribe el archivo serializado con el estado del cine.
 * La escritura es segura: nunca se pierde ni se pisa a medias el archivo anterior.
 */
public class AlmacenCine {

    private final Path archivo;
    private final Path temporal;
    private final Path respaldo;

    public AlmacenCine(Path archivo) {
        this.archivo = archivo;
        this.temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        this.respaldo = archivo.resolveSibling(archivo.getFileName() + ".bak");
    }

    public Path getArchivo() { return archivo; }

    /** Primer inicio: no existe ni el archivo de datos ni un respaldo de un guardado interrumpido. */
    public boolean esPrimerInicio() {
        return !Files.exists(archivo) && !Files.exists(respaldo);
    }

    /**
     * Primer inicio → estado inicial vacío. Archivo existente → su contenido.
     * Archivo ilegible o respaldo huérfano → excepción, sin modificar ningún archivo.
     */
    public EstadoCine cargar() throws PersistenciaException {
        if (!Files.exists(archivo)) {
            if (Files.exists(respaldo)) {
                throw new PersistenciaException("No existe " + archivo + " pero sí el respaldo " + respaldo
                    + ". Un guardado anterior pudo quedar interrumpido. No se creó un estado nuevo para"
                    + " no pisar esos datos: copiá el respaldo con el nombre " + archivo.getFileName()
                    + " para recuperarlos.");
            }
            return new EstadoCine();
        }
        try (ObjectInputStream entrada = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(archivo)))) {
            Object leido = entrada.readObject();
            if (leido instanceof EstadoCine) {
                return (EstadoCine) leido;
            }
            throw new PersistenciaException("El archivo " + archivo + " no contiene datos del cine");
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            throw new PersistenciaException("No se pudo leer " + archivo + " ("
                + e.getClass().getSimpleName() + ": " + e.getMessage() + ")", e);
        }
    }

    /**
     * Escribe en un archivo temporal, fuerza la escritura a disco y recién entonces lo
     * reemplaza por el definitivo. Si algo falla, el archivo anterior queda intacto.
     */
    public void guardar(EstadoCine estado) throws PersistenciaException {
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            escribirTemporal(estado);
            reemplazarArchivo();
        } catch (IOException e) {
            borrarTemporal();
            throw new PersistenciaException("No se pudo guardar en " + archivo + ": " + e.getMessage(), e);
        }
    }

    private void escribirTemporal(EstadoCine estado) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(temporal.toFile());
             ObjectOutputStream salida = new ObjectOutputStream(new BufferedOutputStream(fos))) {
            salida.writeObject(estado);
            salida.flush();
            fos.getFD().sync();   // después de flush() y antes de cerrar el archivo
        }
    }

    /**
     * Primero intenta el movimiento atómico solo (con ATOMIC_MOVE las otras opciones se
     * ignoran y el reemplazo depende del sistema). Si falla, usa un respaldo: el archivo
     * anterior se renombra a .bak y solo se borra cuando el nuevo ya está en su lugar.
     */
    private void reemplazarArchivo() throws IOException {
        try {
            Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE);
            return;
        } catch (IOException e) {
            // se continúa con el reemplazo mediante respaldo
        }
        if (!Files.exists(archivo)) {
            Files.move(temporal, archivo);
            return;
        }
        Files.move(archivo, respaldo, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(temporal, archivo);
        } catch (IOException e) {
            try {
                Files.move(respaldo, archivo);
            } catch (IOException restauracion) {
                e.addSuppressed(restauracion);
                throw new IOException(e.getMessage() + ". El archivo anterior quedó en " + respaldo, e);
            }
            throw e;
        }
        try {
            Files.deleteIfExists(respaldo);
        } catch (IOException ignorada) {
            // el guardado ya se completó; un .bak sobrante no afecta
        }
    }

    /** Solo borra archivos comunes: nunca una carpeta que ocupe ese nombre. */
    private void borrarTemporal() {
        try {
            if (Files.isRegularFile(temporal, LinkOption.NOFOLLOW_LINKS)) {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException ignorada) {
            // no hay nada más que hacer: el error original ya se informa
        }
    }
}
