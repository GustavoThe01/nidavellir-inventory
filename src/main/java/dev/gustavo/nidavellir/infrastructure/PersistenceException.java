package dev.gustavo.nidavellir.infrastructure;

/** Falha técnica de banco. Esconde o {@code SQLException} verificado das camadas de dentro. */
public final class PersistenceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
