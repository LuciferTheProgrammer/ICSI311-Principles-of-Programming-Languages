package Tran;
import AST.*;
import javax.swing.text.html.Option;
import java.util.*;

public class Parser {
    private TokenManager manageTokens;
    private TranNode currentNode;


    public Parser(TranNode top, List<Token> tokens) {
        currentNode = top;
        manageTokens = new TokenManager(tokens);
    }

    public void RequireNewLine() throws SyntaxErrorException {
        manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE);
        while(manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) ;
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
        if (!(manageTokens.matchAndRemove(Token.TokenTypes.INTERFACE).isPresent()))
            return Optional.empty();
        if (!(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()))
            throw new SyntaxErrorException("Interface statements must have a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        node.name = manageTokens.getCurrentText();
        RequireNewLine();
        if (!(manageTokens.matchAndRemove(Token.TokenTypes.INDENT).isPresent()))
            throw new SyntaxErrorException("Interface statements must have an indentation", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        while(!(manageTokens.matchAndRemove(Token.TokenTypes.DEDENT).isPresent()) && !(manageTokens.getToken().isEmpty())) {
            Optional<MethodHeaderNode> test = methodHeaders();
            if (!(test.isPresent())) {
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
        if (!(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()))
            return Optional.empty();
        methodHeaderNode.name = manageTokens.getCurrentText();
        if ((manageTokens.matchAndRemove(Token.TokenTypes.LPAREN).isPresent())) {
            checkParameterVariableSetUp(methodHeaderNode);
        }
        if(!(manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isPresent()))
            throw new SyntaxErrorException("Interface statements must have a right parentheses", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        if((manageTokens.matchAndRemove(Token.TokenTypes.COLON).isPresent())) {
            checkReturnVariableSetUp(methodHeaderNode);
        }
        RequireNewLine();
        return Optional.of(methodHeaderNode);
    }
    public Optional<VariableDeclarationNode> variableDeclarations () throws SyntaxErrorException {
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
        VariableDeclarationNode variableDeclaration = new VariableDeclarationNode();
        Optional<VariableDeclarationNode> holder = variableDeclarations();
        if(holder.isPresent()) {
            variableDeclaration = holder.get();
            sample.parameters.add(variableDeclaration);
            while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                Optional<VariableDeclarationNode> holder2 = variableDeclarations();
                VariableDeclarationNode variableDeclaration2 = new VariableDeclarationNode();
                if (holder2.isPresent()) {
                    variableDeclaration2 = holder2.get();
                    sample.parameters.add(variableDeclaration2);
                }
                else
                    throw new SyntaxErrorException("Interface statements needs correct parameter variable declarations", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
        }
    }
    public void checkReturnVariableSetUp(MethodHeaderNode sample) throws SyntaxErrorException {
        Optional<VariableDeclarationNode> holder = variableDeclarations();
        VariableDeclarationNode variableDeclaration = new VariableDeclarationNode();
        if(holder.isPresent()) {
            variableDeclaration = holder.get();
            sample.returns.add(variableDeclaration);
            while (manageTokens.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                Optional<VariableDeclarationNode> holder2 = variableDeclarations();
                VariableDeclarationNode variableDeclaration2 = new VariableDeclarationNode();
                if (holder2.isPresent()) {
                    variableDeclaration2 = holder2.get();
                    sample.returns.add(variableDeclaration2);
                }
                else
                    throw new SyntaxErrorException("Interface statements needs correct return variable declarations", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            }
        }
    }
}


