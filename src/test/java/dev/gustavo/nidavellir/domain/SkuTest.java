package dev.gustavo.nidavellir.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Sku")
class SkuTest {

    @Test
    @DisplayName("normaliza para maiúsculas e remove espaços nas pontas")
    void normalizesValue() {
        assertThat(Sku.of("  cn-001 ").value()).isEqualTo("CN-001");
    }

    @Test
    @DisplayName("SKUs com o mesmo valor normalizado são iguais")
    void equalityByNormalizedValue() {
        assertThat(Sku.of("ax-9")).isEqualTo(Sku.of("AX-9"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("rejeita valor vazio")
    void rejectsBlank(String value) {
        assertThatThrownBy(() -> Sku.of(value))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("obrigatório");
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "ABCDEFGHIJKLMNOPQRSTU"})
    @DisplayName("rejeita tamanho fora de 3 a 20 caracteres")
    void rejectsInvalidLength(String value) {
        assertThatThrownBy(() -> Sku.of(value)).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-ABC", "ABC-", "AB--C", "AB C", "AB_C", "ÁBC"})
    @DisplayName("rejeita caracteres e hífens fora do padrão")
    void rejectsInvalidFormat(String value) {
        assertThatThrownBy(() -> Sku.of(value))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("SKU inválido");
    }
}
