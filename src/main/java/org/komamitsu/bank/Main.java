package org.komamitsu.bank;

import com.scalar.db.exception.transaction.TransactionException;
import org.komamitsu.bank.commands.Cli;
import picocli.CommandLine;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, TransactionException {
        Cli cli = new Cli();
        CommandLine commandLine = new CommandLine(cli).setExecutionExceptionHandler((ex, cmdline, parseResult) -> {
            // To avoid being captured in picocli.CommandLine.handleUnhandled
            throw new Error(ex);
        });
        commandLine.execute(args);
    }
}