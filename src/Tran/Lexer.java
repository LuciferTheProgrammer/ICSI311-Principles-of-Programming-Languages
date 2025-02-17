package Tran;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


// This is the Lexer Class, which reads a given text letter by letter and converts it into a collection
// of words. [David is here!]
public class Lexer {

    // The hash map to contain the known keywords with their specified token type.
    private Map<String, Token.TokenTypes> keywords;

    // The text manager instance field to contain the text to be processed.
    private final TextManager textManager;

    // The instance field to store the line number in the text.
    private int lineNumber;

    // The instance field to store the position in the text.
    private int characterPosition;

    // The hash map to contain the known punctuations with their specified token type.
    private Map<String, Token.TokenTypes> punctuations;

    // The previous indentation level.
    private int previousIndentCounter;

    // The current indentation level.
    private int currentIndentCounter;

    /**
     * This constructor takes in a String and assigns it to the text manager instance field for text
     * processing, it sets the starting line number to 1 and position to 0, and also creates 2 hash maps
     * one that contains the keywords and the other for punctuations with their associated token types.
     * Finally, it sets both the previous and current indentation levels to 0.
     *
     * @param input The String text to be processed.
     */
    public Lexer(String input) {
        textManager = new TextManager(input);
        lineNumber = 1;
        characterPosition = 0;
        keywords = new HashMap<>();
        keywords.put("if", Token.TokenTypes.IF);
        keywords.put("else", Token.TokenTypes.ELSE);
        keywords.put("loop", Token.TokenTypes.LOOP);
        keywords.put("class", Token.TokenTypes.CLASS);
        keywords.put("implements", Token.TokenTypes.IMPLEMENTS);
        keywords.put("interface", Token.TokenTypes.INTERFACE);
        keywords.put("shared", Token.TokenTypes.SHARED);
        keywords.put("new", Token.TokenTypes.NEW);
        keywords.put("private", Token.TokenTypes.PRIVATE);
        keywords.put("construct", Token.TokenTypes.CONSTRUCT);

        punctuations = new HashMap<>();
        punctuations.put("=", Token.TokenTypes.ASSIGN);
        punctuations.put("(", Token.TokenTypes.LPAREN);
        punctuations.put(")", Token.TokenTypes.RPAREN);
        punctuations.put(":", Token.TokenTypes.COLON);
        punctuations.put(".", Token.TokenTypes.DOT);
        punctuations.put("+", Token.TokenTypes.PLUS);
        punctuations.put("-", Token.TokenTypes.MINUS);
        punctuations.put("*", Token.TokenTypes.TIMES);
        punctuations.put("/", Token.TokenTypes.DIVIDE);
        punctuations.put("%", Token.TokenTypes.MODULO);
        punctuations.put(",", Token.TokenTypes.COMMA);
        punctuations.put("==", Token.TokenTypes.EQUAL);
        punctuations.put("!=", Token.TokenTypes.NOTEQUAL);
        punctuations.put("<", Token.TokenTypes.LESSTHAN);
        punctuations.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        punctuations.put(">", Token.TokenTypes.GREATERTHAN);
        punctuations.put(">=", Token.TokenTypes.GREATERTHANEQUAL);

        previousIndentCounter = 0;
        currentIndentCounter = 0;
    }

    /**
     * This method creates a linked list in which to add/store the different tokens that are generated
     * as our text is processed and returns the list of tokens once we have reached the end of the text.
     *
     * @return The list of tokens.
     * @throws Exception if an error occurs in the program.
     */
    public List<Token> Lex() throws Exception {
        List<Token> ListOfTokens = new LinkedList<>();
        while (!textManager.isAtEnd()) {
            char C = textManager.peekCharacter();
            if (C == '\0')
                break;
            if (C == '\n') {
                ListOfTokens.add(readNewLine());
                if (!textManager.isAtEnd())
                    readIndentAndDedent(ListOfTokens);
            }
            else if (Character.isLetter(C)) {
                ListOfTokens.add(readWord());
            }
            else if (Character.isDigit(C)) {
                ListOfTokens.add(readNumber());
            }
            else if (C == '.') {
                char next = textManager.peekCharacter(1);
                if (Character.isDigit(next))
                    ListOfTokens.add(readNumber());
                else
                    ListOfTokens.add(readPunctuation());
            }
            else if (C == '\"') {
                ListOfTokens.add(readQuotedString());
            }
            else if (C == '{') {
                readComments();
            }
            else if (C == '\'') {
                ListOfTokens.add(readQuotedCharacter());
            }
            else if (!Character.isWhitespace(C) && !Character.isLetter(C) && !Character.isDigit(C)) {
                ListOfTokens.add(readPunctuation());
            }
            else {
                textManager.getCharacter();
                characterPosition++;
            }

        }
        for (int i = 0; i < (previousIndentCounter / 4); i++) {
            ListOfTokens.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
        }
        return ListOfTokens;
    }

