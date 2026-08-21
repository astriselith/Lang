// com/lang/module/ModuleLoader.java
package com.lang.module;

import com.lang.evaluator.Evaluator;
import com.lang.lexer.Lexer;
import com.lang.library.StandardLibrary;
import com.lang.parser.Parser;
import com.lang.source.SourceStream;
import com.lang.unit.CompilationUnit;
import com.lang.value.BlockValue;
import com.lang.value.Value;
import com.lang.value.ValueResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ModuleLoader {
    private static ModuleLoader instance;

    private final Map<String, Module> cache = new HashMap<>();
    private final List<String> paths = new ArrayList<>();
    private final String workingDir;

    private ModuleLoader() {
        this(System.getProperty("user.dir"));
    }

    private ModuleLoader(String workingDir) {
        this.workingDir = workingDir;
        paths.add(workingDir);
        paths.add(workingDir + "/modules");
        paths.add(workingDir + "/lib");
        paths.add(workingDir + "/.lang/modules");
    }

    public static ModuleLoader getInstance() {
        if (instance == null) {
            instance = new ModuleLoader();
        }
        return instance;
    }

    public static ModuleLoader newInstance(String workingDir) {
        instance = new ModuleLoader(workingDir);
        return instance;
    }

    public static ModuleLoader newInstance() {
        instance = new ModuleLoader();
        return instance;
    }

    public BlockValue load(String name) throws Exception {
        if (cache.containsKey(name)) {
            Module cached = cache.get(name);
            if (cached.isLoaded()) {
                return cached.getExports();
            }
        }

        File file = findFile(name);
        if (file == null) {
            throw new RuntimeException("Module not found: " + name +
                    "\nPaths searched: " + paths);
        }

        CompilationUnit unit = compile(file);
        if (unit.hasErrors()) {
            unit.printReport();
            throw new RuntimeException("Compilation failed: " + name);
        }

        Evaluator evaluator = new Evaluator();

        Module module = new Module(name, file.getAbsolutePath(), unit.getProgram());
        cache.put(name, module);

        BlockValue scope = new BlockValue();

        StandardLibrary.getInstance().open(scope);

        scope.set("__filepath", Value.ofString(module.getPath()));

        ValueResult result = evaluator.evaluate(module.getProgram(), scope);

        if (result.isLaunched() && result.getId().equals("module")) {
            Value exports = result.getValue();
            if (exports != null && exports.isBlock()) {
                module.setExports(exports.asBlock());
            }
        }

        module.setLoaded(true);
        return module.getExports();
    }

    private File findFile(String name) {
        File file = new File(name);
        if (file.exists()) {
            return file;
        }

        if (name.startsWith("./") || name.startsWith("../")) {
            File relative = new File(workingDir, name);
            if (relative.exists()) {
                return relative;
            }
        }

        for (String path : paths) {
            File candidate = new File(path, name + ".l");
            if (candidate.exists()) {
                return candidate;
            }

            candidate = new File(path, name);
            if (candidate.exists()) {
                return candidate;
            }
        }

        return null;
    }

    private CompilationUnit compile(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            SourceStream source = new SourceStream(input);
            CompilationUnit unit = new CompilationUnit(file.getAbsolutePath(), file.getName());
            Lexer lexer = new Lexer(source, unit);
            Parser parser = new Parser(lexer, unit);
            parser.parse();
            return unit;
        }
    }

    public void addPath(String path) {
        if (!paths.contains(path)) {
            paths.add(path);
        }
    }

    public void removePath(String path) {
        paths.remove(path);
    }

    public List<String> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void clearCache() {
        cache.clear();
    }

    public boolean isLoaded(String name) {
        Module module = cache.get(name);
        return module != null && module.isLoaded();
    }

    public Map<String, Module> getCache() {
        return cache;
    }

    public static void resetInstance() {
        instance = null;
    }

    @Override
    public String toString() {
        return "ModuleLoader{" +
                "workingDir='" + workingDir + '\'' +
                ", cache=" + cache.size() +
                ", paths=" + paths +
                '}';
    }
}