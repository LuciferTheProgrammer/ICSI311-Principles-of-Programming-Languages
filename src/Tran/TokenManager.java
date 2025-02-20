package Tran;
import java.util.List;
import java.util.Optional;

public class TokenManager {
    private List<Token> token;

    public TokenManager(List<Token> tokens) {
        token = tokens;
    }

    public boolean done() {
        if(token.isEmpty())
            return true;
        else
          return false;
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        if(!token.isEmpty()) {
            if (token.get(0).getType() == t) {
                Token holder = token.get(0);
                token.remove(0);
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }

    public Optional<Token> peek(int i) {
        if(!token.isEmpty()) {
            Token holder = token.get(i);
            return Optional.of(holder);
        }
        else
	        return Optional.empty();
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
        if(token.get(0).getType() == first && token.get(1).getType() == second) {
            return true;
        }
        else
	        return false;
    }

    public boolean nextIsEither(Token.TokenTypes first, Token.TokenTypes second) {
        if(token.get(0).getType() == first || token.get(0).getType() == second)
            return true;
        else
	        return false;
    }

    public int getCurrentLine() {
            return token.get(0).getLineNumber();
    }

    public int getCurrentColumnNumber() {
            return token.get(0).getColumnNumber();
    }
}
