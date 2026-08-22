package com.lang.library;

import com.lang.value.BlockValue;
import com.lang.ast.BlockExpr;
import com.lang.ast.Expr;
import com.lang.ast.RefExpr;
import com.lang.context.Context;
import com.lang.module.ModuleLoader;
import com.lang.value.Value;
import com.lang.value.ValueResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public final class StandardLibrary implements Library {
    private static final StandardLibrary INSTANCE = new StandardLibrary();
    private static final Scanner SCANNER = new Scanner(System.in);

    public static final BlockValue INCLUDE = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 1) {
                throw new RuntimeException("iclude(name) expects 1 argument");
            }

            ValueResult nameResult = (ValueResult) arguments.get(0).accept(ctx.visitor());
            if (nameResult.isLaunched()) {
                return nameResult;
            }

            String moduleName = nameResult.getValue().toLString();

            try {
                BlockValue exports = ModuleLoader.getInstance().load(moduleName);
                BlockValue target = ctx.stack().peek();
                exports.getLocals().forEach((name, value) -> target.setLocal(name, value));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load module: " + moduleName, e);
            }
            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
        }
    };

    public static final BlockValue IMPORT = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 1) {
                throw new RuntimeException("import(name) expects 1 argument");
            }

            ValueResult nameResult = (ValueResult) arguments.get(0).accept(ctx.visitor());
            if (nameResult.isLaunched()) {
                return nameResult;
            }

            String name = nameResult.getValue().toLString();

            try {
                BlockValue exports = ModuleLoader.getInstance().load(name);
                return ValueResult.of(ValueResult.NORMAL, null, exports);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load module: " + name, e);
            }
        }
    };

    public static final BlockValue EXPORT = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 1) {
                throw new RuntimeException("export(obj) expects 1 argument");
            }

            ValueResult objResult = (ValueResult) arguments.get(0).accept(ctx.visitor());
            if (objResult.isLaunched()) {
                return objResult;
            }

            Value obj = objResult.getValue();

            return ValueResult.of(ValueResult.LAUNCHED, "module", obj);
        }
    };

    private static final BlockValue FUN = new BlockValue() {

        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {

            if (bindings.isEmpty()) {
                throw new RuntimeException("fun() requires at least one attachment (the function body)");
            }

            Expr body = bindings.get(bindings.size() - 1);

            if (!(body instanceof BlockExpr)) {
                throw new RuntimeException("fun() last attachment must be a BlockExpr");
            }

            List<RefExpr> params = new ArrayList<>();
            List<RefExpr> attachments = new ArrayList<>();

            for (Expr arg : arguments) {
                if (!(arg instanceof RefExpr)) {
                    throw new RuntimeException("fun() arguments must be RefExpr");
                }

                params.add((RefExpr) arg);
            }

            for (int i = 0; i < bindings.size() - 1; i++) {
                Expr binding = bindings.get(i);

                if (!(binding instanceof RefExpr)) {
                    throw new RuntimeException("fun() attachments must be RefExpr");
                }

                attachments.add((RefExpr) binding);
            }

            BlockValue function = new BlockValue(null, ctx.stack().peek()) {

                @Override
                public ValueResult call(Context ctx, List<Expr> args, List<Expr> binds) {

                    if (args.size() != params.size()) {
                        throw new RuntimeException("Expected " + params.size() + " arguments, got " + args.size());
                    }

                    if (binds.size() != attachments.size()) {
                        throw new RuntimeException("Expected " + attachments.size() + " bindings, got " + binds.size());
                    }

                    BlockValue scope = new BlockValue(null, this);

                    for (int i = 0; i < params.size(); i++) {

                        ValueResult result = (ValueResult) args.get(i).accept(ctx.visitor());

                        if (result.isLaunched()) {
                            return result;
                        }

                        scope.setLocal(params.get(i).name.source, result.getValue());
                    }

                    for (int i = 0; i < attachments.size(); i++) {

                        ValueResult result = (ValueResult) binds.get(i).accept(ctx.visitor());

                        if (result.isLaunched()) {
                            return result;
                        }

                        scope.setLocal(attachments.get(i).name.source, result.getValue());
                    }

                    ctx.stack().push(scope);

                    ValueResult result = new BlockValue((BlockExpr) body, scope).call(ctx, Collections.emptyList(),
                            Collections.emptyList());

                    ctx.stack().pop();

                    if (result.isLaunched() && "return".equals(result.getId())) {

                        return ValueResult.of(ValueResult.NORMAL, null, result.getValue());
                    }

                    return result;
                }
            };

            return ValueResult.of(ValueResult.NORMAL, null, function);
        }
    };

    private static final BlockValue IF = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 1) {
                throw new RuntimeException("if(condition) expects 1 argument");
            }

            if (bindings.size() != 1 && bindings.size() != 2) {
                throw new RuntimeException("if(condition) requires only 1 or 2 bindings (then) or (then, else)");
            }

            ValueResult condResult = (ValueResult) arguments.get(0).accept(ctx.visitor());
            if (condResult.isLaunched()) {
                return condResult;
            }

            Value cond = condResult.getValue();
            if (!cond.isBool()) {
                throw new RuntimeException("if() condition must be boolean");
            }

            Expr chosen = bindings.size() == 1 ? (cond.toLBool() ? bindings.get(0) : null)
                    : (cond.toLBool() ? bindings.get(0) : bindings.get(1));

            if (chosen == null) {
                return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
            }

            ValueResult blockResult = (ValueResult) chosen.accept(ctx.visitor());
            if (blockResult.isLaunched()) {
                return blockResult;
            }

            Value blockValue = blockResult.getValue();
            if (!blockValue.isBlock()) {
                throw new RuntimeException("if() then and else must be blocks");
            }

            return blockValue.asBlock().call(ctx, new ArrayList<>(), new ArrayList<>());
        }
    };

    private static final BlockValue WHILE = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
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
                ValueResult condResult = (ValueResult) condition.accept(ctx.visitor());

                if (condResult.isLaunched()) {
                    if (condResult.getId().equals("break")) {
                        result = ValueResult.of(ValueResult.NORMAL, null, condResult.getValue());
                        break;
                    } else if (condResult.getId().equals("continue")) {
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

                ValueResult bodyResult = (ValueResult) body.accept(ctx.visitor());

                if (bodyResult.isLaunched()) {
                    if (bodyResult.getId().equals("break")) {
                        result = ValueResult.of(ValueResult.NORMAL, null, bodyResult.getValue());
                        break;
                    } else if (bodyResult.getId().equals("continue")) {
                        continue;
                    } else {
                        return bodyResult;
                    }
                }

                Value bodyValue = bodyResult.getValue();

                if (bodyValue != null && bodyValue.isBlock()) {
                    result = bodyValue.asBlock().call(ctx, new ArrayList<>(), new ArrayList<>());
                } else {
                    result = ValueResult.of(ValueResult.NORMAL, null, bodyValue);
                }
            }

            return result;
        }
    };

    private static final BlockValue BREAK = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() > 1) {
                throw new RuntimeException("break() expects 0 or 1 arguments");
            }
            if (arguments.size() == 1) {
                ValueResult v = (ValueResult) arguments.get(0).accept(ctx.visitor());
                return ValueResult.of(ValueResult.LAUNCHED, "break", v.getValue());
            }
            return ValueResult.of(ValueResult.LAUNCHED, "break", Value.ofNull());
        }
    };

    private static final BlockValue CONTINUE = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (!arguments.isEmpty()) {
                throw new RuntimeException("continue() expects 0 arguments");
            }
            return ValueResult.of(ValueResult.LAUNCHED, "continue", Value.ofNull());
        }
    };

    private static final BlockValue PRINTLN = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() == 0) {
                System.out.println();
            } else {
                for (Expr arg : arguments) {
                    ValueResult v = (ValueResult) arg.accept(ctx.visitor());
                    if (v.isLaunched()) {
                        return v;
                    }
                    System.out.println(v.getValue().toLString());
                }
            }
            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
        }
    };

    private static final BlockValue READLN = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (!arguments.isEmpty()) {
                throw new RuntimeException("readln() expects 0 arguments");
            }

            return ValueResult.of(
                    ValueResult.NORMAL, null, Value.ofString(SCANNER.nextLine()));
        }
    };

    private static final BlockValue RETURN = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 1) {
                throw new RuntimeException("return() expects 1 argument");
            }
            ValueResult v = (ValueResult) arguments.get(0).accept(ctx.visitor());
            return ValueResult.of(ValueResult.LAUNCHED, "return", v.getValue());
        }
    };

    private static final BlockValue LAUNCH = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 2) {
                throw new RuntimeException("launch(id, value) expects 2 arguments");
            }

            ValueResult idResult = (ValueResult) arguments.get(0).accept(ctx.visitor());

            if (idResult.isLaunched()) {
                return idResult;
            }

            Value id = idResult.getValue();

            if (!id.isString()) {
                throw new RuntimeException("launch() id must be string");
            }

            ValueResult valueResult = (ValueResult) arguments.get(1).accept(ctx.visitor());

            if (valueResult.isLaunched()) {
                return valueResult;
            }

            return ValueResult.of(ValueResult.LAUNCHED, id.toLString(), valueResult.getValue());
        }
    };

    private static final BlockValue FINALIZE = new BlockValue() {
        @Override
        public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
            if (arguments.size() != 2) {
                throw new RuntimeException("finalize(id, body) expects 2 arguments");
            }

            ValueResult idResult = (ValueResult) arguments.get(0).accept(ctx.visitor());

            if (idResult.isLaunched()) {
                return idResult;
            }

            Value id = idResult.getValue();

            if (!id.isString()) {
                throw new RuntimeException("finalize() id must be string");
            }

            ValueResult bodyResult = (ValueResult) arguments.get(1).accept(ctx.visitor());

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

        block.set("include", INCLUDE);
        block.set("import", IMPORT);
        block.set("export", EXPORT);

        block.set("fun", FUN);
        block.set("return", RETURN);

        block.set("if", IF);
        block.set("while", WHILE);
        block.set("break", BREAK);
        block.set("continue", CONTINUE);

        block.set("println", PRINTLN);
        block.set("readln", READLN);

        block.set("launch", LAUNCH);
        block.set("finalize", FINALIZE);
    }
}