package org.komamitsu.bank.commands;

import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(
        name = "bank",
        mixinStandardHelpOptions = true,
        description = "Just a sample application for Scalar DL",
        subcommands = {
                RegisterCert.class
        }
)
public class Cli {
    @CommandLine.Option(
            names = {"-c", "--config"},
            description = "Client config file",
            required = true
    )
    protected Path path;
}
