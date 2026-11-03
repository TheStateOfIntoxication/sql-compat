package com.sqlcompat.cli;

import picocli.CommandLine;

/**
 * Entry point. Delegates to sub-commands; {@code sql-compat check -i <path>} is the main one,
 * {@code sql-compat verify} runs the live dual-database comparison.
 */
@CommandLine.Command(
        name = "sql-compat",
        version = "0.1.0",
        mixinStandardHelpOptions = true,
        description = "Check SQL / MyBatis mappers for MySQL -> Kingbase(PostgreSQL) compatibility issues.",
        subcommands = {CheckCommand.class, VerifyCommand.class})
public final class SqlCompatCommand implements Runnable {

    @Override
    public void run() {
        // No sub-command given: print usage and exit with a non-zero code so scripts notice.
        CommandLine.usage(this, System.out);
        System.exit(2);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SqlCompatCommand()).execute(args);
        System.exit(exitCode);
    }
}
