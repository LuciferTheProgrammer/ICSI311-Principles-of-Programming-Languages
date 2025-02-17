package Tests;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Tran.*;


public class MyUnitTests {

        @Test
        public void DoubleNumberTest() {
            // Create a Lexer instance with the given input string.
            var l = new Lexer("789.25 165.35 -789.25 -165.35");
            try {
                var res = l.Lex();
                for(var result : res) {
                    System.out.println("Token: " + result.getType());
                }
                // Expecting a total of 6 tokens.
                Assertions.assertEquals(6, res.size());

                // Token 0: "789.25" (a positive number)
                Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(0).getType());
                Assertions.assertEquals("789.25", res.get(0).getValue());

                // Token 1: "165.35" (a positive number)
                Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(1).getType());
                Assertions.assertEquals("165.35", res.get(1).getValue());

                // Token 2: "-" (the minus sign for -789.25)
                Assertions.assertEquals(Token.TokenTypes.MINUS, res.get(2).getType());

                // Token 3: "789.25" (the numeric part of -789.25)
                Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
                Assertions.assertEquals("789.25", res.get(3).getValue());

                // Token 4: "-" (the minus sign for -165.35)
                Assertions.assertEquals(Token.TokenTypes.MINUS, res.get(4).getType());

                // Token 5: "165.35" (the numeric part of -165.35)
                Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(5).getType());
                Assertions.assertEquals("165.35", res.get(5).getValue());
            } catch (Exception e) {
                Assertions.fail("Exception occurred: " + e.getMessage());
            }
        }
    @Test
    public void invalidNumberTest() {
        var lexer = new Lexer("1.2.5");
        try {
            lexer.Lex();
            // If no exception is thrown, then the test should fail.
            Assertions.fail("Expected a syntax error when lexing '1.2.5', but no exception was thrown.");
        }
        catch(SyntaxErrorException e) {
            System.out.println(e.toString());
            Assertions.assertEquals("Syntax Error: Number Formatting", e.getMessage());
        }
        catch (Exception e) {
            // Exception is expected. Optionally, you can verify details of the exception here.
            Assertions.fail("Unexpected exception occurred: " + e.getMessage());
        }

    }
    @Test
    public void validCharacterLiteralTest() {
        // Create a lexer with a valid character literal, e.g., 'a'
        var lexer = new Lexer("\'a\'");
        try {
            var tokens = lexer.Lex();
            System.out.println("Token: " + tokens.get(0).getType());
            // Assume the first token produced is the character literal.
            // Adjust the index if your lexer produces extra tokens.
            var token = tokens.get(0);
            // Check that the token type is QUOTEDCHARACTER (or your equivalent)
            Assertions.assertEquals(Token.TokenTypes.QUOTEDCHARACTER, token.getType());
            // Check that the token's value is "a"
            Assertions.assertEquals("a", token.getValue());
        }
        catch (SyntaxErrorException e) {
            Assertions.fail("Unexpected SyntaxErrorException occurred: " + e.getMessage());
        }
        catch (Exception e) {
            Assertions.fail("Unexpected exception occurred: " + e.getMessage());
        }
    }
    @Test
    public void unterminatedCharacterLiteralTest() {
        // Create a lexer with an unterminated character literal (missing the closing quote)
        var lexer = new Lexer("\'a");
        try {
            lexer.Lex();
            // If no exception is thrown, then the test should fail.
            Assertions.fail("Expected a syntax error when lexing an unterminated character literal, but no exception was thrown.");
        }
        catch (SyntaxErrorException e) {
            System.out.println(e.toString());
            // Verify that the exception's message is what you expect.
            Assertions.assertEquals("Syntax Error: Unterminated Character", e.getMessage());
        }
        catch (Exception e) {
            // Fail the test if an unexpected exception type is thrown.
            Assertions.fail("Unexpected exception occurred: " + e.getMessage());
        }
    }
    @Test
    public void unterminatedCommentTest() {
        // Create a lexer with an unterminated comment that spans three lines.
        // Note: The comment starts with '{' but never ends with '}'.
        var lexer = new Lexer("{ This is an unterminated comment\nSpanning multiple lines\n\tStill unterminated");
        try {
            lexer.Lex();
            // If no exception is thrown, the test should fail.
            Assertions.fail("Expected a syntax error when lexing an unterminated comment, but no exception was thrown.");
        }
        catch (SyntaxErrorException e) {
            System.out.println(e.toString());
            // Verify that the exception's message is what you expect.
            Assertions.assertEquals("Syntax Error: Unterminated Comment", e.getMessage());
        }
        catch (Exception e) {
            // Fail if any unexpected exception is thrown.
            Assertions.fail("Unexpected exception occurred: " + e.getMessage());
        }
    }
    @Test
    public void unterminatedQuotedStringTest() {
        // Create a lexer with an unterminated quoted string.
        // The opening double quote is present but the closing one is missing.
        var lexer = new Lexer("\"This is an unterminated string");
        try {
            lexer.Lex();
            // If no exception is thrown, then the test should fail.
            Assertions.fail("Expected a syntax error when lexing an unterminated quoted string, but no exception was thrown.");
        }
        catch (SyntaxErrorException e) {
            System.out.println(e.toString());
            // Verify that the exception's message is what you expect.
            Assertions.assertEquals("Syntax Error: Unterminated String", e.getMessage());
        }
        catch (Exception e) {
            // Fail if any other unexpected exception is thrown.
            Assertions.fail("Unexpected exception occurred: " + e.getMessage());
        }
    }
    @Test
    public void punctuationMismatchTest() {
        // Provide an input with an unrecognized punctuation character.
        var lexer = new Lexer("@");
        try {
            lexer.Lex();
            // If no exception is thrown, then the test should fail.
            Assertions.fail("Expected a syntax error for punctuation mismatch, but no exception was thrown.");
        }
        catch (SyntaxErrorException e) {
            System.out.println(e.toString());
            // Verify that the exception message is as expected.
            Assertions.assertEquals("Syntax Error: Punctuation Mismatch", e.getMessage());
        }
        catch (Exception e) {
            // Fail if an unexpected exception is thrown.
            Assertions.fail("Unexpected exception occurred: " + e.getMessage());
        }
    }
    @Test
    public void complexPunctuationErrorTest() {
        // The input string contains a mix of valid tokens, but "[=" is not recognized.
        var lexer = new Lexer(" x >1 and x <= 2 while 2 [= 4");
        try {
            lexer.Lex();
            // If no exception is thrown, the test should fail.
            Assertions.fail("Expected a syntax error for punctuation mismatch, but no exception was thrown.");
        }
        catch (SyntaxErrorException e) {
            System.out.println(e.toString());
            // Verify that the exception message is as expected.
            Assertions.assertEquals("Syntax Error: Punctuation Mismatch", e.getMessage());
        }
        catch (Exception e) {
            // If any other exception is thrown, the test fails.
            Assertions.fail("Unexpected exception occurred: " + e.getMessage());
        }
    }
    @Test
    public void improperIndentationTest() {
        // Create a lexer with input that has improper indentation.
        // The first line contains "if", followed by a newline and 2 spaces (instead of a multiple of 4) before "word".
        var lexer = new Lexer("if\n\thello world\n\n\t word");
        try {
            lexer.Lex();
            // If no exception is thrown, then the test should fail.
            Assertions.fail("Expected a syntax error due to improper indentation, but no exception was thrown.");
        } catch (SyntaxErrorException e) {
            System.out.println(e.toString());
            // Verify that the exception's message is what you expect.
            Assertions.assertEquals("Syntax Error: Improper Indentation", e.getMessage());
        } catch (Exception e) {
            // Fail if any other unexpected exception is thrown.
            Assertions.fail("Unexpected exception occurred: " + e.getMessage());
        }
    }
}
