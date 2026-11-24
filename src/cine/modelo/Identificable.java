package cine.modelo;

/** Contrato de toda entidad que tiene un identificador de tipo K. */
public interface Identificable<K> {

    K getId();
}
