package cine.excepciones;

/** Error de acceso a archivos (lectura, escritura o exportación). No es un error de negocio. */
public class PersistenciaException extends Exception {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }
}
