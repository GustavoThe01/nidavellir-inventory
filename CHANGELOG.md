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
