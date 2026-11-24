package cine.excepciones;

/** Dato obligatorio ausente o con valor inválido (título vacío, precio negativo, etc.). */
public class ValidacionException extends ReglaNegocioException {

    private static final long serialVersionUID = 1L;

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
