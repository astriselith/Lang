package com.lang.value;

import com.lang.ast.BlockExpr;
import com.lang.ast.Expr;
import com.lang.execution.Execution;

import java.util.*;

public class BlockValue extends Value {
    protected final BlockExpr block;
    protected final Map<String, Value> locals;
    protected final Set<String> collectors;
    protected final BlockValue parent;

    public BlockValue() {
        this(null, null);
    }

    public BlockValue(BlockExpr block, BlockValue parent) {
        this.block = block;
        this.parent = parent;
        this.locals = new HashMap<>();
        this.collectors = new HashSet<>();
    }

    @Override
    public String toLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (block != null && block.parameters != null) {
            for (int i = 0; i < block.parameters.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(block.parameters.get(i).name.source);
            }
        }
        sb.append(") -> {}");
        return sb.toString();
    }

    public Value get(String name) {
        if (locals.containsKey(name)) {
            return locals.get(name);
        }
        if (parent != null) {
            return parent.get(name);
        }
        return null;
    }

    public boolean has(String name) {
        if (locals.containsKey(name)) {
            return true;
        }
        if (parent != null) {
            return parent.has(name);
        }
        return false;
    }

    public void set(String name, Value value) {
        if (locals.containsKey(name)) {
            locals.put(name, value);
            return;
        }

        if (parent != null && parent.has(name)) {
            parent.set(name, value);
            return;
        }

        locals.put(name, value);
    }

    public Value getLocal(String name) {
        return locals.get(name);
    }

    public boolean hasLocal(String name) {
        return locals.containsKey(name);
    }

    public void setLocal(String name, Value value) {
        locals.put(name, value);
    }

    public Map<String, Value> getLocals() {
        return locals;
    }

    public void addCollector(String id) {
        collectors.add(id);
    }

    public boolean hasCollector(String id) {
        return collectors.contains(id);
    }

    public Set<String> getCollectors() {
        return collectors;
    }

    public BlockValue getParent() {
        return parent;
    }

    public BlockExpr getBlock() {
        return block;
    }

    public ValueResult call(
            Execution rt,
            List<Expr> arguments,
            List<Expr> bindings) {

        int paramCount = block.parameters.size();

        if (paramCount != arguments.size()) {
            throw new RuntimeException(
                    "Expected "
                            + paramCount
                            + " arguments, got "
                            + arguments.size());
        }

        if (block.attachments.size() != bindings.size()) {
            throw new RuntimeException(
                    "Expected "
                            + block.attachments.size()
                            + " attachments, got "
                            + bindings.size());
        }

        BlockValue newBlock = new BlockValue(block, parent);

        for (int i = 0; i < paramCount; i++) {
            String name = block.parameters.get(i).name.source;
            ValueResult argResult = rt.accept(arguments.get(i));
            if (argResult.isLaunched()) {
                return argResult;
            }
            newBlock.set(name, argResult.getValue());

        }

        for (int i = 0; i < bindings.size(); i++) {
            String name = block.attachments.get(i).name.source;
            ValueResult bindResult = rt.accept(bindings.get(i));
            if (bindResult.isLaunched()) {
                return bindResult;
            }
            newBlock.set(name, bindResult.getValue());
        }

        rt.pushScope(newBlock);

        for (Expr expression : block.expressions) {
            ValueResult result = rt.accept(expression);

            if (result.isLaunched()) {
                rt.popScope();

                if (newBlock.hasCollector(result.getId())) {
                    return ValueResult.of(ValueResult.NORMAL, null, result.getValue());
                } else {
                    return result;
                }
            }
        }
        rt.popScope();

        return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
    }

}
