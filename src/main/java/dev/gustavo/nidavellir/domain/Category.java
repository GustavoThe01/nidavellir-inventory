package dev.gustavo.nidavellir.domain;

import java.util.Locale;

/** Categoria do produto. Duas categorias são iguais mesmo com diferença de maiúsculas no cadastro. */
public record Category(String name) {

    private static final int MAX_LENGTH = 50;

    public Category {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("A categoria é obrigatória.");
        }
        name = name.strip();
        if (name.length() > MAX_LENGTH) {
            throw new InvalidValueException("A categoria deve ter no máximo %d caracteres.".formatted(MAX_LENGTH));
        }
    }

    public static Category of(String name) {
        return new Category(name);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Category category && name.equalsIgnoreCase(category.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
