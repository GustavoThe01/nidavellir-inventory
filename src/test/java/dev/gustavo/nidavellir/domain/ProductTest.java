package dev.gustavo.nidavellir.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Product")
class ProductTest {

    private static Product hammer() {
        return Product.create(Sku.of("MJ-001"), "Martelo de forja", Money.of("149.90"), Quantity.of(5));
    }

    @Nested
    @DisplayName("ao cadastrar")
    class Creation {

        @Test
        @DisplayName("começa com estoque zerado")
        void startsWithZeroStock() {
            assertThat(hammer().stock()).isEqualTo(Quantity.ZERO);
        }

        @Test
        @DisplayName("remove espaços nas pontas do nome")
        void stripsName() {
            Product product = Product.create(Sku.of("MJ-002"), "  Bigorna  ", Money.of("10"), Quantity.ZERO);
            assertThat(product.name()).isEqualTo("Bigorna");
        }

        @Test
        @DisplayName("rejeita preço zero")
        void rejectsZeroPrice() {
            assertThatThrownBy(() -> Product.create(Sku.of("MJ-003"), "Tenaz", Money.ZERO, Quantity.ZERO))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("maior que zero");
        }

        @Test
        @DisplayName("rejeita nome muito curto")
        void rejectsShortName() {
            assertThatThrownBy(() -> Product.create(Sku.of("MJ-004"), "X", Money.of("1"), Quantity.ZERO))
                    .isInstanceOf(InvalidValueException.class);
        }
    }

    @Nested
    @DisplayName("ao movimentar o estoque")
    class StockMovement {

        @Test
        @DisplayName("entrada aumenta o estoque")
        void addStockIncreases() {
            Product product = hammer();
            product.addStock(Quantity.of(10));
            assertThat(product.stock()).isEqualTo(Quantity.of(10));
        }

        @Test
        @DisplayName("saída diminui o estoque")
        void removeStockDecreases() {
            Product product = hammer();
            product.addStock(Quantity.of(10));
            product.removeStock(Quantity.of(4));
            assertThat(product.stock()).isEqualTo(Quantity.of(6));
        }

        @Test
        @DisplayName("saída maior que o estoque lança InsufficientStockException e não altera nada")
        void removeMoreThanAvailable() {
            Product product = hammer();
            product.addStock(Quantity.of(2));

            assertThatThrownBy(() -> product.removeStock(Quantity.of(3)))
                    .isInstanceOfSatisfying(InsufficientStockException.class, e -> {
                        assertThat(e.available()).isEqualTo(Quantity.of(2));
                        assertThat(e.requested()).isEqualTo(Quantity.of(3));
                    });
            assertThat(product.stock()).isEqualTo(Quantity.of(2));
        }

        @Test
        @DisplayName("rejeita movimentação de quantidade zero")
        void rejectsZeroQuantity() {
            Product product = hammer();
            assertThatThrownBy(() -> product.addStock(Quantity.ZERO)).isInstanceOf(InvalidValueException.class);
            assertThatThrownBy(() -> product.removeStock(Quantity.ZERO)).isInstanceOf(InvalidValueException.class);
        }
    }

    @Nested
    @DisplayName("estoque mínimo")
    class MinimumStock {

        @Test
        @DisplayName("está abaixo do mínimo quando o estoque é menor que o mínimo")
        void belowMinimum() {
            Product product = hammer();
            product.addStock(Quantity.of(4));
            assertThat(product.isBelowMinimum()).isTrue();
        }

        @Test
        @DisplayName("não está abaixo do mínimo quando o estoque é igual ao mínimo")
        void atMinimum() {
            Product product = hammer();
            product.addStock(Quantity.of(5));
            assertThat(product.isBelowMinimum()).isFalse();
        }
    }

    @Test
    @DisplayName("valor em estoque é preço × quantidade")
    void stockValue() {
        Product product = hammer();
        product.addStock(Quantity.of(3));
        assertThat(product.stockValue()).isEqualTo(Money.of("449.70"));
    }

    @Test
    @DisplayName("a identidade é o SKU")
    void identityIsSku() {
        Product a = Product.create(Sku.of("MJ-001"), "Martelo", Money.of("1"), Quantity.ZERO);
        Product b = Product.create(Sku.of("mj-001"), "Outro nome", Money.of("2"), Quantity.ZERO);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
