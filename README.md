# ICSI311 - Principles of Programming Languages
Applications are contained in the src folder.

## Tran Language Interpreter

This repository contains a Java-based programming language project created for a Principles of Programming Languages course. The project implements major parts of a custom programming language called **Tran**, including lexical analysis, parsing, abstract syntax tree generation, and interpretation.

The project processes source code written in the Tran language, converts it into tokens, builds an Abstract Syntax Tree, and interprets the program using custom runtime data types.

## Features

- Java-based programming language implementation
- Custom lexer for tokenizing Tran source code
- Custom parser based on Tran language grammar rules
- Abstract Syntax Tree node system
- Interpreter for executing parsed Tran programs
- Support for classes and interfaces
- Support for constructors
- Support for shared and private methods
- Support for member variables and local variables
- Support for method parameters and return values
- Support for multiple return values
- Support for object creation using `new`
- Support for method calls on objects and classes
- Support for assignments
- Support for arithmetic expressions
- Support for comparison expressions
- Support for boolean logic
- Support for if/else statements
- Support for loop statements
- Support for indentation-based blocks
- Support for strings, numbers, booleans, characters, objects, and references
- Built-in console output support
- Syntax error handling with line and character position tracking
- JUnit tests for lexer, parser, and interpreter behavior

## Project Files

### Core Tran Package

- `Lexer.java` - reads Tran source code and converts it into a list of tokens
- `Parser.java` - parses tokens into an Abstract Syntax Tree using Tran grammar rules
- `Token.java` - defines token types such as words, numbers, keywords, punctuation, indentation, strings, and characters
- `TokenManager.java` - manages the token stream during parsing
- `TextManager.java` - manages character-by-character reading of source text
- `SyntaxErrorException.java` - custom exception used for syntax errors
- `Error.java` - small test file used for simple output testing

### AST Package

- `TranNode.java` - root AST node containing all parsed classes and interfaces
- `ClassNode.java` - represents a Tran class
- `InterfaceNode.java` - represents a Tran interface
- `ConstructorNode.java` - represents a class constructor
- `MethodDeclarationNode.java` - represents a full method declaration
- `MethodHeaderNode.java` - represents a method header
- `MemberNode.java` - represents a class member variable
- `VariableDeclarationNode.java` - represents a variable declaration
- `VariableReferenceNode.java` - represents a variable reference
- `AssignmentNode.java` - represents an assignment statement
- `IfNode.java` - represents an if statement
- `ElseNode.java` - represents an else block
- `LoopNode.java` - represents a loop statement
- `MethodCallExpressionNode.java` - represents a method call used as an expression
- `MethodCallStatementNode.java` - represents a method call used as a statement
- `NewNode.java` - represents object creation
- `MathOpNode.java` - represents arithmetic operations
- `CompareNode.java` - represents comparison operations
- `BooleanOpNode.java` - represents boolean operations
- `NotOpNode.java` - represents logical NOT
- `NumericLiteralNode.java` - represents numeric literals
- `StringLiteralNode.java` - represents string literals
- `CharLiteralNode.java` - represents character literals
- `BooleanLiteralNode.java` - represents boolean literals
- `ExpressionNode.java` - base interface for expression nodes
- `StatementNode.java` - base interface for statement nodes
- `Node.java` - base interface for AST nodes and shared formatting helpers

### Interpreter Package

- `Interpreter.java` - executes parsed Tran programs from the AST
- `InterpreterDataType.java` - base interface for runtime data types
- `NumberIDT.java` - runtime number value
- `StringIDT.java` - runtime string value
- `BooleanIDT.java` - runtime boolean value
- `CharIDT.java` - runtime character value
- `ObjectIDT.java` - runtime object value with member variables
- `ReferenceIDT.java` - runtime reference value
- `ConsoleWrite.java` - built-in console output method

### Test Files

- Lexer tests validate words, numbers, keywords, punctuation, indentation, comments, strings, characters, and syntax errors
- Parser tests validate interfaces, classes, constructors, members, methods, method calls, expressions, loops, if/else statements, and return values
- Interpreter tests validate execution of simple programs, object creation, method calls, loops, arithmetic, and console output

## How It Works

The project begins with the `Lexer`, which reads Tran source code character by character using the `TextManager`. It identifies words, keywords, numbers, punctuation, quoted strings, quoted characters, comments, indentation, dedentation, and newlines. The lexer then produces a list of `Token` objects.

The `Parser` takes the token list and uses the `TokenManager` to process the stream of tokens. It builds an Abstract Syntax Tree containing classes, interfaces, constructors, methods, members, statements, and expressions.

The AST represents the structure of the program. For example, classes are stored as `ClassNode` objects, methods are stored as `MethodDeclarationNode` objects, assignments are stored as `AssignmentNode` objects, and expressions are represented using nodes such as `MathOpNode`, `CompareNode`, `MethodCallExpressionNode`, and `VariableReferenceNode`.

The `Interpreter` takes the completed AST and runs the program. It searches for a shared `start` method, creates local variables, evaluates expressions, executes statements, handles method calls, creates objects, assigns values, runs loops, processes if/else blocks, and supports console output through a built-in `console.write` method.

## Supported Language Features

The Tran language supports class-based programming concepts such as:

- Classes
- Interfaces
- Constructors
- Member variables
- Methods
- Shared methods
- Private methods
- Object creation
- Method calls
- Multiple return values
- Local variables
- Assignments
- Arithmetic
- Comparisons
- If/else statements
- Loops
- Console output

## Example Tran-Style Code

```text
class SimpleAdd
    number x
    number y

    construct()
        x = 6
        y = 6

    add()
        number z
        z = x + y
        console.write(z)

    shared start()
        SimpleAdd t
        t = new SimpleAdd()
        t.add()
```

## Technologies Used

- Java
- JUnit
- Object-oriented programming
- Lexical analysis
- Parsing
- Abstract Syntax Trees
- Recursive descent parsing
- Interpreter design
- Custom runtime data types
- Syntax error handling
- Token stream processing

## Purpose

This project was created for a Principles of Programming Languages course to practice how programming languages are designed and implemented.

The project demonstrates how source code can be processed through multiple stages: reading raw text, generating tokens, parsing grammar rules, building an Abstract Syntax Tree, and interpreting the program. It also reinforces concepts such as language syntax, runtime values, scoping, method calls, object references, control flow, and error handling.
