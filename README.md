# Nidavellir · inventory-cli

> *Nidavellir, o reino dos anões ferreiros, onde cada item forjado é contado e guardado.*

Sistema de **controle de estoque** em linha de comando, escrito em **Java 21 puro**
(sem frameworks), com persistência em **SQLite via JDBC**.

O objetivo é mostrar domínio sólido da linguagem e de orientação a objetos
**antes** de usar frameworks como Spring.

## Competências demonstradas
- POO: encapsulamento, invariantes, composição vs. herança
- **SOLID** e arquitetura em camadas (domain / application / infrastructure / cli)
- **Design Patterns:** Repository, Strategy, Factory, Builder, Observer, Command
- Java moderno: `record`, `sealed interface`, pattern matching em `switch`, `Optional`, virtual threads
- **Collections e Streams** (`groupingBy`, `partitioningBy`, `Collectors`)
- **JDBC**: `PreparedStatement`, try-with-resources, transações (commit/rollback)
- I/O com `java.nio` (importação/exportação CSV e JSON)
- Testes com **JUnit 5, AssertJ e Mockito**; cobertura com **JaCoCo**
- Build com **Maven**, CI com GitHub Actions

## Arquitetura planejada
```
src/main/java/dev/gustavo/nidavellir/
├── domain/          # Product, Sku, Money, StockMovement, regras e exceções
├── application/     # serviços / casos de uso, interfaces de repositório
├── infrastructure/  # JDBC, SQLite, CSV/JSON
└── cli/             # menu interativo (Command pattern)
```

## Como rodar
_A preencher no Dia 10 (jar executável)._

## Status
Veja o [ROADMAP.md](ROADMAP.md).
