package dev.gustavo.nidavellir.domain;

/** Quantidade de unidades em estoque. Nunca é negativa. */
public record Quantity(int value) implements Comparable<Quantity> {

    public static final Quantity ZERO = new Quantity(0);

    public Quantity {
        if (value < 0) {
            throw new InvalidValueException("A quantidade não pode ser negativa: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity add(Quantity other) {
        return new Quantity(Math.addExact(value, other.value));
    }

    /**
     * Subtrai {@code other} desta quantidade.
     *
     * @throws InvalidValueException se o resultado ficaria negativo
     */
    public Quantity subtract(Quantity other) {
        return new Quantity(value - other.value);
    }

    public boolean isZero() {
        return value == 0;
    }

    public boolean isLessThan(Quantity other) {
        return value < other.value;
    }

    @Override
    public int compareTo(Quantity other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