    /**
     * This method creates a mutable String in which we loop around the text, adding characters one at a time
     * until the current character being processed is no longer a letter or digit. Then the newly created String
     * is utilized to see if it is a match to any of the keywords in our hash map. If there is a match, a token
     * for that specific keyword is generated and returned. Otherwise, a WORD token is generated and returned
     * for that unknown word. In addition, this method also increments the character position for each character
     * that is processed.
     *
     * @return The token for a specific keyword or a WORD token for the unknown word.
     */
    public Token readWord() {
        StringBuilder container = new StringBuilder();

        while (Character.isLetterOrDigit(textManager.peekCharacter())) {
            container.append(textManager.getCharacter());
            characterPosition++;
        }
        String holder = container.toString();
        if (keywords.containsKey(holder))
            return new Token(keywords.get(holder), lineNumber, characterPosition);
        else
            return new Token(Token.TokenTypes.WORD, lineNumber, characterPosition, holder);
    }

    /**
     * This method moves to the next available character in the Text while also incrementing
     * line number, setting the character position to 0 and generates and returns a new line token.
     *
     * @return The new line token.
     */
    public Token readNewLine() {
        textManager.getCharacter();
        lineNumber++;
        characterPosition = 0;
        return new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition);
    }

    /**
     * This method creates a mutable String in which we loop around the text, adding digits one at a time while
     * also checking for and adding one dot (if present) to build a floating point number. If the there is more than 1 dot,
     * the method throws a syntax error. When the current character is no longer a digit then the
     * newly created String is utilized to generate and return a number token. In addition, this method also increments
     * the character position for each character that is processed.
     *
     * @return The number token.
     * @throws Exception Syntax error for Number Formatting.
     */
    public Token readNumber() throws Exception {
        StringBuilder numberContainer = new StringBuilder();
        int numberOfDots = 0;
        while (Character.isDigit(textManager.peekCharacter()) || textManager.peekCharacter() == '.') {
            if (textManager.peekCharacter() == '.') {
                numberOfDots++;
                if (numberOfDots > 1)
                    throw new SyntaxErrorException("Syntax Error: Number Formatting", lineNumber, characterPosition);
                if (!Character.isDigit(textManager.peekCharacter(1)))
                    throw new SyntaxErrorException("Syntax Error: Number Formatting", lineNumber, characterPosition);
            }
            numberContainer.append(textManager.getCharacter());
            characterPosition++;
        }
        String numberReturn = numberContainer.toString();
        return new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, numberReturn);
    }

    /**
     * This method creates a mutable String then adds the current character and the next one from the text.
     * Then it proceeds to store the first character at another String. It also checks the two
     * character String, if there is a match to any of the punctuations in our hash map a token
     * for that specific punctuation is generated and returned. If there is no match for the two character String
     * then we move and check if there is a match for our first character String in our punctuation hash map, when
     * a match is found, a token for that specific punctuation is generated and returned. Finally, if there is no
     * match to any of the punctuations in our hash map a syntax error is thrown. In addition, character position
     * is also incremented for each character that is processed.
     *
     * @return The punctuation token.
     * @throws Exception Syntax error for Punctuation Mismatch.
     */
    public Token readPunctuation() throws Exception {
        StringBuilder punctuationContainer = new StringBuilder();
        punctuationContainer.append(textManager.getCharacter());
        characterPosition++;
        punctuationContainer.append(textManager.peekCharacter());
        String container1 = punctuationContainer.toString();
        String firstCharacter = container1.substring(0, 1);
        if (punctuations.containsKey(container1)) {
            textManager.getCharacter();
            characterPosition++;
            return new Token(punctuations.get(container1), lineNumber, characterPosition);
        } else if (punctuations.containsKey(firstCharacter))
            return new Token(punctuations.get(firstCharacter), lineNumber, characterPosition);
        else
            throw new SyntaxErrorException("Syntax Error: Punctuation Mismatch", lineNumber, characterPosition);
    }

    /**
     * This method takes in a list of tokens then keeps count of the number of spaces for the current
     * indentation level, for each space the counter is incremented by 1 and if it's a '\t' the space
     * counter is incremented by 4. This rule falls the same for character position. If current line is just
     * an empty line, then the current indentation level is set to the previous indentation level. If the number
     * of spaces is not divisible by 4 then a syntax error is thrown. Otherwise, if the current line is more
     * indented from the previous line then an indent token is generated and returned and if its less then
     * a dedent token is generated and returned. Finally, before moving to the next line, the indentation level
     * for the previous line is set to the current and then we set the current to 0 for the new line.
     *
     * @param tokens list of tokens.
     * @throws Exception Syntax error for Improper Indentation.
     */
    public void readIndentAndDedent(List<Token> tokens) throws Exception {
        while(Character.isWhitespace(textManager.peekCharacter()) && textManager.peekCharacter() != '\n') {
            if(textManager.peekCharacter() == '\t') {
                currentIndentCounter += 4;
                textManager.getCharacter();
                characterPosition += 4;
            }
            else {
                currentIndentCounter++;
                textManager.getCharacter();
                characterPosition++;
            }
        }
        if(textManager.peekCharacter() == '\n')
            currentIndentCounter = previousIndentCounter;
        if((currentIndentCounter % 4 != 0))
            throw new SyntaxErrorException("Syntax Error: Improper Indentation", lineNumber, characterPosition);
        else if(currentIndentCounter > previousIndentCounter)
            tokens.add(new Token(Token.TokenTypes.INDENT, lineNumber, characterPosition));
        else if(currentIndentCounter < previousIndentCounter) {
            int blockClose = (previousIndentCounter - currentIndentCounter) / 4;
            int counter = 0;
            while(counter < blockClose) {
                tokens.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
                counter++;
            }
        }
        previousIndentCounter = currentIndentCounter;
        currentIndentCounter = 0;
    }

    /**
     * This method creates a mutable String which keeps adding one character at a time from the text as long as
     * it is inside a double quote, if it encounters a '\n' it will increment the line number by 1 and set
     * the character position to 0, and once it reaches the ending double quote it generates and returns
     * a token for the quoted String. Otherwise, if it's an unterminated String it will throw a syntax error.
     *
     * @return The quoted String token.
     * @throws Exception Syntax error for Unterminated String.
     */
    public Token readQuotedString() throws Exception {
        StringBuilder quotedString = new StringBuilder();
        textManager.getCharacter();
        characterPosition++;
        while(textManager.peekCharacter() != '\"') {
            if(textManager.isAtEnd())
                throw new SyntaxErrorException("Syntax Error: Unterminated String", lineNumber, characterPosition);
            if(textManager.peekCharacter() == '\n') {
                lineNumber++;
                characterPosition = 0;
            }
            quotedString.append(textManager.getCharacter());
            characterPosition++;
        }
        textManager.getCharacter();
        characterPosition++;
        String toContain = quotedString.toString();
        return new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, characterPosition, toContain);
    }

    /**
     * This method processes one character at a time from the text and as long as it is inside the curly braces,
     * which indicates comments, this part of the text is ignored. When a '\n' is detected the line number
     * is incremented by 1 and the character position is set to 0. If there is no ending brace for the comment,
     * which indicates the presence of an unterminated comment, it throws a syntax error.
     *
     * @throws Exception Syntax error for Unterminated Comment.
     */
    public void readComments() throws Exception {
        textManager.getCharacter();
        characterPosition++;
        while(textManager.peekCharacter() != '}') {
            if(textManager.isAtEnd())
                throw new SyntaxErrorException("Syntax Error: Unterminated Comment", lineNumber, characterPosition);
            if(textManager.peekCharacter() == '\n') {
                lineNumber++;
                characterPosition = 0;
            }
            textManager.getCharacter();
            characterPosition++;
        }
        textManager.getCharacter();
        characterPosition++;
    }

    /**
     * This method creates a mutable String which adds the character after the first single quote and ends
     * once it reaches the ending single quote which then generates and returns a quoted character token. If there is
     * no ending single quote which marks the presence of an unterminated character then a syntax error is
     * thrown.
     *
     * @return The quoted character token.
     * @throws Exception Syntax error for Unterminated Character.
     */
    public Token readQuotedCharacter() throws Exception {
        StringBuilder quotedCharacter = new StringBuilder();
        textManager.getCharacter();
        characterPosition++;
        if(textManager.peekCharacter(1) == '\'') {
            quotedCharacter.append(textManager.getCharacter());
            characterPosition++;
        }
        else
            throw new SyntaxErrorException("Syntax Error: Unterminated Character", lineNumber, characterPosition);
        textManager.getCharacter();
        characterPosition++;
        String quotedCharacterString = quotedCharacter.toString();
        return new Token(Token.TokenTypes.QUOTEDCHARACTER, lineNumber, characterPosition, quotedCharacterString);
    }
}