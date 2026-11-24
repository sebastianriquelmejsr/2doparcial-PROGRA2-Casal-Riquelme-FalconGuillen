package cine.excepciones;

/** Error de negocio: el usuario intentó algo que las reglas del cine no permiten. */
public class ReglaNegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
