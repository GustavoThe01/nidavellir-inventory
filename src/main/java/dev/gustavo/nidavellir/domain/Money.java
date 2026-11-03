package dev.gustavo.nidavellir.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetário em reais, sempre com 2 casas decimais.
 *
 * <p>Uso {@link BigDecimal} porque {@code double} não representa valores como 0,10 com exatidão.
 * Valores com mais de 2 casas são rejeitados, em vez de arredondados em silêncio.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    private static final int SCALE = 2;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            throw new InvalidValueException("O valor é obrigatório.");
        }
        if (amount.signum() < 0) {
            throw new InvalidValueException("O valor não pode ser negativo: " + amount.toPlainString());
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidValueException(
                    "O valor deve ter no máximo %d casas decimais: %s".formatted(SCALE, amount.toPlainString()));
        }
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Money of(String amount) {
        try {
            return new Money(new BigDecimal(amount));
        } catch (NumberFormatException e) {
            throw new InvalidValueException("Valor monetário inválido: '%s'.".formatted(amount));
        }
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money multiply(int times) {
        if (times < 0) {
            throw new InvalidValueException("O multiplicador não pode ser negativo: " + times);
        }
        return new Money(amount.multiply(BigDecimal.valueOf(times)));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return "R$ " + amount.toPlainString();
    }
}
