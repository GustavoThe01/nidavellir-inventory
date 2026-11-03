# Roadmap — Nidavellir (10 dias)

## Semana 1 — Domínio e persistência

### Dia 01 — Fundação do projeto
- [x] Projeto Maven (`groupId dev.gustavo, artifactId nidavellir-inventory`), Java 21, Maven Wrapper
- [x] Pacotes `domain`, `application`, `infrastructure`, `cli`
- [x] Dependências de teste: JUnit 5, AssertJ, Mockito
- [x] GitHub Actions: `./mvnw verify`
- [x] ADR 0001 — Arquitetura em camadas e regra de dependência
- **Conceitos:** build tool, ciclo de vida do Maven, regra de dependência

### Dia 02 — Modelo de domínio
- [ ] Value objects com `record`: `Sku`, `Money` (BigDecimal!), `Quantity`
- [ ] Entidade `Product` com invariantes (preço > 0, estoque ≥ 0)
- [ ] Exceções de domínio (`InsufficientStockException`...)
- [ ] Testes unitários do domínio (TDD: teste primeiro)
- **Conceitos:** encapsulamento, imutabilidade, por que não usar `double` para dinheiro

### Dia 03 — Repositório em memória e Streams
- [ ] Interface `ProductRepository` (Inversão de Dependência)
- [ ] `InMemoryProductRepository` com `Map`
- [ ] Consultas com Streams: por categoria, valor total em estoque, abaixo do mínimo
- **Conceitos:** `List` vs `Set` vs `Map`, `equals/hashCode`, `Optional`

### Dia 04 — JDBC + SQLite
- [ ] Driver `sqlite-jdbc`, `ConnectionFactory`, criação de schema na inicialização
- [ ] `JdbcProductRepository` com `PreparedStatement` e try-with-resources
- [ ] Trocar a implementação sem alterar a camada de aplicação (prova do DIP)
- **Conceitos:** SQL injection, gerenciamento de recursos

### Dia 05 — Movimentações e transações
- [ ] `StockMovement` (entrada, saída, ajuste) e histórico
- [ ] `StockService` com transação manual (commit/rollback)
- [ ] Testes de integração com banco em arquivo temporário
- **Conceitos:** atomicidade, `setAutoCommit(false)`

## Semana 2 — Padrões, interface e qualidade

### Dia 06 — Design Patterns
- [ ] **Strategy:** políticas de precificação/desconto
- [ ] **Observer:** alerta quando o estoque fica abaixo do mínimo
- [ ] **Builder** para `Product`, **Factory** para movimentações
- **Conceitos:** aberto/fechado (OCP), quando um padrão é exagero

### Dia 07 — CLI interativa
- [ ] Menu com **Command pattern** (cada opção é um `Command`)
- [ ] Validação de entrada e mensagens de erro amigáveis
- [ ] Tratamento centralizado de exceções
- **Conceitos:** separação entre UI e regra de negócio

### Dia 08 — Java moderno e relatórios
- [ ] `sealed interface` para tipos de movimentação + `switch` com pattern matching
- [ ] Relatórios (`groupingBy`, `summarizingDouble`...)
- [ ] Exportação CSV e JSON com `java.nio.file.Files`
- **Conceitos:** tipos algébricos, exaustividade do compilador

### Dia 09 — Importação em lote e concorrência
- [ ] Importar CSV grande, validando linha a linha (relatório de erros)
- [ ] Processamento paralelo com `ExecutorService` / virtual threads
- [ ] Logging com SLF4J + Logback
- **Conceitos:** thread-safety, condições de corrida, batch insert

### Dia 10 — Qualidade e release
- [ ] JaCoCo (meta ≥ 80% no domínio) e formatação com Spotless
- [ ] Diagrama de classes (Mermaid) no README
- [ ] Jar executável (`maven-shade-plugin`) + seção "Como rodar"
- [ ] `CHANGELOG.md` + tag `v1.0.0` + GitHub Release com o jar
- **Conceitos:** empacotamento, versionamento semântico

## Ideias de evolução (opcional)
- Interface gráfica com JavaFX reutilizando as camadas `domain`/`application`
- Migrações com Flyway (ponte para o Moirai)
