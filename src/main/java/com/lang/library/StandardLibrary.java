package com.lang.library;

import com.lang.value.BlockValue;
import com.lang.ast.Expr;
import com.lang.ast.Program;
import com.lang.lexer.Lexer;
import com.lang.runtime.Runtime;
import com.lang.source.SourceStream;
import com.lang.unit.CompilationUnit;
import com.lang.parser.Parser;
import com.lang.value.Value;
import com.lang.value.ValueResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public final class StandardLibrary implements Library {
    private static final StandardLibrary INSTANCE = new StandardLibrary();
    private static final Map<String, BlockValue> cache = new HashMap<>();

    private static final Scanner SCANNER = new Scanner(System.in);

    public static final BlockValue INCLUDE = new BlockValue() {
        @Override
        public ValueResult call(
                Runtime rt,
                List<Expr> arguments,
                List<Expr> bindings) {

            if (arguments.size() != 1) {
                throw new RuntimeException(
                        "include(name) expects 1 argument");
            }

            ValueResult nameResult = rt.accept(arguments.get(0));

            if (nameResult.isLaunched()) {
                return nameResult;
            }

            String name = nameResult.getValue().toLString();

            File file = new File(rt.workingDir(), name);

            if (!file.isFile() && !name.endsWith(".l")) {
                file = new File(rt.workingDir(), name + ".l");
            }

            if (!file.isFile()) {
                throw new RuntimeException(
                        "Module not found: " + name);
            }

            String path;

            try {
                path = file.getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to resolve module: " + name, e);
            }

            BlockValue exports = cache.get(path);

            if (exports == null) {
                Program program = compileFile(rt, name);

                BlockValue moduleScope = new BlockValue(null, rt.peekScope());

                ValueResult result = rt.execute(program, moduleScope);

                if (!result.isLaunched()
                        || !"module-export".equals(result.getId())) {

                    throw new RuntimeException(
                            "Module did not export: " + name);
                }

                Value value = result.getValue();

                if (value == null || !value.isBlock()) {
                    throw new RuntimeException(
                            "Module export must be a block: " + name);
                }

                exports = value.asBlock();

                cache.put(path, exports);
            }

            exports.getLocals().forEach(
                    rt.peekScope()::setLocal);

            return ValueResult.of(
                    ValueResult.NORMAL,
                    null,
                    Value.ofNull());
        }
    };

    public static final BlockValue IMPORT = new BlockValue() {
        @Override
        public ValueResult call(
                Runtime rt,
                List<Expr> arguments,
                List<Expr> bindings) {

            if (arguments.size() != 1) {
                throw new RuntimeException(
                        "import(name) expects 1 argument");
            }

            ValueResult nameResult = rt.accept(arguments.get(0));

            if (nameResult.isLaunched()) {
                return nameResult;
            }

            String name = nameResult.getValue().toLString();

            File file = new File(rt.workingDir(), name);

            if (!file.isFile() && !name.endsWith(".l")) {
                file = new File(rt.workingDir(), name + ".l");
            }

            if (!file.isFile()) {
                throw new RuntimeException(
                        "Module not found: " + name);
            }

            String path;

            try {
                path = file.getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to resolve module: " + name, e);
            }

            BlockValue cached = cache.get(path);

            if (cached != null) {
                return ValueResult.of(
                        ValueResult.NORMAL,
                        null,
                        cached);
            }

            Program program = compileFile(rt, name);

            BlockValue moduleScope = new BlockValue(null, rt.peekScope());

            ValueResult result = rt.execute(program, moduleScope);

            if (result.isLaunched()
                    && "module-export".equals(result.getId())) {

                Value value = result.getValue();

                if (value == null || !value.isBlock()) {
                    throw new RuntimeException(
                            "Module export must be a block: " + name);
                }

                BlockValue exports = value.asBlock();

                cache.put(path, exports);

                return ValueResult.of(
                        ValueResult.NORMAL,
                        null,
                        exports);
            }

            throw new RuntimeException(
                    "Module did not export: " + name);
        }
    };

    public static final BlockValue EXPORT = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 1) {
                throw new RuntimeException("export(obj) expects 1 argument");
            }

            ValueResult objResult = rt.accept(arguments.get(0));
            if (objResult.isLaunched()) {
                return objResult;
            }

            Value obj = objResult.getValue();

            return ValueResult.of(ValueResult.LAUNCHED, "module-export", obj);
        }
    };

    private static final BlockValue IF = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 1) {
                throw new RuntimeException("if(condition) expects 1 argument");
            }

            if (bindings.size() != 1 && bindings.size() != 2) {
                throw new RuntimeException("if(condition) requires 1 or 2 bindings (then) or (then, else)");
            }

            ValueResult condResult = rt.accept(arguments.get(0));
            if (condResult.isLaunched()) {
                return condResult;
            }

            Value cond = condResult.getValue();
            if (!cond.isBool()) {
                throw new RuntimeException("if() condition must be boolean");
            }

            boolean condition = cond.toLBool();
            Expr chosen;

            if (bindings.size() == 1) {
                if (!condition) {
                    return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
                }
                chosen = bindings.get(0);
            } else {
                chosen = condition ? bindings.get(0) : bindings.get(1);
            }

            ValueResult blockResult = rt.accept(chosen);
            if (blockResult.isLaunched()) {
                return blockResult;
            }

            Value blockValue = blockResult.getValue();
            if (!blockValue.isBlock()) {
                throw new RuntimeException("if() then and else must evaluate to blocks");
            }

            return blockValue.asBlock().call(rt, new ArrayList<>(), new ArrayList<>());
        }
    };

    private static final BlockValue WHILE = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 1) {
                throw new RuntimeException("while(condition) expects 1 argument");
            }

            if (bindings.size() != 1) {
                throw new RuntimeException("while(condition) requires 1 attachment (body)");
            }

            Expr condition = arguments.get(0);
            Expr body = bindings.get(0);

            ValueResult result = ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());

            while (true) {
                ValueResult condResult = rt.accept(condition);

                if (condResult.isLaunched()) {
                    if ("break".equals(condResult.getId())) {
                        result = ValueResult.of(ValueResult.NORMAL, null, condResult.getValue());
                        break;
                    } else if ("continue".equals(condResult.getId())) {
                        continue;
                    } else {
                        return condResult;
                    }
                }

                Value cond = condResult.getValue();

                if (!cond.isBool()) {
                    throw new RuntimeException("while() condition must return boolean");
                }

                if (!cond.toLBool()) {
                    break;
                }

                ValueResult bodyResult = rt.accept(body);

                if (bodyResult.isLaunched()) {
                    if ("break".equals(bodyResult.getId())) {
                        result = ValueResult.of(ValueResult.NORMAL, null, bodyResult.getValue());
                        break;
                    } else if ("continue".equals(bodyResult.getId())) {
                        continue;
                    } else {
                        return bodyResult;
                    }
                }

                Value bodyValue = bodyResult.getValue();

                if (bodyValue != null && bodyValue.isBlock()) {
                    result = bodyValue.asBlock().call(rt, new ArrayList<>(), new ArrayList<>());
                } else {
                    result = ValueResult.of(ValueResult.NORMAL, null, bodyValue);
                }
            }

            return result;
        }
    };

    private static final BlockValue BREAK = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() > 1) {
                throw new RuntimeException("break() expects 0 or 1 arguments");
            }
            if (arguments.size() == 1) {
                ValueResult v = rt.accept(arguments.get(0));
                if (v.isLaunched()) {
                    return v;
                }
                return ValueResult.of(ValueResult.LAUNCHED, "break", v.getValue());
            }
            return ValueResult.of(ValueResult.LAUNCHED, "break", Value.ofNull());
        }
    };

    private static final BlockValue CONTINUE = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (!arguments.isEmpty()) {
                throw new RuntimeException("continue() expects 0 arguments");
            }
            return ValueResult.of(ValueResult.LAUNCHED, "continue", Value.ofNull());
        }
    };

    private static final BlockValue PRINTLN = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.isEmpty()) {
                System.out.println();
                return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
            }

            ValueResult v = rt.accept(arguments.get(0));
            if (v.isLaunched()) {
                return v;
            }
            System.out.println(v.getValue().toLString());

            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
        }
    };

    private static final BlockValue PRINT = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.isEmpty()) {
                throw new RuntimeException("print() expects 1 argument");
            }

            ValueResult v = rt.accept(arguments.get(0));
            if (v.isLaunched()) {
                return v;
            }
            System.out.print(v.getValue().toLString());

            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
        }
    };

    private static final BlockValue READLN = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (!arguments.isEmpty()) {
                throw new RuntimeException("readln() expects 0 arguments");
            }

            return ValueResult.of(ValueResult.NORMAL, null, Value.ofString(SCANNER.nextLine()));
        }
    };

    private static final BlockValue LAUNCH = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 2) {
                throw new RuntimeException("launch(id, value) expects 2 arguments");
            }

            ValueResult idResult = rt.accept(arguments.get(0));
            if (idResult.isLaunched()) {
                return idResult;
            }

            Value id = idResult.getValue();
            if (!id.isString()) {
                throw new RuntimeException("launch() id must be string");
            }

            ValueResult valueResult = rt.accept(arguments.get(1));
            if (valueResult.isLaunched()) {
                return valueResult;
            }

            return ValueResult.of(ValueResult.LAUNCHED, id.toLString(), valueResult.getValue());
        }
    };

    private static final BlockValue FINALIZE = new BlockValue() {
        @Override
        public ValueResult call(Runtime rt, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 2) {
                throw new RuntimeException("finalize(id, body) expects 2 arguments");
            }

            ValueResult idResult = rt.accept(arguments.get(0));
            if (idResult.isLaunched()) {
                return idResult;
            }

            Value id = idResult.getValue();
            if (!id.isString()) {
                throw new RuntimeException("finalize() id must be string");
            }

            ValueResult bodyResult = rt.accept(arguments.get(1));
            if (bodyResult.isLaunched() && bodyResult.getId().equals(id.toLString())) {
                return ValueResult.of(ValueResult.NORMAL, null, bodyResult.getValue());
            }

            return bodyResult;
        }
    };

    private StandardLibrary() {
    }

    public static StandardLibrary getInstance() {
        return INSTANCE;
    }

    @Override
    public void open(BlockValue block) {
        if (block == null) {
            throw new IllegalArgumentException("block cannot be null");
        }

        block.setLocal("include", INCLUDE);
        block.setLocal("import", IMPORT);
        block.setLocal("export", EXPORT);

        block.setLocal("if", IF);
        block.setLocal("while", WHILE);
        block.setLocal("break", BREAK);
        block.setLocal("continue", CONTINUE);

        block.setLocal("println", PRINTLN);
        block.setLocal("print", PRINT);
        block.setLocal("readln", READLN);

        block.setLocal("launch", LAUNCH);
        block.setLocal("finalize", FINALIZE);
    }

    private static Program compileFile(Runtime rt, String name) {
        File file = new File(rt.workingDir(), name);

        if (!file.isFile() && !name.endsWith(".l")) {
            file = new File(rt.workingDir(), name + ".l");
        }

        if (!file.isFile()) {
            throw new RuntimeException(
                    "Module not found: " + name);
        }

        try (FileInputStream input = new FileInputStream(file)) {
            SourceStream source = new SourceStream(input);

            CompilationUnit unit = new CompilationUnit(
                    file.getAbsolutePath(),
                    file.getName());

            Lexer lexer = new Lexer(source, unit);
            Parser parser = new Parser(lexer, unit);

            parser.parse();
            // unit.printReport();

            if (unit.hasErrors()) {
                throw new RuntimeException(
                        "Compilation failed: " + name);
            }

            return unit.getProgram();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to compile module: " + name, e);
        }
    }
}