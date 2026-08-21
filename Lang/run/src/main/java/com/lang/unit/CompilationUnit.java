package com.lang.unit;

import com.lang.ast.Program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompilationUnit {
    private final String filePath;
    private final String fileName;
    private Program program;
    private final Map<String, List<CompilationException>> errors;
    private final Map<String, List<WarningException>> warnings;

    public CompilationUnit(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.program = new Program();
        this.errors = new HashMap<>();
        this.warnings = new HashMap<>();
    }

    public void addError(String tag, CompilationException error) {
        errors.computeIfAbsent(tag, k -> new ArrayList<>()).add(error);
    }

    public void addWarning(String tag, WarningException warning) {
        warnings.computeIfAbsent(tag, k -> new ArrayList<>()).add(warning);
    }

    public List<CompilationException> getErrors(String tag) {
        return errors.getOrDefault(tag, new ArrayList<>());
    }

    public List<WarningException> getWarnings(String tag) {
        return warnings.getOrDefault(tag, new ArrayList<>());
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasErrors(String tag) {
        return errors.containsKey(tag) && !errors.get(tag).isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean hasWarnings(String tag) {
        return warnings.containsKey(tag) && !warnings.get(tag).isEmpty();
    }

    public int getErrorCount() {
        return errors.values().stream().mapToInt(List::size).sum();
    }

    public int getWarningCount() {
        return warnings.values().stream().mapToInt(List::size).sum();
    }

    public Map<String, List<CompilationException>> getErrors() {
        return errors;
    }

    public Map<String, List<WarningException>> getWarnings() {
        return warnings;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public void printReport() {
        System.out.println("\n=== COMPILATION REPORT ===");

        if (hasErrors()) {
            System.out.println("\nERRORS (" + getErrorCount() + "):");
            for (Map.Entry<String, List<CompilationException>> entry : errors.entrySet()) {
                String tag = entry.getKey();
                List<CompilationException> list = entry.getValue();
                System.out.printf("  [%s] %d error(s):%n", tag, list.size());
                for (CompilationException e : list) {
                    System.out.printf("      %s%n", e.getMessage());
                }
            }
        }

        if (hasWarnings()) {
            System.out.println("\nWARNINGS (" + getWarningCount() + "):");
            for (Map.Entry<String, List<WarningException>> entry : warnings.entrySet()) {
                String tag = entry.getKey();
                List<WarningException> list = entry.getValue();
                System.out.printf("  [%s] %d warning(s):%n", tag, list.size());
                for (WarningException w : list) {
                    System.out.printf("      %s%n", w.getMessage());
                }
            }
        }

        if (!hasErrors() && !hasWarnings()) {
            System.out.println("\nNo errors or warnings found!");
        } else if (!hasErrors()) {
            System.out.println("\nCompilation successful with warnings!");
        } else {
            System.out.println("\nCompilation failed with errors!");
        }
    }

    public boolean isSuccessful() {
        return !hasErrors();
    }
}