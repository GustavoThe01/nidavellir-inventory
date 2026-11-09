package dev.gustavo.nidavellir.application;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.filtering;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.summarizingInt;
import static java.util.stream.Collectors.teeing;

import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.domain.StockMovement.Adjustment;
import dev.gustavo.nidavellir.domain.StockMovement.Inbound;
import dev.gustavo.nidavellir.domain.StockMovement.Outbound;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/** Relatórios do estoque, montados com Collectors em uma única passada pelos produtos. */
public final class InventoryReports {

    /** Uma linha do relatório por categoria. */
    public record CategoryReport(
            String category,
            long products,
            long units,
            int minimumStock,
            int maximumStock,
            double averageStock,
            Money stockValue,
            long belowMinimum) {
    }

    /** Quanto entrou, quanto saiu e quantos ajustes houve no histórico de um produto. */
    public record MovementSummary(int inboundUnits, int outboundUnits, int adjustments) {
    }

    private record ValueAndAlerts(Money value, long belowMinimum) {
    }

    private final ProductRepository products;

    public InventoryReports(ProductRepository products) {
        this.products = Objects.requireNonNull(products, "products");
    }

    /**
     * Estatísticas de estoque por categoria, em ordem alfabética.
     *
     * <p>{@code summarizingInt} calcula contagem, soma, mínimo, máximo e média de uma vez. O valor em dinheiro
     * não entra num {@code summarizingDouble}: {@code double} arredonda centavos, então ele é somado como
     * {@link Money}, com {@code BigDecimal}. O {@code teeing} junta os três coletores numa passada só.
     */
    public List<CategoryReport> byCategory() {
        return products.findAll().stream()
                .collect(groupingBy(
                        product -> product.category().name(),
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                        teeing(
                                summarizingInt(product -> product.stock().value()),
                                teeing(
                                        reducing(Money.ZERO, Product::stockValue, Money::add),
                                        filtering(Product::isBelowMinimum, counting()),
                                        ValueAndAlerts::new),
                                (stock, rest) -> report(stock, rest))))
                .entrySet().stream()
                .map(entry -> withCategory(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Resume um histórico. O {@code switch} desmonta cada variante com um record pattern e não tem
     * {@code default}: se um tipo novo de movimentação for criado, este método deixa de compilar até tratá-lo.
     */
    public static MovementSummary summarize(List<StockMovement> history) {
        int inbound = 0;
        int outbound = 0;
        int adjustments = 0;
        for (StockMovement movement : history) {
            switch (movement) {
                case Inbound(var sku, var quantity, var stockAfter, var reason, var at) -> inbound += quantity.value();
                case Outbound(var sku, var quantity, var stockAfter, var reason, var at) -> outbound += quantity.value();
                case Adjustment adjustment -> adjustments++;
            }
        }
        return new MovementSummary(inbound, outbound, adjustments);
    }

    private static CategoryReport report(IntSummaryStatistics stock, ValueAndAlerts rest) {
        return new CategoryReport("", stock.getCount(), stock.getSum(), stock.getMin(), stock.getMax(),
                stock.getAverage(), rest.value(), rest.belowMinimum());
    }

    private static CategoryReport withCategory(String category, CategoryReport report) {
        return new CategoryReport(category, report.products(), report.units(), report.minimumStock(),
                report.maximumStock(), report.averageStock(), report.stockValue(), report.belowMinimum());
    }
}
