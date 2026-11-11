package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.domain.Category;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Consultas sobre o estoque, escritas com Streams sobre a porta {@link ProductRepository}. */
public final class InventoryQueryService {

    private final ProductRepository products;

    public InventoryQueryService(ProductRepository products) {
        this.products = Objects.requireNonNull(products, "products");
    }

    public List<Product> findByCategory(Category category) {
        return products.findAll().stream()
                .filter(product -> product.category().equals(category))
                .toList();
    }

    /** Valor de todo o estoque: soma de preço × quantidade de cada produto. */
    public Money totalStockValue() {
        return products.findAll().stream().map(Product::stockValue).reduce(Money.ZERO, Money::add);
    }

    /** Produtos a repor, começando pelos que têm o maior déficit em relação ao mínimo. */
    public List<Product> findBelowMinimum() {
        return products.findAll().stream()
                .filter(Product::isBelowMinimum)
                .sorted(Comparator.comparingInt(InventoryQueryService::deficit).reversed())
                .toList();
    }

    /** Valor em estoque por categoria, em ordem alfabética. */
    public Map<String, Money> stockValueByCategory() {
        return products.findAll().stream()
                .collect(Collectors.toMap(
                        product -> product.category().name(),
                        Product::stockValue,
                        Money::add,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
    }

    private static int deficit(Product product) {
        return product.minimumStock().value() - product.stock().value();
    }
}
