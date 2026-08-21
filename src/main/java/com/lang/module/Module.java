// com/lang/module/Module.java
package com.lang.module;

import com.lang.ast.Program;
import com.lang.value.BlockValue;

public class Module {
    private final String name;
    private final String path;
    private final Program program;
    private BlockValue exports;
    private boolean loaded;


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