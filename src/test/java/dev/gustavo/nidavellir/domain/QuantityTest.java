package dev.gustavo.nidavellir.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Quantity")
class QuantityTest {

    @Test
    @DisplayName("soma e subtrai")
    void addsAndSubtracts() {
        assertThat(Quantity.of(5).add(Quantity.of(3))).isEqualTo(Quantity.of(8));
        assertThat(Quantity.of(5).subtract(Quantity.of(5))).isEqualTo(Quantity.ZERO);
    }

    @Test
    @DisplayName("rejeita valor negativo")
    void rejectsNegative() {
        assertThatThrownBy(() -> Quantity.of(-1)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("subtração que ficaria negativa é rejeitada")
    void rejectsNegativeResult() {
        assertThatThrownBy(() -> Quantity.of(2).subtract(Quantity.of(3)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("soma que estoura int é rejeitada")
    void rejectsOverflow() {
        assertThatThrownBy(() -> Quantity.of(Integer.MAX_VALUE).add(Quantity.of(1)))
                .isInstanceOf(ArithmeticException.class);
    }
}
