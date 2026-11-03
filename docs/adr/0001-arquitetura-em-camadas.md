# ADR 0001 — Arquitetura em camadas com regra de dependência

- **Status:** Aceita
- **Data:** 2026-11-03

## Contexto
O Nidavellir é uma aplicação de console sem framework. Mesmo pequena, ela vai trocar de
persistência (memória → JDBC/SQLite) e de interface (CLI, talvez JavaFX no futuro).
Precisamos que essas trocas não exijam mudar a regra de negócio.

## Decisão
Organizar o código em quatro pacotes, com dependências apontando sempre para dentro:

```
cli ──► application ──► domain
infrastructure ──► application ──► domain
```

- `domain` — entidades, value objects e regras. Não depende de nada.
- `application` — casos de uso e **interfaces** de repositório (portas).
- `infrastructure` — implementações técnicas (JDBC, arquivos CSV/JSON).
- `cli` — interação com o usuário e montagem das dependências (composition root).

## Alternativas consideradas
- **Pacote por camada técnica sem regra** (`model`, `dao`, `util`) — simples, mas acopla
  regra de negócio a JDBC e dificulta testes.
- **Módulos Maven separados** — garante a regra no compilador, porém é cerimônia demais
  para o tamanho do projeto. Pode ser revisto no futuro.

## Consequências
- ✅ Domínio testável sem banco nem console.
- ✅ Trocar `InMemoryProductRepository` por `JdbcProductRepository` não afeta os casos de uso.
- ⚠️ A regra é garantida por disciplina e revisão; um teste de arquitetura pode ser adicionado depois.
