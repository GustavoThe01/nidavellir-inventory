package dev.gustavo.nidavellir.domain;

import java.util.Objects;

/**
 * Produto do estoque. É a entidade central do Nidavellir.
 *
 * <p>Invariantes garantidas pela própria classe:
 * <ul>
 *   <li>o preço é sempre maior que zero;</li>
 *   <li>o estoque nunca fica negativo;</li>
 *   <li>o nome tem entre 2 e 100 caracteres.</li>
 * </ul>
 *
 * <p>A identidade do produto é o {@link Sku}: dois objetos com o mesmo SKU são o mesmo produto,
 * mesmo que o nome ou o preço sejam diferentes.
 */
public final class Product {

    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 100;

    private final Sku sku;
    private String name;
    private Category category;
    private Money price;
    private Quantity stock;
    private Quantity minimumStock;

    private Product(Sku sku, String name, Category category, Money price, Quantity stock, Quantity minimumStock) {
        this.sku = Objects.requireNonNull(sku, "sku");
        this.name = validateName(name);
        this.category = Objects.requireNonNull(category, "category");
        this.price = validatePrice(price);
        this.stock = Objects.requireNonNull(stock, "stock");
        this.minimumStock = Objects.requireNonNull(minimumStock, "minimumStock");
    }

    /** Cadastra um produto novo, sempre com estoque zerado. A primeira entrada é uma movimentação. */
    public static Product create(Sku sku, String name, Category category, Money price, Quantity minimumStock) {
        return new Product(sku, name, category, price, Quantity.ZERO, minimumStock);
    }

    public void addStock(Quantity quantity) {
        requirePositive(quantity, "entrada");
        stock = stock.add(quantity);
    }

    public void removeStock(Quantity quantity) {
        requirePositive(quantity, "saída");
        if (stock.isLessThan(quantity)) {
            throw new InsufficientStockException(sku, stock, quantity);
        }
        stock = stock.subtract(quantity);
    }

    public void rename(String newName) {
        name = validateName(newName);
    }

    public void changeCategory(Category newCategory) {
        category = Objects.requireNonNull(newCategory, "newCategory");
    }

    public void changePrice(Money newPrice) {
        price = validatePrice(newPrice);
    }

    public void changeMinimumStock(Quantity newMinimumStock) {
        minimumStock = Objects.requireNonNull(newMinimumStock, "newMinimumStock");
    }

    public boolean isBelowMinimum() {
        return stock.isLessThan(minimumStock);
    }

    /** Valor do estoque deste produto: preço × quantidade. */
    public Money stockValue() {
        return price.multiply(stock.value());
    }

    public Sku sku() {
        return sku;
    }

    public String name() {
        return name;
    }

    public Category category() {
        return category;
    }

    public Money price() {
        return price;
    }

    public Quantity stock() {
        return stock;
    }

    public Quantity minimumStock() {
        return minimumStock;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("O nome do produto é obrigatório.");
        }
        String stripped = name.strip();
        if (stripped.length() < NAME_MIN_LENGTH || stripped.length() > NAME_MAX_LENGTH) {
            throw new InvalidValueException("O nome do produto deve ter entre %d e %d caracteres."
                    .formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH));
        }
        return stripped;
    }

    private static Money validatePrice(Money price) {
        Objects.requireNonNull(price, "price");
        if (price.isZero()) {
            throw new InvalidValueException("O preço do produto deve ser maior que zero.");
        }
        return price;
    }

    private static void requirePositive(Quantity quantity, String operation) {
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.isZero()) {
            throw new InvalidValueException("A quantidade da %s deve ser maior que zero.".formatted(operation));
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Product product && sku.equals(product.sku);
    }

    @Override
    public int hashCode() {
        return sku.hashCode();
    }

    @Override
    public String toString() {
        return "Product[%s, %s, %s, estoque=%s]".formatted(sku, name, price, stock);
    }
}
