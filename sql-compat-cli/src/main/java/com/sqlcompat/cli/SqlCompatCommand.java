package com.sqlcompat.cli;

import picocli.CommandLine;

/**
 * 程序入口。cmd 交给各子命令；{@code sql-compat check -i <path>} 是主命令，
 * {@code sql-compat verify} 执行活库双库对比。
 */
@CommandLine.Command(
        name = "sql-compat",
        version = "0.1.0",
        mixinStandardHelpOptions = true,
        description = "检查 SQL / MyBatis Mapper 在 MySQL → Kingbase(PostgreSQL) 迁移中的兼容性问题。",
        subcommands = {CheckCommand.class, VerifyCommand.class})
public final class SqlCompatCommand implements Runnable {

    @Override
    public void run() {
        // 没给子命令时打印用法并以非 0 退出，方便脚本发现问题。
        CommandLine.usage(this, System.out);
        System.exit(2);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SqlCompatCommand()).execute(args);
        System.exit(exitCode);
    }
}
