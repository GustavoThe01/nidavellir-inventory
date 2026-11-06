package dev.gustavo.nidavellir.domain;

/** Lançada quando uma operação pede um produto que não existe. */
public final class ProductNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(Sku sku) {
        super("Produto %s não encontrado.".formatted(sku));
    }
}
