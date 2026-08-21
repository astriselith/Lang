# Lang

Lang is an experimental programming language implemented in Java 11. Source files use the `.l` extension and are processed in four stages:

```text
source (.l) -> SourceStream -> Lexer -> TokenStream -> Parser -> AST -> Evaluator
```

The lexer and parser share a `CompilationUnit`, so lexical and syntax errors are collected with source positions before execution begins.

## Requirements

- Java 11 or newer
- Gradle wrapper (`./gradlew`)

## Build and Run

Build the project:

```bash
./gradlew build
```

Run a source file:

```bash
./gradlew run --args="form/main.l"
```

The launcher also accepts an entry point without the `.l` suffix and supports:

```text
-h, --help                 Show usage
-v, --verbose              Print the generated AST
-w, --working-dir <path>   Set the working directory
-e, --entry <file>         Set the entry point
```

Examples:

```bash
./gradlew run --args="--verbose form/main.l"
./gradlew run --args="--working-dir form --entry main.l"
./gradlew run --args="form/main.l first second"
```

Running the application without arguments opens the `lang>` interactive loop. Enter `exit` or `quit` to leave it.

## Lexer

`com.lang.lexer.Lexer` reads Unicode code points from a `CodepointStream` and produces a buffered `TokenStream`. It tracks line and character offsets on every token.

### Ignored input

- Unicode whitespace
- Line comments beginning with `//`
- Block comments enclosed by `/*` and `*/`

### Literals and identifiers

The lexer recognizes:

| Form | Examples |
| --- | --- |
| Integer | `0`, `42`, `1_000` |
| Floating-point | `3.14`, `1e3`, `2.5E-2` |
| Boolean | `true`, `false` |
| Null | `null` |
| String | `"hello"`, `"line\\n"`, `"quote: \\\""` |
| Identifier | `name`, `read_line`, `value2` |

Strings support `\\n`, `\\r`, `\\t`, `\\\\`, and `\\"` escape sequences. Invalid characters, malformed numbers, unknown escapes, unterminated comments, and unterminated strings are reported with the `LEXER` tag.

### Token categories

The lexer emits tokens for:

- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Comparison: `=`, `!`, `<`, `>`
- Logical: `&`, `|`
- Other operators: `?`, `:`
- Delimiters: `(`, `)`, `{`, `}`, `[`, `]`, `,`, `.`, `;`
- Special symbols: `$`, `@`
- `EOF`

The parser combines individual tokens into multi-character operators such as `==`, `!=`, `<=`, `>=`, `&&`, `||`, `::`, and `->`.

## Parser

`com.lang.parser.Parser` consumes the token stream and builds the AST rooted at `Program`. Every top-level expression must end with a semicolon. Parser errors are collected with the `PARSER` tag and parsing continues at the next semicolon when possible.

### Expression precedence

Expressions are parsed from lowest to highest precedence:

1. Assignment: `=`
2. Logical OR: `||`
3. Logical AND: `&&`
4. Equality: `==`, `!=`
5. Comparison: `<`, `<=`, `>`, `>=`
6. Addition and subtraction: `+`, `-`
7. Multiplication, division, and remainder: `*`, `/`, `%`
8. Unary operators: `!`, `+`, `-`
9. Postfix calls and member access: `(...)`, `:`, `.`
10. Primary expressions: literals, identifiers, and blocks

Assignment is right-associative. Binary operators at the same level are parsed left-associatively.

### Supported syntax

Basic expressions:

```lang
name = "Ada";
age = 36;
ready = age >= 18 && name != "";
println(name + " is ready: " + ready);
```

Function-style blocks use `->` and may have parameters:

```lang
add = (left, right) -> left + right;
result = add(2, 3);
```

A block body uses braces and semicolon-terminated expressions:

```lang
announce = (message) -> {
    println(message);
    return(message);
};
```

Blocks can also declare attachments after `:`. A double colon separates multiple attachments:

```lang
try: {
    operation();
} :: (error) -> {
    println(error);
};
```

Member access and calls can be chained:

```lang
Input.read("Name: ");
object.member();
```

### Parser pipeline in Java

The normal compilation path is:

```java
CompilationUnit unit = new CompilationUnit(path, name);
Lexer lexer = new Lexer(new SourceStream(input), unit);
Parser parser = new Parser(lexer, unit);
parser.parse();

if (unit.hasErrors()) {
    unit.printReport();
}
```

The parser itself does not evaluate expressions. It creates AST nodes such as `LiteralExpr`, `RefExpr`, `AssignExpr`, `BinaryExpr`, `CallExpr`, `MemberAccessExpr`, and `BlockExpr`. Evaluation is performed afterward by `com.lang.evaluator.Evaluator`.

## Modules and Examples

`ModuleLoader` compiles modules through the same lexer/parser pipeline. It searches the working directory and these locations:

- `<working-directory>`
- `<working-directory>/modules`
- `<working-directory>/lib`
- `<working-directory>/.lang/modules`

The `form/` directory contains runnable examples:

- `form/main.l`: input, validation, function calls, and control-flow helpers
- `form/Input.l`: reading input and exporting a block
- `form/CFlow.l`: `try`, `throw`, and `assert` helpers

## Error Reporting

Compilation does not start when the `CompilationUnit` contains lexer or parser errors. Errors include their category and source position, which makes malformed input visible before the evaluator runs.

## Project Layout

```text
src/main/java/com/lang/
├── ast/       AST node types
├── evaluator/ Runtime evaluation
├── lexer/     Source-to-token conversion
├── parser/    Token-to-AST conversion
├── source/    Input streams
├── token/     Tokens and token buffering
├── module/    Module loading
└── value/     Runtime values
```

## License

No license has been declared for this project yet.
