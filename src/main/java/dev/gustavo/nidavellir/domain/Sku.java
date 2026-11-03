package dev.gustavo.nidavellir.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Código único de um produto (Stock Keeping Unit).
 *
 * <p>É normalizado para maiúsculas e sem espaços nas pontas, então {@code " cn-001 "} e
 * {@code "CN-001"} representam o mesmo SKU.
 */
public record Sku(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]+(-[A-Z0-9]+)*");
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 20;

    public Sku {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("O SKU é obrigatório.");
        }
        value = value.strip().toUpperCase(Locale.ROOT);
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    "O SKU deve ter entre %d e %d caracteres.".formatted(MIN_LENGTH, MAX_LENGTH));
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidValueException(
                    "SKU inválido: '%s'. Use letras, números e hífens entre eles.".formatted(value));
        }
    }

    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
