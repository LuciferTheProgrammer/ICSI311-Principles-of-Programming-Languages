package Tran;
import AST.*;

import java.sql.Statement;
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
            else
                throw new SyntaxErrorException("Expected an interface", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
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
        while((manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) && !(manageTokens.getToken().isEmpty())) {
            Optional<ConstructorNode> holder = constructor();
            if (holder.isPresent()) {
                classNode.constructors.add(holder.get());
            }
            Optional<MemberNode> holder2 = member();
            if(holder2.isPresent()) {
                MemberNode placement = holder2.get();
                classNode.members.add(holder2.get());
                while(manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                    if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()) {
                        MemberNode placement2 = new MemberNode();
                        placement2.declaration = new VariableDeclarationNode();
                        placement2.declaration.type = placement.declaration.type;
                        placement2.declaration.name = manageTokens.getCurrentText();
                        classNode.members.add(placement2);
                    }
                }
                RequireNewLine();
            }
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
        //Set Up Method Body for later
        //.......
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

    public Optional<MemberNode> member() throws SyntaxErrorException {
        MemberNode memberNode = new MemberNode();
        Optional<VariableDeclarationNode> variableDeclarations = variableDeclarations();
        if(variableDeclarations.isPresent()) {
            memberNode.declaration = variableDeclarations.get();
            return Optional.of(memberNode);
        }
        else
            return Optional.empty();
    }

    public Optional<MethodDeclarationNode> methodDeclaration() throws SyntaxErrorException {
        MethodDeclarationNode methodDeclarationNode = new MethodDeclarationNode();
        if(manageTokens.matchAndRemove(Token.TokenTypes.SHARED).isPresent()) {
            methodDeclarationNode.isShared = true;
        }
        if(manageTokens.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent()) {
            methodDeclarationNode.isPrivate = true;
        }
        if(!methodDeclarationNode.isShared && !methodDeclarationNode.isPrivate) {
            throw new SyntaxErrorException("Method declaration expected a specifier", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        Optional<MethodHeaderNode> methodHeader = methodHeaders();
        if(methodHeader.isEmpty()) {
            throw new SyntaxErrorException("Method declaration expected a method header", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        methodDeclarationNode.name = methodHeader.get().name;
        methodDeclarationNode.parameters = methodHeader.get().parameters;
        methodDeclarationNode.returns = methodHeader.get().returns;
        //RequireNewLine();
        //Set Up Method Body for later
        //.......
        return Optional.empty();
    }

    public void methodBodyConstructor(ConstructorNode sample) throws SyntaxErrorException {
        if(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Method body expected an indent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while(!manageTokens.done() && manageTokens.getToken().get(0).getType() == Token.TokenTypes.WORD) {
            List<VariableDeclarationNode> holder = toReturn();
            sample.locals.addAll(holder);
        }
        //Statement (could be many)

        if((manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()))
            throw new SyntaxErrorException("Method body expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
    }

    public void methodBodyMethodDeclaration(MethodDeclarationNode sample) throws SyntaxErrorException {
        if(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty())
            throw new SyntaxErrorException("Method body expected an indent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        while(!manageTokens.done() && manageTokens.getToken().get(0).getType() == Token.TokenTypes.WORD) {
            List<VariableDeclarationNode> holder = toReturn();
            sample.locals.addAll(holder);
        }
        //Statement (could be many)

        if((manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()))
            throw new SyntaxErrorException("Method body expected a dedent", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
    }

    public List<VariableDeclarationNode> toReturn() throws SyntaxErrorException {
        List<VariableDeclarationNode> variableDeclarations = new ArrayList<>();
        Optional<VariableDeclarationNode> holder = variableDeclarations();
        if(holder.isEmpty())
            throw new SyntaxErrorException("Method body expected a variable declaration", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        VariableDeclarationNode declaredVariable = holder.get();
        variableDeclarations.add(declaredVariable);
        while((manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent())) {
            if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()) {
                VariableDeclarationNode execute = new VariableDeclarationNode();
                execute.type = declaredVariable.type;
                execute.name = manageTokens.getCurrentText();
                variableDeclarations.add(execute);
            }
            else
                throw new SyntaxErrorException("Method body expected a variable name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        RequireNewLine();
        return variableDeclarations;
    }

    public List<StatementNode> statements() throws SyntaxErrorException {
        //List<StatementNode> statements = new ArrayList<>();
        return new ArrayList<>();
    }

    public Optional<StatementNode> statement() throws SyntaxErrorException {
        if(manageTokens.matchAndRemove(Token.TokenTypes.IF).isPresent()) {

        }
        return Optional.empty();
    }

}