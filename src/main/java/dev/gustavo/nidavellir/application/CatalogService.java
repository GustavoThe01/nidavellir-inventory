package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.domain.InvalidValueException;
import dev.gustavo.nidavellir.domain.Product;
import java.util.Objects;

/** Cadastro de produtos. Garante que um SKU novo não sobrescreva, sem aviso, um produto que já existe. */
public final class CatalogService {

    private final ProductRepository products;

    public CatalogService(ProductRepository products) {
        this.products = Objects.requireNonNull(products, "products");
    }

    public Product register(Product product) {
        Objects.requireNonNull(product, "product");
        if (products.findBySku(product.sku()).isPresent()) {
            throw new InvalidValueException("Já existe um produto com o SKU %s.".formatted(product.sku()));
        }
        products.save(product);
        return product;
    }
}
