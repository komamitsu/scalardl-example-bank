package org.komamitsu.bank.commands;

import picocli.CommandLine;

/**
 * Picocli doesn't allow a parent class that has an annotation including `subcommands` to be inherited by the sub command classes by throwing
 * "picocli.CommandLine$InitializationException: player-battle (org.komamitsu.bank.commands.Cli) has a subcommand (org.komamitsu.bank.commands.RegisterCert) that is a subclass of itself".
 * That's reason why we use Base class.
 */
abstract class Base {
    @CommandLine.ParentCommand Cli base;
}
