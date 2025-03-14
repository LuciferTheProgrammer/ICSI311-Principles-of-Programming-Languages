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
     * @param top The TranNode.
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
        if((manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty())) {
            if((!manageTokens.done())&& (manageTokens.getSpecificToken(0) == Token.TokenTypes.DEDENT))
                return;
            throw new SyntaxErrorException("Expected a newline", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while(manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent());
    }

    /**
     * This method adds a list of interfaces and classes to the TranNode instance field while the list of
     * tokens is being processed through the TokenManager instance field.
     *
     * @throws SyntaxErrorException When an error occurs in the program.
     */
    public void Tran() throws SyntaxErrorException {
        while(!manageTokens.done()) {
            if(manageTokens.getSpecificToken(0)== Token.TokenTypes.NEWLINE) {
                manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE);
            }
            Optional<InterfaceNode> holder = interfaceStatement();
            if (holder.isPresent()) {
                currentNode.Interfaces.add(holder.get());
            }
            else {
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
        while((manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) && !(manageTokens.getToken().isEmpty())) {
            Optional<MethodHeaderNode> test = methodHeaders();
            if ((test.isEmpty())) {
                throw new SyntaxErrorException("Interface must have method with a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            else {
                MethodHeaderNode method = test.get();
                node.methods.add(method);
            }
        }
        if((manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())) {
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
        if((manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()))
            throw new SyntaxErrorException("Method must have a right parentheses", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        // Optional for methods on an interface and class.
        if((manageTokens.matchAndRemove(Token.TokenTypes.COLON).isPresent())) {
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
        if((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent())) {
            variableDeclarationNode.type = manageTokens.getCurrentText();
            if((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent())) {
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
        if(holder.isPresent()) {
            sample.parameters.add(holder.get());
            while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                Optional<VariableDeclarationNode> holder2 = variableDeclarations();
                if (holder2.isPresent()) {
                    sample.parameters.add(holder2.get());
                }
                else
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
        if(holder.isPresent()) {
            sample.returns.add(holder.get());
            while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                Optional<VariableDeclarationNode> holder2 = variableDeclarations();
                if (holder2.isPresent()) {
                    sample.returns.add(holder2.get());
                }
                else
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
        if((manageTokens.matchAndRemove(Token.TokenTypes.CLASS).isEmpty())) {
            return Optional.empty();
        }
        if((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty())) {
            throw new SyntaxErrorException("Class must have a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        classNode.name = manageTokens.getCurrentText();
        checkForInterfaces(classNode);
        RequireNewLine();
        if(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Class must have a proper indentation", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while((manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) && !(manageTokens.getToken().isEmpty())) {
            Optional<ConstructorNode> constructorNode = constructor();
            if(constructorNode.isPresent()) {
                classNode.constructors.add(constructorNode.get());
                continue;
            }
            if(manageTokens.getSpecificToken(0) == Token.TokenTypes.WORD) {
                if ((manageTokens.getTokenSize() > 1) && (manageTokens.getSpecificToken(1) == Token.TokenTypes.LPAREN)) {
                    Optional<MethodDeclarationNode> md = methodDeclaration();
                    if (md.isPresent()) {
                        classNode.methods.add(md.get());
                        continue;
                    }
                }
                else {
                    List<MemberNode> memberNode = members();
                    if(!memberNode.isEmpty()) {
                        classNode.members.addAll(memberNode);
                        continue;
                    }
                }
            }
            throw new SyntaxErrorException("Class expected a constructor, member, or method", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if((manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())) {
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
        if((manageTokens.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isEmpty())) {
            return;
        }
        if((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty())) {
            throw new SyntaxErrorException("Class must implement an interface with a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        sample.interfaces.add(manageTokens.getCurrentText());
        while((manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent())) {
            if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()) {
                sample.interfaces.add(manageTokens.getCurrentText());
            }
            else
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
        if((manageTokens.matchAndRemove(Token.TokenTypes.CONSTRUCT).isEmpty())) {
            return Optional.empty();
        }
        if((manageTokens.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty())) {
            throw new SyntaxErrorException("Constructor must have a left parentheses", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        checkParameterConstructor(constructorNode);
        if(manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
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
        if(holder.isPresent()) {
            sample.parameters.add(holder.get());
            while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                Optional<VariableDeclarationNode> holder2 = variableDeclarations();
                if (holder2.isPresent()) {
                    sample.parameters.add(holder2.get());
                }
                else
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
        if(manageTokens.matchAndRemove(Token.TokenTypes.SHARED).isPresent()) {
            methodDeclarationNode.isShared = true;
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent()) {
            methodDeclarationNode.isPrivate = true;
        }
        Optional<MethodHeaderNode> methodHeader = methodHeaders();
        if(methodHeader.isEmpty()) {
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
        if(holder.isEmpty()) {
            return memberNodes;
        }
        for(VariableDeclarationNode variableDeclarationNode : holder) {
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
        if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty())
            return multivariable;
        String typeName = manageTokens.getCurrentText();
        VariableDeclarationNode variableDeclarationNode = new VariableDeclarationNode();
        variableDeclarationNode.type = typeName;
        Optional<VariableDeclarationNode> put = VariableNameValue(variableDeclarationNode);
        if(put.isEmpty())
            return multivariable;
        multivariable.add(put.get());
        while(manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
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
        if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()) {
            throw new SyntaxErrorException("Expected to have a variable name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        sample.name = manageTokens.getCurrentText();
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
        if(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Method body expected an indent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while(manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) {
            List<VariableDeclarationNode> list = multipleVariableDeclarations();
            if(!list.isEmpty()) {
                sample.locals.addAll(list);
                continue;
            }
            Optional<StatementNode> collector = statement();
            if(collector.isPresent()) {
                sample.statements.add(collector.get());
                continue;
            }
            throw new SyntaxErrorException("Method expected a statement", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
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
        if(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Method body expected an indent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while(manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) {
            List<VariableDeclarationNode> list = multipleVariableDeclarations();
            if(!list.isEmpty()) {
                sample.locals.addAll(list);
                continue;
            }
            Optional<StatementNode> collector = statement();
            if(collector.isPresent()) {
                sample.statements.add(collector.get());
                continue;
            }
            throw new SyntaxErrorException("Method expected a statement", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
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
        if(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Indent Expected on statements", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while(manageTokens.getSpecificToken(0) != Token.TokenTypes.DEDENT) {
            Optional<StatementNode> statement = statement();
            if(statement.isPresent()) {
                statements.add(statement.get());
            }
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            throw new SyntaxErrorException("Statement expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        return statements;
    }

    /**
     * This method creates an if statement node or loop statement node which then proceeds to check for
     * the presence of either one, then proceeds to return that type of statement node.
     *
     * @return The statement node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<StatementNode> statement() throws SyntaxErrorException {
        if(manageTokens.matchAndRemove(Token.TokenTypes.IF).isPresent()) {
            Optional<IfNode> ifHolder = ifStatement();
            if(ifHolder.isPresent()) {
                return Optional.of(ifHolder.get());
            }
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.LOOP).isPresent()) {
            Optional<LoopNode> loopHolder = loopStatement();
            if(loopHolder.isPresent()) {
                return Optional.of(loopHolder.get());
            }
        }
        return Optional.empty();
    }

    /**
     * This method creates an if statement node then checks for a boolean op node through BoolExpTerm(),
     * a new line, and statement(s), along with an (optional) else followed by a branching statement(s).
     * All of these properties are then proceeded to be added to the if statement node which is then returned.
     *
     *
     * @return The if statement node.
     * @throws SyntaxErrorException When there is an error that occurs in the program.
     */
    public Optional<IfNode> ifStatement() throws SyntaxErrorException {
        IfNode holder = new IfNode();
        Optional<BooleanOpNode> bool = BoolExpTerm();
        if(bool.isPresent()) {
            holder.condition = bool.get();
        }
        else
            holder.condition = new BooleanOpNode();
        RequireNewLine();
        List<StatementNode> statements = Statements();
        if(statements.isEmpty()) {
            throw new SyntaxErrorException("Statement expected", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        else
            holder.statements = statements;
        if(manageTokens.matchAndRemove(Token.TokenTypes.ELSE).isPresent()) {
            ElseNode elseNode = new ElseNode();
            RequireNewLine();
            List<StatementNode> elseStatements = Statements();
            if(elseStatements.isEmpty()) {
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
        if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()) {
            Optional<VariableReferenceNode> reference = Expression();
            VariableReferenceNode variableReferenceNode = reference.get();
            variableReferenceNode.name = manageTokens.getCurrentText();
            holder.assignment = Optional.of(variableReferenceNode);
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()) {
            // TO DO LATER
        }
        Optional<BooleanOpNode> bool = BoolExpTerm();
        if(bool.isPresent()) {
            holder.expression = bool.get();
        }
        else
            holder.expression = new BooleanOpNode();
        RequireNewLine();
        List<StatementNode> statements = Statements();
        if(statements.isEmpty()) {
            throw new SyntaxErrorException("Statement expected", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        else
            holder.statements = statements;
        return Optional.of(holder);
    }

    /**
     * This method creates and returns a variable reference node (temporarily -> to be modified later).
     *
     * @return The variable reference node.
     */
    public Optional<VariableReferenceNode> Expression() throws SyntaxErrorException {
        VariableReferenceNode reference = VariableReference();
        return Optional.of(reference);
    }

    /**
     * This method returns an empty boolean op node (temporarily -> to be modified later).
     *
     * @return The boolean op node.
     */
    public Optional<BooleanOpNode> BoolExpTerm() throws SyntaxErrorException {
        BooleanOpNode holder = new BooleanOpNode();
        Optional<MethodCallExpressionNode> methodCallExpressionNode = MethodCallExpression();
        if(methodCallExpressionNode.isPresent()) {
            //return Optional.empty();
            // TO DO
        }
        Optional<VariableReferenceNode> expression1 = Expression();
        if(expression1.isPresent()) {
            holder.left = expression1.get();
            if(manageTokens.matchAndRemove(Token.TokenTypes.EQUAL).isPresent()) {
                Optional<VariableReferenceNode> expression2 = Expression();
                holder.right = expression2.get();
                return Optional.of(holder);
            }
            else if(manageTokens.matchAndRemove(Token.TokenTypes.NOTEQUAL).isPresent()) {
                Optional<VariableReferenceNode> expression2 = Expression();
                holder.right = expression2.get();
                return Optional.of(holder);
            }
            else if(manageTokens.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL).isPresent()) {
                Optional<VariableReferenceNode> expression2 = Expression();
                holder.right = expression2.get();
                return Optional.of(holder);
            }
            else if(manageTokens.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL).isPresent()) {
                Optional<VariableReferenceNode> expression2 = Expression();
                holder.right = expression2.get();
                return Optional.of(holder);
            }
            else if(manageTokens.matchAndRemove(Token.TokenTypes.GREATERTHAN).isPresent()) {
                Optional<VariableReferenceNode> expression2 = Expression();
                holder.right = expression2.get();
                return Optional.of(holder);
            }
            else if(manageTokens.matchAndRemove(Token.TokenTypes.LESSTHAN).isPresent()) {
                Optional<VariableReferenceNode> expression2 = Expression();
                holder.right = expression2.get();
                return Optional.of(holder);
            }
        }
        VariableReferenceNode take = VariableReference();
        if(take.name != null) {
            holder.left = take;
            return Optional.of(holder);
        }
        return Optional.empty();
    }

    public Optional<MethodCallExpressionNode> MethodCallExpression() {
        return Optional.empty();
    }

    public VariableReferenceNode VariableReference() throws SyntaxErrorException {
        VariableReferenceNode holder = new VariableReferenceNode();
        if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()) {
            throw new SyntaxErrorException("Variable reference expected", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        holder.name = manageTokens.getCurrentText();
        return holder;
    }
}