package Tran;
import java.util.List;
import java.util.Optional;

// The TokenManager Class processes the list of tokens gathered from the Lexer.
public class TokenManager {

    // The list to store the stream of tokens to be processed.
    private List<Token> token;

    // The instance field to store the String value of the specified token.
    private String container;

    /**
     * The constructor takes in a list of tokens and sets it to the token instance field.
     *
     * @param tokens The list of tokens.
     */
    public TokenManager(List<Token> tokens) {
        token = tokens;
    }

    /**
     * This method returns true once all the tokens from the list have been processed and thereby removed.
     * If not, then the processing of the list of tokens isn't over yet and returns false.
     *
     * @return The status of whether the list of tokens has been fully processed.
     */
    public boolean done() {
        if(token.isEmpty())
            return true;
        else
          return false;
    }

    /**
     * This method takes in a token with the specified type and checks if it's a match to the current token
     * type on the current position in the list of tokens. If it's a match then the current token is removed
     * from the list and if it's not then there is no change. This also returns the token that was matched
     * removed. If there is no match, then an empty value is returned.
     *
     * @param t The token type.
     * @return The token that was matched and removed.
     */
    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        if(!token.isEmpty()) {
            if (token.get(0).getType() == t) {
                container = token.get(0).getValue();
                Token holder = token.get(0);
                token.remove(0);
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }

    /**
     * This method takes in a number and uses it as an index to look and return the token in that position
     * from the list of tokens. If the index is out of bounds of the list, then an empty value
     * is returned.
     *
     * @param i The index.
     * @return The token that was peeked.
     */
    public Optional<Token> peek(int i) {
        if(!token.isEmpty()) {
            Token holder = token.get(i);
            return Optional.of(holder);
        }
        else
	        return Optional.empty();
    }

    /**
     * This method takes in two token types and checks to see if it's a match to the current token type
     * and the next token type from the list of tokens. If both are a match, then it returns true. Otherwise,
     * there is no match and returns false.
     *
     * @param first token type.
     * @param second token type.
     * @return The status of the match.
     */
    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
        if(token.get(0).getType() == first && token.get(1).getType() == second) {
            return true;
        }
        else
	        return false;
    }

    /**
     * This method takes in two token types and checks to see if the current token type is a match
     * to either the first or the second token types specified. If there is a match, then true is returned.
     * Otherwise, a status of false is returned.
     *
     * @param first token type.
     * @param second token type.
     * @return The status of the match.
     */
    public boolean nextIsEither(Token.TokenTypes first, Token.TokenTypes second) {
        if(token.get(0).getType() == first || token.get(0).getType() == second)
            return true;
        else
	        return false;
    }

    /**
     * This method returns the line number of the specified token from the list of tokens.
     *
     * @return The line number.
     */
    public int getCurrentLine() {
            return token.get(0).getLineNumber();
    }

    /**
     * This method returns the column number of the specified token from the list of tokens.
     *
     * @return The column number.
     */
    public int getCurrentColumnNumber() {
            return token.get(0).getColumnNumber();
    }

    /**
     * This method returns the String value of the current token from the list of tokens.
     *
     * @return The String value.
     */
    public String getCurrentText() {
        return container;
    }

    /**
     * This method returns the list of tokens.
     *
     * @return The list of tokens.
     */
    public List<Token> getToken() {
        return token;
    }

    /**
     * This method returns the size of the list of tokens.
     *
     * @return the size of the list of tokens.
     */
    public int getTokenSize() {
        return token.size();
    }

    /**
     * This method takes in a valid index from the list of tokens and returns the corresponding token
     * type for that token.
     *
     * @param i The index in the list of tokens.
     * @return The token type.
     */
    public Token.TokenTypes getSpecificToken(int i) {
        return  token.get(i).getType();
    }
}
