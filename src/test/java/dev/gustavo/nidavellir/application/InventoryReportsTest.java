package dev.gustavo.nidavellir.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gustavo.nidavellir.application.InventoryReports.CategoryReport;
import dev.gustavo.nidavellir.application.InventoryReports.MovementSummary;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement.Adjustment;
import dev.gustavo.nidavellir.domain.StockMovement.Inbound;
import dev.gustavo.nidavellir.domain.StockMovement.Outbound;
import dev.gustavo.nidavellir.infrastructure.InMemoryProductRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Relatórios do estoque")
class InventoryReportsTest {

    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final InventoryReports reports = new InventoryReports(products);

    private void product(String sku, String category, String price, int minimum, int stock) {
        Product product = Product.builder()
                .sku(sku)
                .name("Item " + sku)
                .category(category)
                .price(price)
                .minimumStock(minimum)
                .build();
        if (stock > 0) {
            product.addStock(Quantity.of(stock));
        }
        products.save(product);
    }

    @Test
    @DisplayName("agrupa por categoria com contagem, unidades, mínimo, máximo, média, valor e alertas")
    void byCategory() {
        product("MJ-001", "Forja", "10.00", 5, 2);
        product("MJ-002", "forja", "0.10", 1, 9);
        product("RN-001", "Runas", "33.33", 0, 3);

        List<CategoryReport> lines = reports.byCategory();

        assertThat(lines).extracting(CategoryReport::category).containsExactly("Forja", "Runas");
        CategoryReport forge = lines.getFirst();
        assertThat(forge.products()).isEqualTo(2);
        assertThat(forge.units()).isEqualTo(11);
        assertThat(forge.minimumStock()).isEqualTo(2);
        assertThat(forge.maximumStock()).isEqualTo(9);
        assertThat(forge.averageStock()).isEqualTo(5.5);
        assertThat(forge.stockValue()).isEqualTo(Money.of("20.90"));
        assertThat(forge.belowMinimum()).isEqualTo(1);
        assertThat(lines.get(1).stockValue()).isEqualTo(Money.of("99.99"));
    }

    @Test
    @DisplayName("sem produtos, o relatório é vazio")
    void empty() {
        assertThat(reports.byCategory()).isEmpty();
    }

    @Test
    @DisplayName("o resumo do histórico soma entradas e saídas e conta os ajustes")
    void summarize() {
        Sku sku = Sku.of("MJ-001");
        Instant now = Instant.parse("2026-11-09T12:00:00Z");

        MovementSummary summary = InventoryReports.summarize(List.of(
                new Inbound(sku, Quantity.of(10), Quantity.of(10), "Compra", now),
                new Outbound(sku, Quantity.of(4), Quantity.of(6), "Venda", now),
                new Adjustment(sku, Quantity.of(5), Quantity.of(5), "Contagem", now),
                new Inbound(sku, Quantity.of(2), Quantity.of(7), "Compra", now)));

        assertThat(summary).isEqualTo(new MovementSummary(12, 4, 1));
    }
}
