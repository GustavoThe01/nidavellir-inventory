package dev.gustavo.nidavellir.cli;

/**
 * Command: cada opção do menu é um objeto com uma única ação.
 *
 * <p>O menu não sabe o que cada opção faz. Uma opção nova é uma classe nova registrada no menu,
 * sem {@code switch} crescendo e sem mexer nas opções que já existem.
 */
public interface Command {

    String description();

    void execute(Terminal terminal);

    /** Só o comando de sair encerra o menu. */
    default boolean exits() {
        return false;
    }
}
