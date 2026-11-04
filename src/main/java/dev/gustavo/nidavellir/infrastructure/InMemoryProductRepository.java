package dev.gustavo.nidavellir.infrastructure;

import dev.gustavo.nidavellir.application.ProductRepository;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Sku;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositório em memória, usado nos testes e como primeira implementação da porta.
 *
 * <p>Uso {@code Map} porque a busca principal é pela chave (SKU), em tempo constante. Só funciona
 * porque {@link Sku} é um record, com {@code equals} e {@code hashCode} por valor.
 */
public final class InMemoryProductRepository implements ProductRepository {

    private final Map<Sku, Product> products = new HashMap<>();

    @Override
    public void save(Product product) {
        Objects.requireNonNull(product, "product");
        products.put(product.sku(), product);
    }

    @Override
    public Optional<Product> findBySku(Sku sku) {
        return Optional.ofNullable(products.get(sku));
    }

    @Override
    public List<Product> findAll() {
        return products.values().stream()
                .sorted(Comparator.comparing(product -> product.sku().value()))
                .toList();
    }

    @Override
    public boolean deleteBySku(Sku sku) {
        return products.remove(sku) != null;
    }
}
