package dev.gustavo.nidavellir.domain;

/**
 * Base de todas as violações de regra de negócio do domínio.
 *
 * <p>É uma exceção não verificada: regras de negócio quebradas são erros de uso do domínio,
 * e quem chama decide se trata (a CLI mostra uma mensagem amigável) ou deixa propagar.
 */
public abstract class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected DomainException(String message) {
        super(message);
    }
}
