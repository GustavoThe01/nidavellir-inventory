package dev.gustavo.nidavellir.domain;

/** Lançada quando uma saída pediria mais unidades do que o produto tem em estoque. */
public final class InsufficientStockException extends DomainException {

    private static final long serialVersionUID = 1L;

    private final transient Sku sku;
    private final transient Quantity available;
    private final transient Quantity requested;

    public InsufficientStockException(Sku sku, Quantity available, Quantity requested) {
        super("Estoque insuficiente para %s: disponível %d, solicitado %d."
                .formatted(sku, available.value(), requested.value()));
        this.sku = sku;
        this.available = available;
        this.requested = requested;
    }

    public Sku sku() {
        return sku;
    }

    public Quantity available() {
        return available;
    }

    public Quantity requested() {
        return requested;
    }
}
