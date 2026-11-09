# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

### Adicionado
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
