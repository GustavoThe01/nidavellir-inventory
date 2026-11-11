# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

## [1.0.0] - 2026-11-11

Primeira versão completa: os 10 dias do roadmap.

### Adicionado
- JaCoCo 0.8.16: relatório de cobertura no `verify` e meta de 80% das linhas nos pacotes de domínio, que quebra o build se não for atingida.
- Spotless 3.10.4 com palantir-java-format e remoção de imports sem uso; o `verify` falha com código fora do padrão.
- `maven-shade-plugin`: `target/nidavellir.jar` executável, com as dependências dentro (`java -jar nidavellir.jar`).
- Diagrama de classes completo no README (domínio, portas, estratégias de preço e serviço de estoque).
- Cobertura medida: 88,0% das linhas no total, 89,8% em `domain` e 93,1% em `domain.pricing`.

### Alterado
- Versão do projeto de `0.1.0-SNAPSHOT` para `1.0.0`.
- Código formatado pelo palantir-java-format (31 arquivos), sem mudança de comportamento.

### Adicionado (Dias 01 a 09)
- Projeto Maven com Java 21, JUnit 6, AssertJ e Mockito.
- Pacotes `domain`, `application`, `infrastructure` e `cli`.
- Ponto de entrada `NidavellirApp` com teste de fumaça.
- Pipeline de CI no GitHub Actions.
- ADR 0001 — arquitetura em camadas.
- Value objects `Sku`, `Money` (com `BigDecimal`) e `Quantity`, imutáveis e validados na criação.
- Entidade `Product` com invariantes (preço maior que zero, estoque nunca negativo) e controle de estoque mínimo.
- Exceções de domínio `InvalidValueException` e `InsufficientStockException`.
- 38 testes unitários do domínio.
- Porta `ProductRepository` e implementação `InMemoryProductRepository`.
- `InventoryQueryService`: consultas com Streams por categoria, valor total em estoque, valor por categoria e produtos abaixo do mínimo.
- Value object `Category`, agora obrigatório no cadastro de `Product`.
- 10 testes novos, 5 deles usando Mockito.
- Persistência com JDBC puro e SQLite: `ConnectionFactory`, `JdbcProductRepository` e schema criado na inicialização.
- Contrato `ProductRepositoryContract`: os mesmos testes validam as implementações em memória e JDBC.
- 13 testes novos, incluindo SQL injection e preço gravado em centavos.
- Movimentações de estoque: `StockMovement` (entrada, saída e ajuste) e histórico imutável em `stock_movement`.
- `StockService` e a porta `InventoryTransaction`, com transação manual em `JdbcInventoryTransaction` (`setAutoCommit(false)`, commit e rollback).
- 8 testes de integração, incluindo o rollback do saldo quando a gravação do histórico falha.
- Strategy: `PricingPolicy` com preço cheio, `BulkDiscount`, `ClearanceDiscount` e `BestPrice`, que escolhe a política mais barata.
- Observer: `StockLevelListener`, avisado depois do commit quando o estoque cruza o mínimo.
- Builder para `Product` e `StockMovementFactory` para criar entradas, saídas e ajustes.
- 20 testes novos.
- Menu interativo com o padrão Command: cadastro, entrada, saída, ajuste, histórico, produtos abaixo do mínimo e valor por categoria.
- `Terminal`, que valida cada entrada e pergunta de novo (aceita preço com vírgula, como 19,90).
- Tratamento centralizado de exceções no `Menu`: a sessão continua depois de qualquer erro.
- `CatalogService`, que impede cadastrar um SKU que já existe, e `StockService.history`.
- `exec-maven-plugin` para rodar a CLI com `./mvnw -q compile exec:java`.
- 7 testes novos de sessões completas.
- `StockMovement` passa a ser uma `sealed interface` com os records `Inbound`, `Outbound` e `Adjustment`, no lugar do `enum`.
- `switch` com pattern matching e record patterns, sem `default`: o compilador garante que todo tipo é tratado.
- `InventoryReports`: estoque por categoria com `groupingBy`, `summarizingInt` e `teeing`, com o valor somado em `BigDecimal`.
- `ReportExporter`: CSV (RFC 4180) e JSON sem biblioteca externa, gravados num temporário e movidos para o nome final.
- Opções 9 (relatório por categoria) e 10 (exportar) no menu, e o total de entradas e saídas no histórico.
- 10 testes novos.
- Importação de produtos em CSV (opção 11 do menu): validação linha a linha pelo domínio, SKU repetido no arquivo ou já cadastrado e relatório `<arquivo>.erros.csv`.
- Validação em paralelo num `ExecutorService`, conferência de duplicados em sequência e gravação em batch JDBC numa transação só, com o estoque inicial como movimentação de entrada.
- `saveAll` e `appendAll` com `addBatch`/`executeBatch` nos repositórios JDBC.
- Logging com SLF4J 2.0 e Logback 1.6: avisos no terminal e detalhe em `logs/nidavellir.log`, com um arquivo por dia.
- 10 testes novos.

[Não lançado]: https://github.com/GustavoThe01/nidavellir-inventory/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/GustavoThe01/nidavellir-inventory/releases/tag/v1.0.0
