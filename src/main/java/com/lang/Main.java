package com.lang;

import com.lang.evaluator.Evaluator;
import com.lang.lexer.Lexer;
import com.lang.library.StandardLibrary;
import com.lang.parser.Parser;
import com.lang.source.SourceStream;
import com.lang.tools.Printer;
import com.lang.unit.CompilationUnit;
import com.lang.value.BlockValue;
import com.lang.value.Value;
import com.lang.value.ValueResult;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Printer printer = new Printer();
    private static boolean verbose = true;
    private static String workingDir;
    private static String entryPoint;
    private static List<String> args = new ArrayList<>();

    private static final Scanner SCANNER = new Scanner(System.in);

    private static String baseDir;

    public static void main(String[] args) {

        baseDir = System.getProperty("user.dir");
        // Se recebeu argumentos de linha de comando, roda apenas uma vez
        if (args.length > 0) {
            runSingleExecution(args);
            return;
        }

        // Se rodou sem argumentos (ex: pelo Debug da IDE), entra no loop CLI
        runCliLoop();
    }

    private static void runSingleExecution(String[] args) {
        if (!parseArguments(args)) {
            printUsage();
            System.exit(1);
        }

        try {
            run();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static void runCliLoop() {
        String lastCommandLine = "";

        while (true) {
            System.out.print("lang> ");
            String input = SCANNER.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                break;
            }

            // Enter pressionado sem digitar nada reexecuta a última instrução
            if (input.isEmpty()) {
                if (lastCommandLine.isEmpty()) {
                    continue;
                }
                input = lastCommandLine;
                System.out.println("Reexecutando: " + input);
            } else {
                lastCommandLine = input;
            }

            resetState();

            String[] cliArgs = input.split("\\s+");
            if (!parseArguments(cliArgs)) {
                printUsage();
                continue;
            }

            try {
                run();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
            }

            System.out.println();
        }
    }

    private static void resetState() {
        verbose = true;
        workingDir = null;
        entryPoint = null;
        args.clear();
    }

    private static boolean parseArguments(String[] args) {
        if (args.length == 0) {
            return false;
        }

        int i = 0;
        while (i < args.length) {
            String arg = args[i];

            if (arg.equals("--help") || arg.equals("-h")) {
                return false;
            }

            if (arg.equals("--verbose") || arg.equals("-v")) {
                verbose = true;
                i++;
                continue;
            }

            if (arg.equals("--working-dir") || arg.equals("-w")) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --working-dir requires a path");
                    return false;
                }
                workingDir = baseDir + "/" + args[i + 1];
                i += 2;
                continue;
            }

            if (arg.equals("--entry") || arg.equals("-e")) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: --entry requires a file path");
                    return false;
                }
                entryPoint = args[i + 1];
                i += 2;
                continue;
            }

            if (!arg.startsWith("-") && entryPoint == null) {
                entryPoint = arg;
                i++;
                continue;
            }

            if (entryPoint != null) {
                Main.args.add(arg);
                i++;
                continue;
            }

            i++;
        }

        if (entryPoint == null) {
            System.err.println("Error: Entry point not specified");
            return false;
        }

        if (workingDir == null) {
            workingDir = System.getProperty("user.dir");
        }

        return true;
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("Lang - Programming Language");
        System.out.println();
        System.out.println("Usage: lang [options] <entry-point> [args...]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -w, --working-dir <path>   Set working directory");
        System.out.println("  -e, --entry <file>        Entry point file");
        System.out.println("  -v, --verbose             Enable verbose output");
        System.out.println("  -h, --help                Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  main.lang");
        System.out.println("  -w ./src -e app.l");
        System.out.println("  main.l arg1 arg2");
        System.out.println();
    }

    private static void run() throws Exception {
        System.out.println("Lang v1.0");
        System.out.println("Working directory: " + workingDir);
        System.out.println("Entry point: " + entryPoint);
        System.out.println();

        File workingDirFile = new File(workingDir);
        if (!workingDirFile.exists() || !workingDirFile.isDirectory()) {
            throw new RuntimeException("Working directory not found: " + workingDir);
        }

        File entryFile = resolveEntryPoint(entryPoint);
        if (entryFile == null) {
            throw new RuntimeException("Entry point not found: " + entryPoint);
        }

        System.out.println("Loading: " + entryFile.getAbsolutePath());
        System.out.println();

        CompilationUnit unit = compileFile(entryFile);
        if (unit.hasErrors()) {
            printErrors(unit);
            throw new RuntimeException("Compilation failed");
        }

        if (verbose) {
            System.out.println("AST:");
            System.out.println(printer.print(unit.getProgram()));
            System.out.println();
        }

        Evaluator evaluator = new Evaluator(workingDir);
        BlockValue entry = new BlockValue();

        BlockValue argsBlock = new BlockValue();
        for (int i = 0; i < Main.args.size(); i++) {
            argsBlock.set("arg" + String.valueOf(i), Value.ofString(Main.args.get(i)));
        }
        argsBlock.set("length", Value.ofInt(Main.args.size()));
        entry.set("args", argsBlock);

        entry.set("__filepath", Value.ofString(entryFile.getAbsolutePath()));
        StandardLibrary.getInstance().open(entry);

        System.out.println("--- Execution ---");
        ValueResult result = evaluator.execute(unit.getProgram(), entry);

        if (result.isLaunched()) {
            System.out.println(
                    "→ Launch: " + result.getId() + " = " + result.getValue().valueToString());
        } else if (result.getValue() != null && !result.getValue().isNull()) {
            System.out.println("→ " + result.getValue().valueToString());
        }

        System.out.println();
        System.out.println("Finished.");
    }

    private static File resolveEntryPoint(String entryPoint) {
        File file = new File(entryPoint);
        if (file.exists() && file.isFile()) {
            return file;
        }

        file = new File(workingDir, entryPoint);
        if (file.exists() && file.isFile()) {
            return file;
        }

        if (!entryPoint.endsWith(".l")) {
            file = new File(workingDir, entryPoint + ".l");
            if (file.exists() && file.isFile()) {
                return file;
            }
        }

        return null;
    }

    private static CompilationUnit compileFile(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file)) {
            SourceStream source = new SourceStream(input);
            CompilationUnit unit = new CompilationUnit(file.getAbsolutePath(), file.getName());
            Lexer lexer = new Lexer(source, unit);
            Parser parser = new Parser(lexer, unit);
            parser.parse();
            return unit;
        }
    }

    private static void printErrors(CompilationUnit unit) {
        System.out.println("\n=== Compilation Errors ===");
        for (var entry : unit.getErrors().entrySet()) {
            System.out.printf("  [%s] %d error(s):%n", entry.getKey(), entry.getValue().size());
            for (var error : entry.getValue()) {
                System.out.printf("      %s%n", error.getMessage());
            }
        }
    }
}