package com.lang.library;

import com.lang.value.BlockValue;
import com.lang.value.CallableValue;
import com.lang.ast.Expr;
import com.lang.ast.Program;
import com.lang.execution.Execution;
import com.lang.lexer.Lexer;
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

    public static final CallableValue INCLUDE_VAL = (rt, arguments, bindings) -> {

            if (arguments.size() != 1) {
                throw new RuntimeException("include(name) expects 1 argument");
            }

            ValueResult nameResult = rt.execute(arguments.get(0));

            if (nameResult.isLaunched()) {
                return nameResult;
            }

            String name = nameResult.getValue().valueToString();

            File file = new File(rt.workingDir(), name);

            if (!file.isFile() && !name.endsWith(".l")) {
                file = new File(rt.workingDir(), name + ".l");
            }

            if (!file.isFile()) {
                throw new RuntimeException("Module not found: " + name);
            }

            String path;

            try {
                path = file.getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException("Failed to resolve module: " + name, e);
            }

            BlockValue exports = cache.get(path);

            if (exports == null) {
                Program program = compileFile(rt, name);

                BlockValue moduleScope = new BlockValue(null, rt.peekScope());

                ValueResult result = rt.execute(program, moduleScope);

                if (!result.isLaunched() || !"module-export".equals(result.getId())) {

                    throw new RuntimeException("Module did not export: " + name);
                }

                Value value = result.getValue();

                if (value == null || !value.isBlock()) {
                    throw new RuntimeException("Module export must be a block: " + name);
                }

                exports = value.asBlock();

                cache.put(path, exports);
            }

            exports.getLocals().forEach(rt.peekScope()::setLocal);

            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
    };

    public static final CallableValue IMPORT_VAL = (rt, arguments, bindings) -> {

            if (arguments.size() != 1) {
                throw new RuntimeException("import(name) expects 1 argument");
            }

            ValueResult nameResult = rt.execute(arguments.get(0));

            if (nameResult.isLaunched()) {
                return nameResult;
            }

            String name = nameResult.getValue().valueToString();

            File file = new File(rt.workingDir(), name);

            if (!file.isFile() && !name.endsWith(".l")) {
                file = new File(rt.workingDir(), name + ".l");
            }

            if (!file.isFile()) {
                throw new RuntimeException("Module not found: " + name);
            }

            String path;

            try {
                path = file.getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException("Failed to resolve module: " + name, e);
            }

            BlockValue cached = cache.get(path);

            if (cached != null) {
                return ValueResult.of(ValueResult.NORMAL, null, cached);
            }

            Program program = compileFile(rt, name);

            BlockValue moduleScope = new BlockValue(null, rt.peekScope());

            ValueResult result = rt.execute(program, moduleScope);

            if (result.isLaunched() && "module-export".equals(result.getId())) {

                Value value = result.getValue();

                if (value == null || !value.isBlock()) {
                    throw new RuntimeException("Module export must be a block: " + name);
                }

                BlockValue exports = value.asBlock();

                cache.put(path, exports);

                return ValueResult.of(ValueResult.NORMAL, null, exports);
            }

            throw new RuntimeException("Module did not export: " + name);
    };

    public static final CallableValue EXPORT_VAL = (rt, arguments, bindings) -> {
            if (arguments.size() != 1) {
                throw new RuntimeException("export(obj) expects 1 argument");
            }

            ValueResult objResult = rt.execute(arguments.get(0));
            if (objResult.isLaunched()) {
                return objResult;
            }

            Value obj = objResult.getValue();

            return ValueResult.of(ValueResult.LAUNCHED, "module-export", obj);
    };

    private static final CallableValue IF_VAL = (rt, arguments, bindings) -> {
            if (arguments.size() != 1) {
                throw new RuntimeException("if(condition) expects 1 argument");
            }

            if (bindings.size() != 1 && bindings.size() != 2) {
                throw new RuntimeException("if(condition) requires 1 or 2 bindings (then) or (then, else)");
            }

            ValueResult condResult = rt.execute(arguments.get(0));
            if (condResult.isLaunched()) {
                return condResult;
            }

            Value cond = condResult.getValue();
            if (!cond.isBool()) {
                throw new RuntimeException("if() condition must be boolean");
            }

            boolean condition = cond.asBool().getValue();
            Expr chosen;

            if (bindings.size() == 1) {
                if (!condition) {
                    return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
                }
                chosen = bindings.get(0);
            } else {
                chosen = condition ? bindings.get(0) : bindings.get(1);
            }

            ValueResult blockResult = rt.execute(chosen);
            if (blockResult.isLaunched()) {
                return blockResult;
            }

            Value blockValue = blockResult.getValue();
            if (!blockValue.isCallable()) {
                throw new RuntimeException("if() then and else must evaluate to callable values");
            }

            return blockValue.asCallable().call(rt, new ArrayList<>(), new ArrayList<>());
    };

    private static final CallableValue WHILE_VAL = (rt, arguments, bindings) -> {
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
                ValueResult condResult = rt.execute(condition);

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

                if (!cond.asBool().getValue()) {
                    break;
                }

                ValueResult bodyResult = rt.execute(body);

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

                if (bodyValue != null && bodyValue.isCallable()) {
                    result = bodyValue.asCallable().call(rt, new ArrayList<>(), new ArrayList<>());
                } else {
                    result = ValueResult.of(ValueResult.NORMAL, null, bodyValue);
                }
            }

            return result;
    };

    private static final CallableValue BREAK_VAL = (rt, arguments, bindings) -> {
            if (arguments.size() > 1) {
                throw new RuntimeException("break() expects 0 or 1 arguments");
            }
            if (arguments.size() == 1) {
                ValueResult v = rt.execute(arguments.get(0));
                if (v.isLaunched()) {
                    return v;
                }
                return ValueResult.of(ValueResult.LAUNCHED, "break", v.getValue());
            }
            return ValueResult.of(ValueResult.LAUNCHED, "break", Value.ofNull());
    };

    private static final CallableValue CONTINUE_VAL = (rt, arguments, bindings) -> {
            if (!arguments.isEmpty()) {
                throw new RuntimeException("continue() expects 0 arguments");
            }
            return ValueResult.of(ValueResult.LAUNCHED, "continue", Value.ofNull());
    };

    private static final CallableValue PRINTLN_VAL = (rt, arguments, bindings) -> {
            if (arguments.isEmpty()) {
                System.out.println();
                return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
            }

            ValueResult v = rt.execute(arguments.get(0));
            if (v.isLaunched()) {
                return v;
            }
            System.out.println(v.getValue().valueToString());

            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
    };

    private static final CallableValue PRINT_VAL = (rt, arguments, bindings) -> {
            if (arguments.isEmpty()) {
                throw new RuntimeException("print() expects 1 argument");
            }

            ValueResult v = rt.execute(arguments.get(0));
            if (v.isLaunched()) {
                return v;
            }
            System.out.print(v.getValue().valueToString());

            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
    };

    private static final CallableValue READLN_VAL = (rt, arguments, bindings) -> {
            if (arguments.size() > 1) {
                throw new RuntimeException("readln() expects 0 or 1 arguments");
            }

            if (arguments.isEmpty()) {
                return ValueResult.of(
                        ValueResult.NORMAL,
                        null,
                        Value.ofString(SCANNER.nextLine()));
            }

            ValueResult typeResult = rt.execute(arguments.get(0));

            if (typeResult.isLaunched()) {
                return typeResult;
            }

            Value type = typeResult.getValue();

            if (!type.isString()) {
                throw new RuntimeException("readln() type must be string");
            }

            String input = SCANNER.nextLine();
            if (input.isEmpty()) {
                switch (type.valueToString()) {
                    case "int":
                        return ValueResult.of(
                                ValueResult.NORMAL, null, Value.ofInt(0));

                    case "float":
                        return ValueResult.of(
                                ValueResult.NORMAL, null, Value.ofFloat(0.0));

                    case "bool":
                        return ValueResult.of(
                                ValueResult.NORMAL, null, Value.ofBool(false));

                    case "string":
                        return ValueResult.of(
                                ValueResult.NORMAL, null, Value.ofString(""));

                    default:
                        throw new RuntimeException(
                                "readln(): unknown type: " + type.valueToString());
                }
            }

            switch (type.valueToString()) {
                case "int":
                    return ValueResult.of(
                            ValueResult.NORMAL,
                            null,
                            Value.ofInt(Long.parseLong(input)));

                case "float":
                    return ValueResult.of(
                            ValueResult.NORMAL,
                            null,
                            Value.ofFloat(Double.parseDouble(input)));

                case "bool":
                    return ValueResult.of(
                            ValueResult.NORMAL,
                            null,
                            Value.ofBool(Boolean.parseBoolean(input)));

                case "string":
                    return ValueResult.of(
                            ValueResult.NORMAL,
                            null,
                            Value.ofString(input));

                default:
                    throw new RuntimeException(
                            "readln(): unknown type: " + type.valueToString());
            }
    };

    private static final CallableValue LAUNCH_VAL = (rt, arguments, bindings) -> {
            if (arguments.size() != 1 && arguments.size() != 2) {
                throw new RuntimeException("launch(id) or launch(id, value) expects 1 or 2 arguments");
            }

            ValueResult idResult = rt.execute(arguments.get(0));
            if (idResult.isLaunched()) {
                return idResult;
            }

            Value id = idResult.getValue();
            if (!id.isString()) {
                throw new RuntimeException("launch() id must be string");
            }

            Value value = Value.ofNull();
            if (arguments.size() == 2) {
                ValueResult valueResult = rt.execute(arguments.get(1));

                if (valueResult.isLaunched()) {
                    return valueResult;
                }

                value = valueResult.getValue();
            }

            return ValueResult.of(ValueResult.LAUNCHED, id.valueToString(), value);
    };

    private static final CallableValue FINALIZE_VAL = (rt, arguments, bindings) -> {
            if (arguments.size() != 1 && arguments.size() != 2) {
                throw new RuntimeException("finalize(body) or finalize(id, body) expects 1 or 2 arguments");
            }

            ValueResult idResult = rt.execute(arguments.get(0));
            if (idResult.isLaunched()) {
                return idResult;
            }

            Value id = idResult.getValue();
            if (!id.isString()) {
                throw new RuntimeException("finalize() id must be string");
            }

            Value body = Value.ofNull();
            if (arguments.size() == 2) {
                ValueResult bodyResult = rt.execute(arguments.get(1));
                if (!bodyResult.idEquals(id.valueToString())) {
                    return bodyResult;
                }

                body = bodyResult.getValue();
            }

            rt.peekScope().addCollector(id.valueToString());
            return ValueResult.of(ValueResult.NORMAL, null, body);
    };

    private static final CallableValue ARRAY_OF_VAL = (rt, arguments, bindings) -> {
            List<Value> elements = new ArrayList<>();

            for (Expr arg : arguments) {
                ValueResult result = rt.execute(arg);
                if (result.isLaunched()) {
                    return result;
                }
                elements.add(result.getValue());
            }

            BlockValue array = new BlockValue() {
                private final List<Value> data = new ArrayList<>(elements);

                {
                    setLocal("get", (CallableValue) (rt, arguments, bindings) -> {
                            if (arguments.size() != 1) {
                                throw new RuntimeException("get(index) expects 1 argument");
                            }

                            ValueResult indexResult = rt.execute(arguments.get(0));
                            if (indexResult.isLaunched()) {
                                return indexResult;
                            }

                            Value indexVal = indexResult.getValue();
                            if (!indexVal.isInt()) {
                                throw new RuntimeException("get() index must be integer");
                            }

                            long index = indexVal.asInt().getValue();
                            if (index < 0 || index >= data.size()) {
                                throw new RuntimeException("get() index out of bounds: " + index);
                            }

                            return ValueResult.of(ValueResult.NORMAL, null, data.get((int) index));
                    });

                    setLocal("set", (CallableValue) (rt, arguments, bindings) -> {
                            if (arguments.size() != 2) {
                                throw new RuntimeException("set(index, value) expects 2 arguments");
                            }

                            ValueResult indexResult = rt.execute(arguments.get(0));
                            if (indexResult.isLaunched()) {
                                return indexResult;
                            }

                            Value indexVal = indexResult.getValue();
                            if (!indexVal.isInt()) {
                                throw new RuntimeException("set() index must be integer");
                            }

                            long index = indexVal.asInt().getValue();
                            if (index < 0 || index >= data.size()) {
                                throw new RuntimeException("set() index out of bounds: " + index);
                            }

                            ValueResult valueResult = rt.execute(arguments.get(1));
                            if (valueResult.isLaunched()) {
                                return valueResult;
                            }

                            data.set((int) index, valueResult.getValue());
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
                    });

                    setLocal("size", (CallableValue) (rt, arguments, bindings) -> {
                            if (!arguments.isEmpty()) {
                                throw new RuntimeException("size() expects 0 arguments");
                            }
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofInt(data.size()));
                    });
                }
            };

            return ValueResult.of(ValueResult.NORMAL, null, array);
    };

    private static final CallableValue LIST_OF_VAL = (rt, arguments, bindings) -> {
            List<Value> elements = new ArrayList<>();

            for (Expr arg : arguments) {
                ValueResult result = rt.execute(arg);
                if (result.isLaunched()) {
                    return result;
                }
                elements.add(result.getValue());
            }

            BlockValue list = new BlockValue() {
                private final List<Value> data = new ArrayList<>(elements);

                {
                    setLocal("get", (CallableValue) (rt, arguments, bindings) -> {
                            if (arguments.size() != 1) {
                                throw new RuntimeException("get(index) expects 1 argument");
                            }

                            ValueResult indexResult = rt.execute(arguments.get(0));
                            if (indexResult.isLaunched()) {
                                return indexResult;
                            }

                            Value indexVal = indexResult.getValue();
                            if (!indexVal.isInt()) {
                                throw new RuntimeException("get() index must be integer");
                            }

                            long index = indexVal.asInt().getValue();
                            if (index < 0 || index >= data.size()) {
                                throw new RuntimeException("get() index out of bounds: " + index);
                            }

                            return ValueResult.of(ValueResult.NORMAL, null, data.get((int) index));
                    });

                    setLocal("set", (CallableValue) (rt, arguments, bindings) -> {
                            if (arguments.size() != 2) {
                                throw new RuntimeException("set(index, value) expects 2 arguments");
                            }

                            ValueResult indexResult = rt.execute(arguments.get(0));
                            if (indexResult.isLaunched()) {
                                return indexResult;
                            }

                            Value indexVal = indexResult.getValue();
                            if (!indexVal.isInt()) {
                                throw new RuntimeException("set() index must be integer");
                            }

                            long index = indexVal.asInt().getValue();
                            if (index < 0 || index >= data.size()) {
                                throw new RuntimeException("set() index out of bounds: " + index);
                            }

                            ValueResult valueResult = rt.execute(arguments.get(1));
                            if (valueResult.isLaunched()) {
                                return valueResult;
                            }

                            data.set((int) index, valueResult.getValue());
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
                    });

                    setLocal("size", (CallableValue) (rt, arguments, bindings) -> {
                            if (!arguments.isEmpty()) {
                                throw new RuntimeException("size() expects 0 arguments");
                            }
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofInt(data.size()));
                    });

                    setLocal("add", (CallableValue) (rt, arguments, bindings) -> {
                            if (arguments.size() != 1) {
                                throw new RuntimeException("add(value) expects 1 argument");
                            }

                            ValueResult valueResult = rt.execute(arguments.get(0));
                            if (valueResult.isLaunched()) {
                                return valueResult;
                            }

                            data.add(valueResult.getValue());
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
                    });

                    setLocal("remove", (CallableValue) (rt, arguments, bindings) -> {
                            if (arguments.size() != 1) {
                                throw new RuntimeException("remove(index) expects 1 argument");
                            }

                            ValueResult indexResult = rt.execute(arguments.get(0));
                            if (indexResult.isLaunched()) {
                                return indexResult;
                            }

                            Value indexVal = indexResult.getValue();
                            if (!indexVal.isInt()) {
                                throw new RuntimeException("remove() index must be integer");
                            }

                            long index = indexVal.asInt().getValue();
                            if (index < 0 || index >= data.size()) {
                                throw new RuntimeException("remove() index out of bounds: " + index);
                            }

                            Value removed = data.remove((int) index);
                            return ValueResult.of(ValueResult.NORMAL, null, removed);
                    });

                    setLocal("clear", (CallableValue) (rt, arguments, bindings) -> {
                            if (!arguments.isEmpty()) {
                                throw new RuntimeException("clear() expects 0 arguments");
                            }
                            data.clear();
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
                    });

                    setLocal("contains", (CallableValue) (rt, arguments, bindings) -> {
                            if (arguments.size() != 1) {
                                throw new RuntimeException("contains(value) expects 1 argument");
                            }

                            ValueResult valueResult = rt.execute(arguments.get(0));
                            if (valueResult.isLaunched()) {
                                return valueResult;
                            }

                            Value searchValue = valueResult.getValue();
                            boolean found = data.contains(searchValue);
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofBool(found));
                    });

                    setLocal("indexOf", (CallableValue) (rt, arguments, bindings) -> {
                            if (arguments.size() != 1) {
                                throw new RuntimeException("indexOf(value) expects 1 argument");
                            }

                            ValueResult valueResult = rt.execute(arguments.get(0));
                            if (valueResult.isLaunched()) {
                                return valueResult;
                            }

                            Value searchValue = valueResult.getValue();
                            int index = data.indexOf(searchValue);
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofInt(index));
                    });

                    setLocal("isEmpty", (CallableValue) (rt, arguments, bindings) -> {
                            if (!arguments.isEmpty()) {
                                throw new RuntimeException("isEmpty() expects 0 arguments");
                            }
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofBool(data.isEmpty()));
                    });
                }
            };

            return ValueResult.of(ValueResult.NORMAL, null, list);
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

        block.setLocal("include", INCLUDE_VAL);
        block.setLocal("import", IMPORT_VAL);
        block.setLocal("export", EXPORT_VAL);

        block.setLocal("if", IF_VAL);
        block.setLocal("while", WHILE_VAL);
        block.setLocal("break", BREAK_VAL);
        block.setLocal("continue", CONTINUE_VAL);

        block.setLocal("println", PRINTLN_VAL);
        block.setLocal("print", PRINT_VAL);
        block.setLocal("readln", READLN_VAL);

        block.setLocal("launch", LAUNCH_VAL);
        block.setLocal("finalize", FINALIZE_VAL);

        block.setLocal("arrayOf", ARRAY_OF_VAL);
        block.setLocal("listOf", LIST_OF_VAL);
    }

    private static Program compileFile(Execution rt, String name) {
        File file = new File(rt.workingDir(), name);

        if (!file.isFile() && !name.endsWith(".l")) {
            file = new File(rt.workingDir(), name + ".l");
        }

        if (!file.isFile()) {
            throw new RuntimeException("Module not found: " + name);
        }

        try (FileInputStream input = new FileInputStream(file)) {
            SourceStream source = new SourceStream(input);

            CompilationUnit unit = new CompilationUnit(file.getAbsolutePath(), file.getName());

            Lexer lexer = new Lexer(source, unit);
            Parser parser = new Parser(lexer, unit);

            parser.parse();
            // unit.printReport();

            if (unit.hasErrors()) {
                throw new RuntimeException("Compilation failed: " + name);
            }

            return unit.getProgram();

        } catch (IOException e) {
            throw new RuntimeException("Failed to compile module: " + name, e);
        }
    }
}