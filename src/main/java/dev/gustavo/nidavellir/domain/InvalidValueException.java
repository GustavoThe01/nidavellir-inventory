package dev.gustavo.nidavellir.domain;

/** Lançada quando um valor não respeita as regras do seu tipo (SKU mal formado, preço zero...). */
public final class InvalidValueException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidValueException(String message) {
        super(message);
    }
}
