package cine.persistencia;

import java.io.Serializable;

/** Contador de identificadores. Se serializa con el estado para que no se repitan al reiniciar. */
public class Secuencia implements Serializable {

    private static final long serialVersionUID = 1L;

    private int proximo = 1;

    /** Devuelve el valor actual y avanza. */
    public int siguiente() {
        return proximo++;
    }

    /** Valor que se entregaría en la próxima llamada a siguiente(). */
    public int getProximo() { return proximo; }

    /** Solo se usa para revertir un alta cuando falla el guardado. */
    public void restaurar(int valor) {
        this.proximo = valor;
    }
}
