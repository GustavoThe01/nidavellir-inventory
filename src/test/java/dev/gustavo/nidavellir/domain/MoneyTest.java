package dev.gustavo.nidavellir.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Money")
class MoneyTest {

    @Test
    @DisplayName("sempre guarda 2 casas decimais")
    void normalizesScale() {
        assertThat(Money.of("10").amount()).isEqualByComparingTo("10.00");
        assertThat(Money.of("10").amount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("10 e 10.00 são o mesmo valor")
    void equalityIgnoresInputScale() {
        assertThat(Money.of("10")).isEqualTo(Money.of("10.00"));
    }

    @Test
    @DisplayName("soma sem erro de ponto flutuante (0,10 + 0,20 = 0,30)")
    void addsExactly() {
        assertThat(Money.of("0.10").add(Money.of("0.20"))).isEqualTo(Money.of("0.30"));
    }

    @Test
    @DisplayName("multiplica pela quantidade")
    void multiplies() {
        assertThat(Money.of("19.90").multiply(3)).isEqualTo(Money.of("59.70"));
    }

    @Test
    @DisplayName("rejeita valor negativo")
    void rejectsNegative() {
        assertThatThrownBy(() -> Money.of("-0.01")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("rejeita mais de 2 casas decimais em vez de arredondar escondido")
    void rejectsMoreThanTwoDecimals() {
        assertThatThrownBy(() -> Money.of("1.005"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("casas decimais");
    }

    @Test
    @DisplayName("aceita zeros à direita além da segunda casa")
    void acceptsTrailingZeros() {
        assertThat(new Money(new BigDecimal("5.5000"))).isEqualTo(Money.of("5.50"));
    }

    @Test
    @DisplayName("rejeita texto que não é número")
    void rejectsNonNumeric() {
        assertThatThrownBy(() -> Money.of("dez reais")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("compara valores")
    void compares() {
        assertThat(Money.of("2.00")).isGreaterThan(Money.of("1.99"));
    }
}
