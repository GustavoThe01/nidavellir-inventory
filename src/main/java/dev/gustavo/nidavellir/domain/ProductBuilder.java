package dev.gustavo.nidavellir.domain;

/**
 * Builder de {@link Product}: deixa claro o que é cada valor no momento da criação, o que um construtor
 * com cinco parâmetros não faz. As regras continuam em {@code Product.create}: o builder só organiza a chamada.
 */
public final class ProductBuilder {

    private Sku sku;
    private String name;
    private Category category;
    private Money price;
    private Quantity minimumStock = Quantity.ZERO;

    ProductBuilder() {
    }

    public ProductBuilder sku(String value) {
        this.sku = Sku.of(value);
        return this;
    }

    public ProductBuilder name(String value) {
        this.name = value;
        return this;
    }

    public ProductBuilder category(String value) {
        this.category = Category.of(value);
        return this;
    }

    public ProductBuilder price(String value) {
        this.price = Money.of(value);
        return this;
    }

    public ProductBuilder minimumStock(int value) {
        this.minimumStock = Quantity.of(value);
        return this;
    }

    public Product build() {
        requireSet(sku, "SKU");
        requireSet(name, "nome");
        requireSet(category, "categoria");
        requireSet(price, "preço");
        return Product.create(sku, name, category, price, minimumStock);
    }

    private static void requireSet(Object value, String field) {
        if (value == null) {
            throw new InvalidValueException("Informe o campo %s do produto.".formatted(field));
        }
    }
}
