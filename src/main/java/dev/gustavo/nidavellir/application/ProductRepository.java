package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Sku;
import java.util.List;
import java.util.Optional;

/**
 * Porta de persistência de produtos.
 *
 * <p>A interface fica na camada de aplicação, e não na infraestrutura: quem define o contrato é
 * quem precisa dele (Inversão de Dependência). Hoje a implementação é em memória; no Dia 04 entra
 * a versão JDBC sem que nada nesta camada mude.
 */
public interface ProductRepository {

    /** Inclui o produto ou atualiza o existente com o mesmo SKU. */
    void save(Product product);

    Optional<Product> findBySku(Sku sku);

    List<Product> findAll();

    /** @return {@code true} se o produto existia e foi removido */
    boolean deleteBySku(Sku sku);
}
