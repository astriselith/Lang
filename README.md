# Lang

Lang is an experimental programming language implemented in Java. Programs use the `.l` extension and are compiled and evaluated in this order:

```text
.l source -> SourceStream -> Lexer -> Parser -> AST -> Evaluator
```

The lexer and parser share a `CompilationUnit`, which collects errors with source positions before evaluation starts.

## Requirements

- Java 11 or newer
- A shell capable of running the Gradle wrapper

## Build and Run

Build the project:

```bash
./gradlew build
```

Run a program:

```bash
./gradlew run --args="form/main.l"
```

The command-line launcher accepts:

| Option | Description |
| --- | --- |
| `-h`, `--help` | Show usage |
| `-v`, `--verbose` | Print the generated AST |
| `-w`, `--working-dir <path>` | Set the working directory |
| `-e`, `--entry <file>` | Set the entry point |

Arguments after the entry point are exposed as `args.arg0`, `args.arg1`, and so on. Examples:

```bash
./gradlew run --args="--help"
./gradlew run --args="--verbose form/main.l"
./gradlew run --args="--working-dir form --entry main.l"
./gradlew run --args="form/main.l first second"
```

Running without arguments starts the `lang>` interactive loop. Press Enter to repeat the previous command; enter `exit` or `quit` to leave.

## Language Basics

Statements end with semicolons. Values include integers, floating-point numbers, booleans, strings, and `null`.

```lang
name = "Ada";
age = 36;
ready = age >= 18 && name != "";
println(name + " is ready: " + ready);
```

Functions are callable blocks. A block can be an expression or a braced sequence of statements:

```lang
add = (left, right) -> left + right;
announce = (message) -> {
    println(message);
};

println(add(2, 3));
announce("hello");
```

Attachments use `:` and can provide alternate control-flow branches with `|`:

```lang
if(age >= 18): {
    println("adult");
} | {
    println("minor");
};
```

Member access and calls are chained with `.`:

```lang
values = listOf(10, 20);
values.add(30);
println(values.get(0));
println(values.size());
```

### Operators

From lowest to highest precedence:

1. Assignment: `=`
2. Logical OR: `||`
3. Logical AND: `&&`
4. Equality: `==`, `!=`
5. Comparison: `<`, `<=`, `>`, `>=`
6. Addition and subtraction: `+`, `-`
7. Multiplication, division, and remainder: `*`, `/`, `%`
8. Unary operators: `!`, `+`, `-`
9. Calls and member access

Comments use `//` for a line or `/* ... */` for a block. Strings support `\\n`, `\\r`, `\\t`, `\\\\`, and `\\"` escapes.

## Standard Library

The runtime opens these built-ins in the program scope:

| Function | Purpose |
| --- | --- |
| `include(name)` | Load a module and copy its exported locals into the current scope |
| `import(name)` | Load a module and return its exported block |
| `export(value)` | Export a block from a module |
| `if(condition)` | Evaluate one or two attached callable branches |
| `while(condition)` | Repeatedly evaluate an attached body |
| `break(value)` | Leave a loop with an optional value |
| `continue()` | Continue the current loop |
| `print(value)` | Write a value without a newline |
| `println(value)` | Write a value with a newline |
| `readln(type)` | Read input as `int`, `float`, `bool`, or `string` |
| `launch(id, value)` | Raise a named control-flow result |
| `finalize(id, body)` | Consume a named control-flow result |
| `arrayOf(...)` | Create a fixed-size mutable indexed collection |
| `listOf(...)` | Create a mutable indexed collection with add/remove operations |

Modules are resolved relative to the active working directory. The optional `.l` suffix may be omitted. `form/control-flow.l` demonstrates module exports and defines `throw`, `try`, and `assert` helpers.

## Compiler Architecture

The main Java components are:

```text
src/main/java/com/lang/
├── ast/        Abstract syntax tree nodes
├── buffer/     Buffered input support
├── codepoint/  Unicode code-point streams
├── evaluator/  Runtime evaluation
├── execution/  Execution context and frames
├── lexer/      Source-to-token conversion
├── library/    Standard library functions
├── parser/     Token-to-AST conversion
├── source/     Source input streams
├── token/      Tokens and token streams
├── tools/      AST printing utilities
├── unit/       Compilation units and diagnostics
├── util/       Shared utilities
└── value/      Runtime values and callables
```

To embed the compiler pipeline in Java:

```java
CompilationUnit unit = new CompilationUnit(path, name);
Lexer lexer = new Lexer(new SourceStream(input), unit);
Parser parser = new Parser(lexer, unit);
parser.parse();

if (!unit.hasErrors()) {
    new Evaluator(workingDirectory).execute(unit.getProgram(), scope);
}
```

## Development

Run the test suite:

```bash
./gradlew test
```

Compile without running tests:

```bash
./gradlew compileJava
```

The project currently has no declared license.
