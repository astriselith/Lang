// com/lang/module/Module.java
package com.lang.module;

import com.lang.ast.Program;
import com.lang.context.Context;
import com.lang.value.BlockValue;
import com.lang.value.Value;
import com.lang.value.ValueResult;
import com.lang.ast.Expr;

import java.util.List;

public class Module {
    private final String name;
    private final String path;
    private final Program program;
    private BlockValue exports;
    private boolean loaded;

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

    public Module(String name, String path, Program program) {
        this.name = name;
        this.path = path;
        this.program = program;
        this.exports = new BlockValue();
        this.loaded = false;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Program getProgram() {
        return program;
    }

    public BlockValue getExports() {
        return exports;
    }

    public void setExports(BlockValue exports) {
        this.exports = exports;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    public String toString() {
        return "Module{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", loaded=" + loaded +
                '}';
    }
}