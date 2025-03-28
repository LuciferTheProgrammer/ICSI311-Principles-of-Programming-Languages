package Tran;
import AST.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

// The Parser Class takes in a stream of tokens, using the EBNF rules for the Tran Program, generates and returns
// an Optional<> of a specific AST Node.
public class Parser {

    // The TokenManager instance field to store the list of tokens to be processed.
    private TokenManager manageTokens;

    // The TranNode instance field to store the list of classes and interfaces.
    private TranNode currentNode;

    /**
     * The constructor takes in a TranNode and a list of tokens and sets the TranNode instance field as well
     * as the TokenManager instance field.
     *
     * @param top    The TranNode.
     * @param tokens The list of tokens.
     */
    public Parser(TranNode top, List<Token> tokens) {
        currentNode = top;
        manageTokens = new TokenManager(tokens);
    }

    /**
     * This method reads and expects a new line token after each method header, if there is none it throws
     * a syntax error. However, if the last token is a dedent token, then the Parser doesn't expect a new line
     * token and therefore no error is thrown.
     *
     * @throws SyntaxErrorException When no new line token is present after the method header.
     */
    public void RequireNewLine() throws SyntaxErrorException {
        if ((manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty())) {
            if ((!manageTokens.done()) && (manageTokens.getSpecificToken(0) == Token.TokenTypes.DEDENT))
                return;
            throw new SyntaxErrorException("Expected a newline", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while (manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) ;
    }

    /**
     * This method adds a list of interfaces and classes to the TranNode instance field while the list of
     * tokens is being processed through the TokenManager instance field.
     *
     * @throws SyntaxErrorException When an error occurs in the program.
     */
    public void Tran() throws SyntaxErrorException {
        while (!manageTokens.done()) {
            if (manageTokens.getSpecificToken(0) == Token.TokenTypes.NEWLINE) {
                manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE);
            }
            Optional<InterfaceNode> holder = interfaceStatement();
            if (holder.isPresent()) {
                currentNode.Interfaces.add(holder.get());
            } else {
                Optional<ClassNode> holder2 = classStatements();
                if (holder2.isPresent()) {
                    currentNode.Classes.add(holder2.get());
                }
            }
        }
    }

    /**
     * This method creates an interface node, then checks the list of tokens for the presence of an
     * interface keyword, name of the interface, a new line, proper indentation levels, method headers with
     * the appropriate parameter variables and return types. If there is an absence of these properties
     * in the interface, then a syntax error is thrown. When the process is complete, all the methods
     * would be added to the interface node which is then returned.
     *
     * @return The Interface Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<InterfaceNode> interfaceStatement() throws SyntaxErrorException {
        InterfaceNode node = new InterfaceNode();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.INTERFACE).isEmpty()))
            return Optional.empty();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()))
            throw new SyntaxErrorException("Interface must have a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        node.name = manageTokens.getCurrentText();
        RequireNewLine();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()))
            throw new SyntaxErrorException("Interface must have an indentation", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        while ((manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) && !(manageTokens.getToken().isEmpty())) {
            Optional<MethodHeaderNode> test = methodHeaders();
            if ((test.isEmpty())) {
                throw new SyntaxErrorException("Interface must have method with a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            } else {
                MethodHeaderNode method = test.get();
                node.methods.add(method);
            }
        }
        if ((manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())) {
            throw new SyntaxErrorException("Interface expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        return Optional.of(node);
    }

    /**
     * This method creates a method header node, then checks the list of tokens for the presence of the name
     * of the method, a left parenthesis, then parameter variables, right parenthesis, a colon (optional),
     * but a colon is required with return variables (optional), and then finally a new line. If there is an
     * absence of the required properties, then a syntax error is thrown. When the process is complete all the
     * properties would be added to the method header node which is then returned.
     *
     * @return The Method Header Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<MethodHeaderNode> methodHeaders() throws SyntaxErrorException {
        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()))
            return Optional.empty();
        methodHeaderNode.name = manageTokens.getCurrentText();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()))
            throw new SyntaxErrorException("Method must have a left parentheses", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        checkParameterVariableSetUp(methodHeaderNode);
        if ((manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()))
            throw new SyntaxErrorException("Method must have a right parentheses", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        // Optional for methods on an interface and class.
        if ((manageTokens.matchAndRemove(Token.TokenTypes.COLON).isPresent())) {
            checkReturnVariableSetUp(methodHeaderNode);
        }
        RequireNewLine();
        return Optional.of(methodHeaderNode);
    }

    /**
     * This method  creates a variable declaration node, then checks the list of tokens for the type and name
     * of the variable and then proceeds to store those values to the variable declaration node which is
     * then returned.
     *
     * @return The variable declaration node.
     */
    public Optional<VariableDeclarationNode> variableDeclarations() {
        VariableDeclarationNode variableDeclarationNode = new VariableDeclarationNode();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent())) {
            variableDeclarationNode.type = manageTokens.getCurrentText();
            if ((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent())) {
                variableDeclarationNode.name = manageTokens.getCurrentText();
                return Optional.of(variableDeclarationNode);
            }
        }
        return Optional.empty();
    }

    /**
     * This method takes in a method header node and checks the list of tokens for variable declarations, for
     * each variable declaration is stored in the parameters list of the method header node. This method
     * uses a comma as a separator for the variable declarations. If there is a mismatch a syntax error is thrown.
     *
     * @param sample The Method Header Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public void checkParameterVariableSetUp(MethodHeaderNode sample) throws SyntaxErrorException {
        Optional<VariableDeclarationNode> holder = variableDeclarations();
        if (holder.isPresent()) {
            sample.parameters.add(holder.get());
            while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                Optional<VariableDeclarationNode> holder2 = variableDeclarations();
                if (holder2.isPresent()) {
                    sample.parameters.add(holder2.get());
                } else
                    throw new SyntaxErrorException("Method needs a correct parameter variable declaration", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
        }
    }

    /**
     * This method takes in a method header node and checks the list of tokens for variable declarations, for
     * each variable declaration is stored in the returns list of the method header node. This method
     * uses a comma as a separator for the variable declarations. If there is a mismatch a syntax error is thrown.
     *
     * @param sample The Method Header Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public void checkReturnVariableSetUp(MethodHeaderNode sample) throws SyntaxErrorException {
        Optional<VariableDeclarationNode> holder = variableDeclarations();
        if (holder.isPresent()) {
            sample.returns.add(holder.get());
            while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                Optional<VariableDeclarationNode> holder2 = variableDeclarations();
                if (holder2.isPresent()) {
                    sample.returns.add(holder2.get());
                } else
                    throw new SyntaxErrorException("Method needs a correct return variable declaration", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
        }
    }

    /**
     * This method creates a class node, then checks the list of tokens for a presence of the keyword class,
     * the name, while checking for interfaces that are being implemented (optional), a new line, an indent,
     * checking for the presence of constructors, methods, and members, and finally for the presence of
     * a dedent. All of these properties are then proceeded to be added to the class node.
     * This method also returns a class node.
     *
     * @return The class node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<ClassNode> classStatements() throws SyntaxErrorException {
        ClassNode classNode = new ClassNode();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.CLASS).isEmpty())) {
            return Optional.empty();
        }
        if ((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty())) {
            throw new SyntaxErrorException("Class must have a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        classNode.name = manageTokens.getCurrentText();
        checkForInterfaces(classNode);
        RequireNewLine();
        if (manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Class must have a proper indentation", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while ((manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) && !(manageTokens.getToken().isEmpty())) {
            Optional<ConstructorNode> constructorNode = constructor();
            if (constructorNode.isPresent()) {
                classNode.constructors.add(constructorNode.get());
                continue;
            }
            if(manageTokens.getSpecificToken(0) == Token.TokenTypes.SHARED ||
                    manageTokens.getSpecificToken(0) == Token.TokenTypes.PRIVATE) {
                Optional<MethodDeclarationNode> md = methodDeclaration();
                if (md.isPresent()) {
                    classNode.methods.add(md.get());
                    continue;
               }
            }
           if(manageTokens.getSpecificToken(0) == Token.TokenTypes.WORD) {
                if ((manageTokens.getTokenSize() > 1) && (manageTokens.getSpecificToken(1) == Token.TokenTypes.LPAREN)) {
                    Optional<MethodDeclarationNode> md = methodDeclaration();
                    if (md.isPresent()) {
                        classNode.methods.add(md.get());
                        continue;
                    }
                } else {
                    List<MemberNode> memberNode = members();
                    if (!memberNode.isEmpty()) {
                        classNode.members.addAll(memberNode);
                        continue;
                    }
                }
            }
            throw new SyntaxErrorException("Class expected a constructor, member, or method", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if ((manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())) {
            throw new SyntaxErrorException("Class expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        return Optional.of(classNode);
    }

    /**
     * This method takes in a class node and checks for the keyword implements, and finally
     * the name(s) of the interfaces that are being implemented. All of these properties are then proceeded
     * to be added to the class node.
     *
     * @param sample The class node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public void checkForInterfaces(ClassNode sample) throws SyntaxErrorException {
        if ((manageTokens.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isEmpty())) {
            return;
        }
        if ((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty())) {
            throw new SyntaxErrorException("Class must implement an interface with a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        sample.interfaces.add(manageTokens.getCurrentText());
        while ((manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent())) {
            if (manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()) {
                sample.interfaces.add(manageTokens.getCurrentText());
            } else
                throw new SyntaxErrorException("Class must implement an interface with a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
    }

    /**
     * This method creates a constructor node, then proceeds to check for the presence of the keyword construct,
     * a left parentheses, parameter variable declarations (optional), a right parentheses, a new line, and
     * a method body. All of these properties are then proceeded to be added to the constructor node. Finally,
     * this method also returns the constructor node.
     *
     * @return The constructor node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<ConstructorNode> constructor() throws SyntaxErrorException {
        ConstructorNode constructorNode = new ConstructorNode();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.CONSTRUCT).isEmpty())) {
            return Optional.empty();
        }
        if ((manageTokens.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty())) {
            throw new SyntaxErrorException("Constructor must have a left parentheses", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        checkParameterConstructor(constructorNode);
        if (manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
            throw new SyntaxErrorException("Constructor must have a right parentheses", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        RequireNewLine();
        methodBodyConstructor(constructorNode);
        return Optional.of(constructorNode);
    }

    /**
     * This method takes in a constructor node which checks for the presence of parameter variable declarations.
     * All of these properties are then proceeded to be added to the constructor node.
     *
     * @param sample The constructor node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public void checkParameterConstructor(ConstructorNode sample) throws SyntaxErrorException {
        Optional<VariableDeclarationNode> holder = variableDeclarations();
        if (holder.isPresent()) {
            sample.parameters.add(holder.get());
            while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                Optional<VariableDeclarationNode> holder2 = variableDeclarations();
                if (holder2.isPresent()) {
                    sample.parameters.add(holder2.get());
                } else
                    throw new SyntaxErrorException("Constructor needs a correct parameter variable declaration", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
        }
    }

    /**
     * This method creates a method declaration node that checks for the keywords shared or private as
     * potential access specifiers (optional), then a method header, and then for a method body. All
     * of these properties are then proceeded to be added to the method declaration node. This method
     * also returns a method declaration node.
     *
     * @return The method declaration node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<MethodDeclarationNode> methodDeclaration() throws SyntaxErrorException {
        MethodDeclarationNode methodDeclarationNode = new MethodDeclarationNode();
        while(manageTokens.getSpecificToken(0) == Token.TokenTypes.SHARED ||
        manageTokens.getSpecificToken(0) == Token.TokenTypes.PRIVATE) {
            if (manageTokens.matchAndRemove(Token.TokenTypes.SHARED).isPresent()) {
                methodDeclarationNode.isShared = true;
            }
            else if(manageTokens.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent()) {
                methodDeclarationNode.isPrivate = true;
            }
        }
        Optional<MethodHeaderNode> methodHeader = methodHeaders();
        if (methodHeader.isEmpty()) {
            throw new SyntaxErrorException("Method declaration expected a method header", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        methodDeclarationNode.name = methodHeader.get().name;
        methodDeclarationNode.parameters = methodHeader.get().parameters;
        methodDeclarationNode.returns = methodHeader.get().returns;
        methodBodyMD(methodDeclarationNode);
        return Optional.of(methodDeclarationNode);
    }

    /**
     * This method creates a list of member nodes which checks for the presence of variable declarations.
     * Which are then proceeded to be added to the list of member nodes. This method also returns the list
     * of member nodes.
     *
     * @return The list of member nodes.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public List<MemberNode> members() throws SyntaxErrorException {
        List<MemberNode> memberNodes = new ArrayList<>();
        List<VariableDeclarationNode> holder = multipleVariableDeclarations();
        if (holder.isEmpty()) {
            return memberNodes;
        }
        for (VariableDeclarationNode variableDeclarationNode : holder) {
            MemberNode placement = new MemberNode();
            placement.declaration = variableDeclarationNode;
            memberNodes.add(placement);
        }
        return memberNodes;
    }

    /**
     * This method creates a list of variable declaration nodes which checks for the presence of the variable type,
     * then the variable name, then all the subsequent variable names that come after (optional), and finally
     * a new line. All of these properties are then proceeded to be added to the list of variable declaration nodes
     * This method also returns the list of variable declaration nodes.
     *
     * @return The list of variable declaration nodes.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public List<VariableDeclarationNode> multipleVariableDeclarations() throws SyntaxErrorException {
        List<VariableDeclarationNode> multivariable = new ArrayList<>();
        if (manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty())
            return multivariable;
        String typeName = manageTokens.getCurrentText();
        VariableDeclarationNode variableDeclarationNode = new VariableDeclarationNode();
        variableDeclarationNode.type = typeName;
        Optional<VariableDeclarationNode> put = VariableNameValue(variableDeclarationNode);
        if (put.isEmpty())
            return multivariable;
        multivariable.add(put.get());
        while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            VariableDeclarationNode sample = new VariableDeclarationNode();
            sample.type = typeName;
            Optional<VariableDeclarationNode> placement = VariableNameValue(sample);
            multivariable.add(placement.get());
        }
        RequireNewLine();
        return multivariable;
    }

    /**
     * This method takes in a variable declaration node which then proceeds to check for the presence of the
     * variable name. This property is then proceeded to be added to the variable declaration node. Then
     * this method returns the variable declaration node.
     *
     * @param sample The variable declaration node.
     * @return The variable declaration node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<VariableDeclarationNode> VariableNameValue(VariableDeclarationNode sample) throws SyntaxErrorException {
        if (manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()) {
            throw new SyntaxErrorException("Expected to have a variable name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        sample.name = manageTokens.getCurrentText();
        if(manageTokens.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()) {
            Optional<ExpressionNode> expressionContainer = Expression();
            if (expressionContainer.isPresent()) {
                sample.initializer = expressionContainer;
            }
            else
                throw new SyntaxErrorException("Expected an expression" , manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        return Optional.of(sample);
    }

    /**
     * This method takes in a constructor node which then checks for the presence of an indent, local
     * variables, statements, and finally a dedent. All of these properties are then proceeded to be added
     * to the constructor node which is then returned.
     *
     * @param sample The constructor node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public void methodBodyConstructor(ConstructorNode sample) throws SyntaxErrorException {
        if (manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Method body expected an indent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while (manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) {
            if(manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
                List<VariableDeclarationNode> list = multipleVariableDeclarations();
                if (!list.isEmpty()) {
                    sample.locals.addAll(list);
                    continue;
                }
            }
            Optional<StatementNode> collector = statement();
            if (collector.isPresent()) {
                sample.statements.add(collector.get());
                continue;
            }
            throw new SyntaxErrorException("Method expected a statement", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if (manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Method body expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
    }

    /**
     * This method takes in a method declaration node which then checks for the presence of an indent, local
     * variables, statements, and finally a dedent. All of these properties are then proceeded to be added
     * to the method declaration node which is then returned.
     *
     * @param sample The method declaration node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public void methodBodyMD(MethodDeclarationNode sample) throws SyntaxErrorException {
        if (manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Method body expected an indent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while (manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) {
            if(manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
                List<VariableDeclarationNode> list = multipleVariableDeclarations();
                if (!list.isEmpty()) {
                    sample.locals.addAll(list);
                    continue;
                }
            }
            Optional<StatementNode> collector = statement();
            if (collector.isPresent()) {
                sample.statements.add(collector.get());
                continue;
            }
            throw new SyntaxErrorException("Method expected a statement", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if (manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Method body expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
    }

    /**
     * This method creates a list of statement nodes which checks for the presence of an indent,
     * then statement(s), and finally a dedent. All of these properties are then proceeded to be added
     * to the list of statement nodes which is then returned.
     *
     * @return The list of statement nodes.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public List<StatementNode> Statements() throws SyntaxErrorException {
        List<StatementNode> statements = new ArrayList<>();
        if (manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Indent Expected on statements", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while (manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) {
            Optional<StatementNode> statement = statement();
            if (statement.isPresent()) {
                statements.add(statement.get());
            }
        }
        if (manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            throw new SyntaxErrorException("Statement expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        return statements;
    }

    /**
     * This method checks for the presence of either an if statement or a loop statement. If both are not
     * present then the Statement Node generated and returned from the disambiguate method will be returned.
     *
     * @return The Statement Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<StatementNode> statement() throws SyntaxErrorException {
        if (manageTokens.matchAndRemove(Token.TokenTypes.IF).isPresent()) {
            Optional<IfNode> ifHolder = ifStatement();
            if (ifHolder.isPresent()) {
                return Optional.of(ifHolder.get());
            }
        }
        else if (manageTokens.matchAndRemove(Token.TokenTypes.LOOP).isPresent()) {
            Optional<LoopNode> loopHolder = loopStatement();
            if (loopHolder.isPresent()) {
                return Optional.of(loopHolder.get());
            }
        }
        else {
            Optional<StatementNode> toHold = disambiguate();
            if(toHold.isPresent()) {
                return toHold;
            }
        }
        return Optional.empty();
    }

    /**
     * This method creates an if statement node then checks for a boolean op node through BoolExpTerm(),
     * a new line, and statement(s), along with an (optional) else followed by a branching statement(s).
     * All of these properties are then proceeded to be added to the if statement node which is then returned.
     *
     * @return The if statement node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<IfNode> ifStatement() throws SyntaxErrorException {
        IfNode holder = new IfNode();
        Optional<ExpressionNode> bool = BoolExpTerm();
        if (bool.isPresent()) {
            holder.condition = bool.get();
        } else
            throw new SyntaxErrorException("Expected an expression", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        RequireNewLine();
        List<StatementNode> statements = Statements();
        if (statements.isEmpty()) {
            throw new SyntaxErrorException("Statement expected", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        } else
            holder.statements = statements;
        holder.elseStatement = Optional.empty();
        if (manageTokens.matchAndRemove(Token.TokenTypes.ELSE).isPresent()) {
            ElseNode elseNode = new ElseNode();
            RequireNewLine();
            List<StatementNode> elseStatements = Statements();
            if (elseStatements.isEmpty()) {
                throw new SyntaxErrorException("Else statement expected", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            elseNode.statements = elseStatements;
            holder.elseStatement = Optional.of(elseNode);
        }
        return Optional.of(holder);
    }

    /**
     * This method creates a loop statement node which then checks for the presence of a variable reference
     * node (optional), then for an assignment statement ("=") (optional), for a boolean op node through BoolExpTerm(),
     * a new line, and finally for statement(s). All of these properties are then proceeded to be added to the loop
     * statement node which is then returned.
     *
     * @return The loop statement node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<LoopNode> loopStatement() throws SyntaxErrorException {
        LoopNode holder = new LoopNode();
        if (manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)) {
            holder.assignment = VariableReference();
            manageTokens.matchAndRemove(Token.TokenTypes.ASSIGN);
        }
        Optional<ExpressionNode> bool = BoolExpTerm();
        if (bool.isPresent()) {
            holder.expression = bool.get();
        } else
            throw new SyntaxErrorException("Expected an expression", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        RequireNewLine();
        List<StatementNode> statements = Statements();
        if (statements.isEmpty()) {
            throw new SyntaxErrorException("Statement expected", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        } else
            holder.statements = statements;
        return Optional.of(holder);
    }

    /**
     * This method creates and returns a Variable Reference Node (temporarily -> to be modified later).
     *
     * @return The Variable Reference Node.
     */
    public Optional<ExpressionNode> Expression() throws SyntaxErrorException {
        Optional<ExpressionNode> term1 = Term();
        if(term1.isPresent()) {
            while(manageTokens.getSpecificToken(0) == Token.TokenTypes.PLUS || manageTokens.getSpecificToken(0) == Token.TokenTypes.MINUS) {
                MathOpNode mathOp = new MathOpNode();
                mathOp.left = term1.get();
                Token.TokenTypes operator = manageTokens.getSpecificToken(0);
                switch(operator) {
                    case PLUS -> {
                        mathOp.op = MathOpNode.MathOperations.add;
                        manageTokens.matchAndRemove(Token.TokenTypes.PLUS);
                    }
                    case MINUS -> {
                        mathOp.op = MathOpNode.MathOperations.subtract;
                        manageTokens.matchAndRemove(Token.TokenTypes.MINUS);
                    }
                }
                Optional<ExpressionNode> term2 = Term();
                if(term2.isPresent()) {
                    mathOp.right = term2.get();
                }
                else
                    throw new SyntaxErrorException("Expected a right expression", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
                term1 = Optional.of(mathOp);
            }
            return term1;
        }
        return Optional.empty();
    }

    /**
     * This method either returns a Method Call Expression Node, Compare Node, or Variable Reference Node.
     * If none of these options are present then the method returns empty.
     *
     * @return The instance of a class that implements the Expression Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<ExpressionNode> BoolExpTerm() throws SyntaxErrorException {
        BooleanOpNode boolOp = new BooleanOpNode();
        if(manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN) ||
                manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) {
            Optional<MethodCallExpressionNode> methodCallExpressionNode = MethodCallExpression();
            if (methodCallExpressionNode.isPresent()) {
                MethodCallExpressionNode methodCallExpression = methodCallExpressionNode.get();
                return Optional.of(methodCallExpression);
            }
        }
        Optional<ExpressionNode> expression1 = Expression();
        if (expression1.isPresent()) {
            CompareNode holder = new CompareNode();
            holder.left = expression1.get();
            if (manageTokens.getSpecificToken(0) == Token.TokenTypes.EQUAL) {
                Token.TokenTypes container = manageTokens.getSpecificToken(0);
                BoolExpFactor(holder, container);
            }
            else if (manageTokens.getSpecificToken(0) == Token.TokenTypes.NOTEQUAL) {
                Token.TokenTypes container = manageTokens.getSpecificToken(0);
                BoolExpFactor(holder, container);
            }
            else if (manageTokens.getSpecificToken(0) == Token.TokenTypes.LESSTHANEQUAL) {
                Token.TokenTypes container = manageTokens.getSpecificToken(0);
                BoolExpFactor(holder, container);
            }
            else if (manageTokens.getSpecificToken(0) == Token.TokenTypes.GREATERTHANEQUAL) {
                Token.TokenTypes container = manageTokens.getSpecificToken(0);
                BoolExpFactor(holder, container);
            }
            else if (manageTokens.getSpecificToken(0) == Token.TokenTypes.GREATERTHAN) {
                Token.TokenTypes container = manageTokens.getSpecificToken(0);
                BoolExpFactor(holder, container);
            }
            else if (manageTokens.getSpecificToken(0) == Token.TokenTypes.LESSTHAN) {
                Token.TokenTypes container = manageTokens.getSpecificToken(0);
                BoolExpFactor(holder, container);
            }
            if(holder.op != null) {
                Optional<ExpressionNode> expression2 = Expression();
                if (expression2.isPresent()) {
                    holder.right = expression2.get();
                    return Optional.of(holder);
                }
                else
                    throw new SyntaxErrorException("Expected a right expression", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            return Optional.of(expression1.get());
        }
        Optional<VariableReferenceNode> take = VariableReference();
        if (take.isPresent()) {
            VariableReferenceNode variableReferenceNode = take.get();
            return Optional.of(variableReferenceNode);
        }
        return Optional.empty();
    }

    /**
     * This method takes in a Compare Node and a Token Type. Then, based on the Token Type,
     * the method sets the CompareOperations instance field of the Compare Node
     * to the corresponding/matching enum data type regarding the type of comparison operator used.
     *
     * @param sample The Compare Node.
     * @param operator The Token Type.
     */
    public void BoolExpFactor(CompareNode sample, Token.TokenTypes operator) {
        switch (operator) {
            case EQUAL -> {
                sample.op = CompareNode.CompareOperations.eq;
                manageTokens.matchAndRemove(Token.TokenTypes.EQUAL);
            }
            case NOTEQUAL -> {
                sample.op = CompareNode.CompareOperations.ne;
                manageTokens.matchAndRemove(Token.TokenTypes.NOTEQUAL);
            }
            case LESSTHANEQUAL -> {
                sample.op = CompareNode.CompareOperations.le;
                manageTokens.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL);
            }
            case GREATERTHANEQUAL -> {
                sample.op = CompareNode.CompareOperations.ge;
                manageTokens.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL);
            }
            case GREATERTHAN -> {
                sample.op = CompareNode.CompareOperations.gt;
                manageTokens.matchAndRemove(Token.TokenTypes.GREATERTHAN);
            }
            case LESSTHAN -> {
                sample.op = CompareNode.CompareOperations.lt;
                manageTokens.matchAndRemove(Token.TokenTypes.LESSTHAN);
            }
        }
    }

    /**
     * This method creates a Method Call Expression Node which passes it as a parameter to a constructor to
     * create a Method Call Statement Node. Then, keeps a list of Variable Reference Nodes which are then added
     * to the properties of the Method Call Statement Node (return values) at the end while also consuming a NEW LINE
     * token. Finally, the Method Call Statement Node is returned.
     *
     * @return The Method Call Statement Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<MethodCallStatementNode> MethodCall() throws SyntaxErrorException {
        MethodCallStatementNode methodCall;
        List<VariableReferenceNode> references = new ArrayList<>();
        if (manageTokens.getSpecificToken(0) == Token.TokenTypes.WORD) {
            Optional<VariableReferenceNode> reference = VariableReference();
            references.add(reference.get());
            while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                if (manageTokens.getSpecificToken(0) == Token.TokenTypes.WORD) {
                    Optional<VariableReferenceNode> variableReference = VariableReference();
                    references.add(variableReference.get());
                }
                else
                    throw new SyntaxErrorException("Expected a variable reference name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            if (manageTokens.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()) {
                throw new SyntaxErrorException("Expected an assignment statement", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
        }
        Optional<MethodCallExpressionNode> holder = MethodCallExpression();
        MethodCallExpressionNode container = holder.get();
        methodCall = new MethodCallStatementNode(container);
        methodCall.returnValues.addAll(references);
        RequireNewLine();
        return Optional.of(methodCall);
    }

    /**
     * This method creates an Assignment Node which takes in a Variable Reference Node while consuming
     * an ASSIGN token ("=") and then takes in an Expression. Finally, it then consumes a NEW LINE token and
     * returns the Assignment Node. If there is no Variable Reference Node from the start,
     * then it returns empty.
     *
     * @return The Assignment Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<AssignmentNode> Assignment() throws SyntaxErrorException {
        AssignmentNode assignment = new AssignmentNode();
        if(manageTokens.getSpecificToken(0) == Token.TokenTypes.WORD) {
            Optional<VariableReferenceNode> taker = VariableReference();
            assignment.target = taker.get();
            if (manageTokens.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()) {
                throw new SyntaxErrorException("Expected an assignment statement", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            Optional<ExpressionNode> expressionContainer = Expression();
            if (expressionContainer.isEmpty()) {
                throw new SyntaxErrorException("Expected an expression", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            assignment.expression = expressionContainer.get();
            RequireNewLine();
            return Optional.of(assignment);
        }
        return Optional.empty();
    }

    /**
     * This method calls for a Method Call Expression Node. If it's present, then it creates a Method
     * Call Statement Node from it and that is returned. If that case is not true, then a Variable Reference
     * Node is checked, if there is no corresponding value then the method returns empty. However, if a Variable
     * Reference Node is present then it checks for the case of Method Call, which again generates and returns
     * a Method Call Statement Node when there is a COMMA token that follows the Variable Reference Node and
     * if not then it generates and returns an Assignment Node instead.
     *
     * @return The Statement Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<StatementNode> disambiguate() throws SyntaxErrorException {
        if(manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN) ||
        manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) {
            Optional<MethodCallExpressionNode> methodCallExpression = MethodCallExpression();
            if (methodCallExpression.isPresent()) {
                MethodCallStatementNode mcs = new MethodCallStatementNode(methodCallExpression.get());
                RequireNewLine();
                return Optional.of(mcs);
            }
        }
        if(manageTokens.getSpecificToken(0) != Token.TokenTypes.WORD) {
            return Optional.empty();
        }
        else {
            Optional<Token> token = manageTokens.peek(1);
            Token take = token.get();
            if(take.getType() == Token.TokenTypes.COMMA || (take.getType() == Token.TokenTypes.ASSIGN &&
                    manageTokens.getSpecificToken(2) == Token.TokenTypes.WORD &&  manageTokens.getSpecificToken(3) == Token.TokenTypes.DOT)
                    || (take.getType() == Token.TokenTypes.ASSIGN && manageTokens.getSpecificToken(2) == Token.TokenTypes.WORD &&
                    manageTokens.getSpecificToken(3) == Token.TokenTypes.LPAREN)) {
                Optional<MethodCallStatementNode> mst = MethodCall();
                MethodCallStatementNode contained = mst.get();
                return Optional.of(contained);
            }
            else {
                Optional<AssignmentNode> assignment = Assignment();
                AssignmentNode taken = assignment.get();
                return Optional.of(taken);
            }
        }
    }

    /**
     * This method generates and returns a Method Call Expression Node.
     * (To be modified later, currently returns empty).
     *
     * @return The Method Call Expression Node.
     */
    public Optional<MethodCallExpressionNode> MethodCallExpression() throws SyntaxErrorException {
        MethodCallExpressionNode mce = new MethodCallExpressionNode();
        if(manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) {
            manageTokens.matchAndRemove(Token.TokenTypes.WORD);
            String nameHolder = manageTokens.getCurrentText();
            mce.objectName = Optional.of(nameHolder);
            manageTokens.matchAndRemove(Token.TokenTypes.DOT);
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()) {
            return Optional.empty();
        }
        mce.methodName = manageTokens.getCurrentText();
        if(manageTokens.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()) {
            throw new SyntaxErrorException("Expected a left parenthesis", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        Optional<ExpressionNode> expressionContainer = Expression();
        if (expressionContainer.isPresent()) {
            mce.parameters.add(expressionContainer.get());
            while(manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                Optional<ExpressionNode> expressionContainer2 = Expression();
                if(expressionContainer2.isPresent()) {
                    mce.parameters.add(expressionContainer2.get());
                }
                else
                    throw new SyntaxErrorException("Expected an expression", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
            throw new SyntaxErrorException("Expected a right parenthesis", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        return Optional.of(mce);
    }

    /**
     * This method creates a Variable Reference Node, then checks for the presence of a WORD token.
     * If there is a WORD token present then the value of the token is set to the property of the Variable
     * Reference Node (name field), and the Variable Reference Node is returned. Otherwise, it returns
     * empty.
     *
     * @return The Variable Reference Node.
     */
    public Optional<VariableReferenceNode> VariableReference() {
        VariableReferenceNode holder = new VariableReferenceNode();
        if (manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()) {
            holder.name = manageTokens.getCurrentText();
            return Optional.of(holder);
        }
        return Optional.empty();
    }

    /**
     * This method creates an Expression Node which is used to store the value returned from the
     * Factor method. Then this method is used to check if the Expression Node has a value. If it has
     * a value then a Math Op Node is created which is used to store the left hand Expression Node,
     * then stores the Math Operator, and then the right hand Expression Node. If there is no Math Operator
     * then the Expression Node is just returned. Otherwise, if there is no Expression Node present,
     * an empty value if returned.
     *
     * @return The Expression Node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<ExpressionNode> Term() throws SyntaxErrorException {
        Optional<ExpressionNode> factorHolder = Factor();
        if (factorHolder.isPresent()) {
            while(manageTokens.getSpecificToken(0) == Token.TokenTypes.TIMES || manageTokens.getSpecificToken(0) == Token.TokenTypes.DIVIDE || manageTokens.getSpecificToken(0) == Token.TokenTypes.MODULO) {
                MathOpNode mathOp = new MathOpNode();
                mathOp.left = factorHolder.get();
                Token.TokenTypes operator = manageTokens.getSpecificToken(0);
                switch(operator) {
                  case TIMES -> {
                      mathOp.op = MathOpNode.MathOperations.multiply;
                      manageTokens.matchAndRemove(Token.TokenTypes.TIMES);
                  }
                  case DIVIDE -> {
                      mathOp.op = MathOpNode.MathOperations.divide;
                      manageTokens.matchAndRemove(Token.TokenTypes.DIVIDE);
                  }
                  case MODULO -> {
                      mathOp.op = MathOpNode.MathOperations.modulo;
                      manageTokens.matchAndRemove(Token.TokenTypes.MODULO);
                  }
              }
              Optional<ExpressionNode> factorHolder2 = Factor();
                if (factorHolder2.isPresent()) {
                    mathOp.right = factorHolder2.get();
                }
                else
                    throw new SyntaxErrorException("Expected a right expression", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
                factorHolder = Optional.of(mathOp);
            }
            return factorHolder;
        }
        return Optional.empty();
    }

    /**
     * This method first creates
     *
     * @return
     * @throws SyntaxErrorException
     */
    public Optional<ExpressionNode> Factor() throws SyntaxErrorException {
        if(manageTokens.matchAndRemove(Token.TokenTypes.NUMBER).isPresent()) {
            NumericLiteralNode numberHolder = new NumericLiteralNode();
            numberHolder.value = Float.parseFloat(manageTokens.getCurrentText());
            return Optional.of(numberHolder);
        }
        if(manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN) ||
                manageTokens.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) {
            Optional<MethodCallExpressionNode> mce = MethodCallExpression();
            if (mce.isPresent()) {
                MethodCallExpressionNode mceHolder = mce.get();
                return Optional.of(mceHolder);
            }
        }
        if(manageTokens.getSpecificToken(0) == Token.TokenTypes.WORD) {
            Optional<VariableReferenceNode> vr = VariableReference();
            VariableReferenceNode holder = vr.get();
            return Optional.of(holder);
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.QUOTEDSTRING).isPresent()) {
            StringLiteralNode stringHolder = new StringLiteralNode();
            stringHolder.value = manageTokens.getCurrentText();
            return Optional.of(stringHolder);
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER).isPresent()) {
            CharLiteralNode charHolder = new CharLiteralNode();
            charHolder.value = manageTokens.getCurrentText().charAt(0);
            return Optional.of(charHolder);
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            Optional<ExpressionNode> expressionContainer = Expression();
            if(expressionContainer.isPresent()) {
                ExpressionNode exp = expressionContainer.get();
                if(manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
                    throw new SyntaxErrorException("Expected a Right Parenthesis", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
                }
                return Optional.of(exp);
            }
            else
                throw new SyntaxErrorException("Expected an expression", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.NEW).isPresent()) {
            NewNode newHolder = new NewNode();
            if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()) {
                throw new SyntaxErrorException("Expected an instance", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            newHolder.className = manageTokens.getCurrentText();
            if(manageTokens.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()) {
                throw new SyntaxErrorException("Expected a Left Parenthesis", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            Optional<ExpressionNode> expressionTake = Expression();
            if(expressionTake.isPresent()) {
                newHolder.parameters.add(expressionTake.get());
                while(manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                    Optional<ExpressionNode> expressionTake2 = Expression();
                    if(expressionTake2.isPresent()) {
                        newHolder.parameters.add(expressionTake2.get());
                    }
                    else
                        throw new SyntaxErrorException("Expected an expression", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
                }
            }
            if(manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
                throw new SyntaxErrorException("Expected a Right Parenthesis", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            return Optional.of(newHolder);
        }
        return Optional.empty();
    }
}