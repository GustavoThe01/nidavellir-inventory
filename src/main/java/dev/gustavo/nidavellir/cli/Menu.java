package dev.gustavo.nidavellir.cli;

import dev.gustavo.nidavellir.cli.Terminal.InputClosedException;
import dev.gustavo.nidavellir.domain.DomainException;
import dev.gustavo.nidavellir.infrastructure.PersistenceException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * O laço do menu e o tratamento centralizado de erros.
 *
 * <p>Os comandos não têm {@code try/catch}: tudo o que eles lançam chega aqui e vira uma mensagem.
 * Regra de negócio violada é culpa da entrada e aparece como está; falha de banco ou erro inesperado
 * aparece de forma genérica, e o detalhe vai para o log de erros, sem derrubar a sessão.
 */
public final class Menu {

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Terminal terminal;
    private final Consumer<Exception> errorLog;

    public Menu(Terminal terminal, Consumer<Exception> errorLog) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.errorLog = Objects.requireNonNull(errorLog, "errorLog");
    }

    public Menu register(String key, Command command) {
        if (commands.putIfAbsent(key, Objects.requireNonNull(command, "command")) != null) {
            throw new IllegalArgumentException("A opção %s já está em uso.".formatted(key));
        }
        return this;
    }

    public void run() {
        while (true) {
            printOptions();
            String choice = terminal.readLine("Escolha uma opção: ");
            if (choice == null) {
                return;
            }
            Command command = commands.get(choice);
            if (command == null) {
                terminal.println("Opção inválida: digite um dos números do menu.");
                continue;
            }
            if (!execute(command) || command.exits()) {
                return;
            }
        }
    }

    /** @return {@code false} quando a entrada acabou e o menu deve parar */
    private boolean execute(Command command) {
        terminal.println();
        terminal.println("== " + command.description() + " ==");
        try {
            command.execute(terminal);
            return true;
        } catch (InputClosedException exception) {
            return false;
        } catch (DomainException exception) {
            terminal.println("✗ " + exception.getMessage());
        } catch (PersistenceException exception) {
            errorLog.accept(exception);
            terminal.println("✗ Não consegui ler ou gravar os dados. Nada foi alterado; tente de novo.");
        } catch (RuntimeException exception) {
            errorLog.accept(exception);
            terminal.println("✗ Erro inesperado. A operação foi cancelada.");
        }
        return true;
    }

    private void printOptions() {
        terminal.println();
        commands.forEach((key, command) -> terminal.println("  [%s] %s".formatted(key, command.description())));
    }
}
