package Tran;
import AST.*;
import java.util.LinkedList;
import java.util.ArrayList;
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
        List<Token> holder = manageTokens.getToken();
        if((manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty())) {
            if((!manageTokens.done())&& (holder.get(0).getType() == Token.TokenTypes.DEDENT))
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
            Optional<InterfaceNode> holder = interfaceStatement();
            if (holder.isPresent()) {
                currentNode.Interfaces.add(holder.get());
            }
            Optional<ClassNode> holder2 = classStatements();
            if(holder2.isPresent()) {
                currentNode.Classes.add(holder2.get());
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
        while((manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) && !(manageTokens.getToken().isEmpty())) {
            Optional<MethodHeaderNode> test = methodHeaders();
            if ((test.isEmpty())) {
                throw new SyntaxErrorException("Interface must have method with a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            else {
                MethodHeaderNode method = test.get();
                node.methods.add(method);
            }
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
        while((!manageTokens.done() && manageTokens.getToken().get(0).getType() != Token.TokenTypes.DEDENT)) {
            RequireNewLine();
            if (manageTokens.done() || manageTokens.getToken().get(0).getType() == Token.TokenTypes.DEDENT)
                break;
            Optional<ConstructorNode> constructorNode = constructor();
            if(constructorNode.isPresent()) {
                classNode.constructors.add(constructorNode.get());
                continue;
            }
            List<MemberNode> memberNode = members();
            if(memberNode.isEmpty()) {
             classNode.members.addAll(memberNode);
             continue;
            }
            Optional<MethodDeclarationNode> md = methodDeclaration();
            if(md.isPresent()) {
                classNode.methods.add(md.get());
                continue;
            }
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            throw new SyntaxErrorException("Class expected an ending dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        return Optional.of(classNode);
    }

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
    public List<VariableDeclarationNode> multipleVariableDeclarations() throws SyntaxErrorException {
        List<VariableDeclarationNode> multivariable = new ArrayList<>();
        VariableDeclarationNode variableDeclarationNode = new VariableDeclarationNode();
        if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent())
            variableDeclarationNode.type = manageTokens.getCurrentText();
        Optional<VariableDeclarationNode> put = VariableNameValue(variableDeclarationNode);
        if(put.isEmpty())
            return new ArrayList<>();
        multivariable.add(put.get());
        while(manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            Optional<VariableDeclarationNode> placement = VariableNameValue(variableDeclarationNode);
            multivariable.add(placement.get());
        }
        RequireNewLine();
        return multivariable;
    }
    public Optional<VariableDeclarationNode> VariableNameValue(VariableDeclarationNode sample) throws SyntaxErrorException {
        VariableDeclarationNode holder = new VariableDeclarationNode();
        holder.type = sample.type;
        if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()) {
            throw new SyntaxErrorException("Expected to have a variable name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        holder.name = manageTokens.getCurrentText();
        return Optional.of(holder);
    }
    public void methodBodyConstructor(ConstructorNode sample) throws SyntaxErrorException {
        if(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Method body expected an indent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while(manageTokens.getToken().get(0).getType() != Token.TokenTypes.DEDENT && manageTokens.getToken().get(0).getType() == Token.TokenTypes.WORD) {
            List<VariableDeclarationNode> list = multipleVariableDeclarations();
            sample.locals.addAll(list);
        }
        while(manageTokens.getToken().get(0).getType() != Token.TokenTypes.DEDENT) {
            Optional<StatementNode> collector = statement();
            if(collector.isPresent()) {
                sample.statements.add(collector.get());
            }
            else
                throw new SyntaxErrorException("Method expected a statement", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Method body expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
    }

    public void methodBodyMD(MethodDeclarationNode sample) throws SyntaxErrorException {
        if(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Method body expected an indent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while(manageTokens.getToken().get(0).getType() != Token.TokenTypes.DEDENT && manageTokens.getToken().get(0).getType() == Token.TokenTypes.WORD) {
            List<VariableDeclarationNode> list = multipleVariableDeclarations();
            sample.locals.addAll(list);
        }
        while(manageTokens.getToken().get(0).getType() != Token.TokenTypes.DEDENT) {
            Optional<StatementNode> collector = statement();
            if(collector.isPresent()) {
                sample.statements.add(collector.get());
            }
            else
                throw new SyntaxErrorException("Method expected a statement", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty())
            throw new SyntaxErrorException("Method body expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
    }

    public List<StatementNode> Statements() throws SyntaxErrorException {
        List<StatementNode> statements = new ArrayList<>();
        if(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Indent Expected on statements", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while(manageTokens.getToken().get(0).getType() != Token.TokenTypes.DEDENT) {
            Optional<StatementNode> statement = statement();
            if(statement.isPresent()) {
                statements.add(statement.get());
            }
            else
                throw new SyntaxErrorException("Statement expected", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            throw new SyntaxErrorException("Statement expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        return statements;
    }
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
    public Optional<IfNode> ifStatement() throws SyntaxErrorException {
        IfNode holder = new IfNode();
        Optional<BooleanOpNode> bool = BoolExpTerm();
        if(bool.isPresent()) {
            holder.condition = bool.get();
        }
        else
            holder.condition = new BooleanOpNode();
        RequireNewLine();
        //Statements
        List<StatementNode> statements = Statements();
        if(statements.isEmpty()) {
            throw new SyntaxErrorException("Statement expected", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        else
            holder.statements = statements;
        return Optional.of(holder);
    }

    public Optional<LoopNode> loopStatement() throws SyntaxErrorException {
        LoopNode holder = new LoopNode();
        if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()) {
            Optional<VariableReferenceNode> reference = toRefer();
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
        //Statements
        List<StatementNode> statements = Statements();
        if(statements.isEmpty()) {
            throw new SyntaxErrorException("Statement expected", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        else
            holder.statements = statements;
        return Optional.of(holder);
    }
    public Optional<VariableReferenceNode> toRefer() throws SyntaxErrorException {
        VariableReferenceNode holder = new VariableReferenceNode();
        return Optional.of(holder);
    }

    public Optional<BooleanOpNode> BoolExpTerm() throws SyntaxErrorException {
        return Optional.empty();
    }
}