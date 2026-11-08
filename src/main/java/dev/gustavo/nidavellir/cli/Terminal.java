package dev.gustavo.nidavellir.cli;

import dev.gustavo.nidavellir.domain.DomainException;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Sku;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Entrada e saída da CLI. Toda leitura valida o que foi digitado e pergunta de novo até receber um valor aceito,
 * para que os comandos só lidem com valores já convertidos.
 *
 * <p>Recebe um {@link BufferedReader} e um {@link PrintStream}, e não {@code System.in}/{@code System.out}
 * direto: assim os testes simulam uma sessão inteira com texto.
 */
public final class Terminal {

    /** O usuário fechou a entrada (Ctrl+Z / Ctrl+D) no meio de uma pergunta. */
    static final class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        InputClosedException() {
            super("Entrada encerrada.");
        }
    }

    private final BufferedReader in;
    private final PrintStream out;

    public Terminal(BufferedReader in, PrintStream out) {
        this.in = Objects.requireNonNull(in, "in");
        this.out = Objects.requireNonNull(out, "out");
    }

    public void println(String text) {
        out.println(text);
    }

    public void println() {
        out.println();
    }

    /** Lê uma linha. Devolve {@code null} quando a entrada termina. */
    String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            String line = in.readLine();
            return line == null ? null : line.strip();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public String text(String prompt) {
        return ask(prompt, value -> {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Este campo é obrigatório.");
            }
            return value;
        });
    }

    public Sku sku(String prompt) {
        return ask(prompt, Sku::of);
    }

    public int integer(String prompt, int minimum) {
        return ask(prompt, value -> {
            int number;
            try {
                number = Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Digite um número inteiro.", exception);
            }
            if (number < minimum) {
                throw new IllegalArgumentException("O valor mínimo é %d.".formatted(minimum));
            }
            return number;
        });
    }

    /** Aceita vírgula ou ponto como separador decimal: "19,90" e "19.90" são o mesmo preço. */
    public Money money(String prompt) {
        return ask(prompt, value -> Money.of(value.replace("R$", "").strip().replace(',', '.')));
    }

    /**
     * Pergunta até a conversão dar certo. Erros de digitação ({@link IllegalArgumentException}) e de regra
     * do domínio ({@link DomainException}) viram uma mensagem e uma nova pergunta.
     */
    public <T> T ask(String prompt, Function<String, T> parser) {
        while (true) {
            String line = readLine(prompt);
            if (line == null) {
                throw new InputClosedException();
            }
            try {
                return parser.apply(line);
            } catch (IllegalArgumentException | DomainException exception) {
                out.println("  ✗ " + exception.getMessage());
            }
        }
    }
}
