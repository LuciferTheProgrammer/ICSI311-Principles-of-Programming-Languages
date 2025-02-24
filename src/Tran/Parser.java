package Tran;
import AST.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Parser {
    private TokenManager manageTokens;
    private TranNode currentNode;


    public Parser(TranNode top, List<Token> tokens) {
        currentNode = top;
        manageTokens = new TokenManager(tokens);
    }

    public void RequireNewLine() throws SyntaxErrorException {
        List<Token> holder = manageTokens.getToken();
        if((manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty())) {
            if((!manageTokens.done())&& (holder.get(0).getType() == Token.TokenTypes.DEDENT))
                return;
            throw new SyntaxErrorException("Interface statements must a newline", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        }
        while(manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent());
    }

    public void Tran() throws SyntaxErrorException {
        while(!manageTokens.done()) {
            Optional<InterfaceNode> holder = interfaceStatement();
            if (holder.isPresent()) {
                currentNode.Interfaces.add(holder.get());
            }
        }
    }
    public Optional<InterfaceNode> interfaceStatement() throws SyntaxErrorException {
        InterfaceNode node = new InterfaceNode();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.INTERFACE).isEmpty()))
            return Optional.empty();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()))
            throw new SyntaxErrorException("Interface statements must have a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        node.name = manageTokens.getCurrentText();
        RequireNewLine();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()))
            throw new SyntaxErrorException("Interface statements must have an indentation", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        while((manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) && !(manageTokens.getToken().isEmpty())) {
            Optional<MethodHeaderNode> test = methodHeaders();
            if ((test.isEmpty())) {
                throw new SyntaxErrorException("Interface statements must have methods", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
            else {
                MethodHeaderNode method = test.get();
                node.methods.add(method);
            }
        }
        return Optional.of(node);
    }

    public Optional<MethodHeaderNode> methodHeaders() throws SyntaxErrorException {
        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.WORD).isEmpty()))
            return Optional.empty();
        methodHeaderNode.name = manageTokens.getCurrentText();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()))
            throw new SyntaxErrorException("Interface statements must have a left parentheses", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        checkParameterVariableSetUp(methodHeaderNode);
        if((manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()))
            throw new SyntaxErrorException("Interface statements must have a right parentheses", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        //Optional for methods on interface.
        if((manageTokens.matchAndRemove(Token.TokenTypes.COLON).isPresent())) {
            checkReturnVariableSetUp(methodHeaderNode);
        }
        RequireNewLine();
        return Optional.of(methodHeaderNode);
    }
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
                    throw new SyntaxErrorException("Interface statements needs correct parameter variable declarations", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
        }
    }
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
                    throw new SyntaxErrorException("Interface statements needs correct return variable declarations", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
        }
    }
}