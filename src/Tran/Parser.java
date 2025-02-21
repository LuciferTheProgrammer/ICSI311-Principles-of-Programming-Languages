package Tran;
import AST.*;

import javax.swing.text.html.Option;
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
        if (!(manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()))
            throw new SyntaxErrorException("Interface statements must have a new line", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
        while(manageTokens.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent());
    }

    public void Tran() throws SyntaxErrorException {

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


        return Optional.of(node);
    }
    /*public Optional<MethodHeaderNode> methods() {
        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();
        if(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()) {
            methodHeaderNode.name = manageTokens.getCurrentText();
            if((manageTokens.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()))
                while(!(manageTokens.matchAndRemove(Token.TokenTypes.RPAREN).isPresent())) {

                }
        }
    }

}
*/
}
