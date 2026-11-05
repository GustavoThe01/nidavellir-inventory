package dev.gustavo.nidavellir.infrastructure;

import dev.gustavo.nidavellir.application.ProductRepository;
import dev.gustavo.nidavellir.application.ProductRepositoryContract;
import org.junit.jupiter.api.DisplayName;

@DisplayName("InMemoryProductRepository")
class InMemoryProductRepositoryTest extends ProductRepositoryContract {

    private final InMemoryProductRepository repository = new InMemoryProductRepository();

    @Override
    protected ProductRepository repository() {
        return repository;
    }
}
