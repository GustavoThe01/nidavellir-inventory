package dev.gustavo.nidavellir.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gustavo.nidavellir.domain.Category;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato da porta {@link ProductRepository}: os mesmos testes rodam em todas as implementações.
 * É a prova de que trocar uma pela outra não muda o comportamento visto pela aplicação.
 */
public abstract class ProductRepositoryContract {

    protected abstract ProductRepository repository();

    protected static Product product(String sku, String name) {
        return Product.create(Sku.of(sku), name, Category.of("Forja"), Money.of("10"), Quantity.ZERO);
    }

    @Test
    @DisplayName("encontra pelo SKU, mesmo digitado em minúsculas")
    void findsBySku() {
        repository().save(product("MJ-001", "Martelo"));
        assertThat(repository().findBySku(Sku.of("mj-001"))).isPresent();
    }

    @Test
    @DisplayName("devolve vazio quando o SKU não existe")
    void returnsEmptyWhenMissing() {
        assertThat(repository().findBySku(Sku.of("NADA-1"))).isEmpty();
    }

    @Test
    @DisplayName("guarda e devolve todos os campos")
    void keepsAllFields() {
        Product original = Product.create(
                Sku.of("BG-001"), "Bigorna", Category.of("Forja"), Money.of("1249.90"), Quantity.of(3));
        original.addStock(Quantity.of(7));

        repository().save(original);

        Product found = repository().findBySku(Sku.of("BG-001")).orElseThrow();
        assertThat(found.name()).isEqualTo("Bigorna");
        assertThat(found.category()).isEqualTo(Category.of("Forja"));
        assertThat(found.price()).isEqualTo(Money.of("1249.90"));
        assertThat(found.stock()).isEqualTo(Quantity.of(7));
        assertThat(found.minimumStock()).isEqualTo(Quantity.of(3));
    }

    @Test
    @DisplayName("salvar o mesmo SKU atualiza em vez de duplicar")
    void saveReplaces() {
        repository().save(product("MJ-001", "Martelo"));
        repository().save(product("MJ-001", "Martelo novo"));

        assertThat(repository().findAll()).singleElement()
                .satisfies(found -> assertThat(found.name()).isEqualTo("Martelo novo"));
    }

    @Test
    @DisplayName("lista em ordem de SKU")
    void listsSorted() {
        repository().save(product("ZZ-001", "Zarabatana"));
        repository().save(product("AA-001", "Anel"));

        assertThat(repository().findAll()).extracting(found -> found.sku().value())
                .containsExactly("AA-001", "ZZ-001");
    }

    @Test
    @DisplayName("informa se removeu de fato")
    void deleteReportsResult() {
        repository().save(product("MJ-001", "Martelo"));

        assertThat(repository().deleteBySku(Sku.of("MJ-001"))).isTrue();
        assertThat(repository().deleteBySku(Sku.of("MJ-001"))).isFalse();
    }
}
