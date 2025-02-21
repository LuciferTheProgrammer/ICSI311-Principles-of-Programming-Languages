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

    public void RequireNewLine() {

    }

    public void Tran() throws SyntaxErrorException {

    }
    public Optional<InterfaceNode> interfaceStatement() throws SyntaxErrorException{
            InterfaceNode node = new InterfaceNode();
            if(!(manageTokens.matchAndRemove(Token.TokenTypes.INTERFACE).isPresent()))
                return Optional.empty();
            if(!(manageTokens.matchAndRemove(Token.TokenTypes.WORD).isPresent()))
                throw new SyntaxErrorException("Interface Statements must have a name", manageTokens.getCurrentLine(), manageTokens.getCurrentColumnNumber());
            node.name = manageTokens.getCurrentText();
            return Optional.of(node);
    }
}
