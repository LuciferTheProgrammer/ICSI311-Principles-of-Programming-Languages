package Tests;
import AST.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.LinkedList;
import java.util.List;
import Tran.*;

public class MyParser2TestOptional {
    @Test
    public void testMembers_and_methoddeclaration() throws Exception {
        var tran = new TranNode();
        // Ignore the line and column numbers here; the parser uses these for printing syntax errors in Tran code.
        List<Token> list = List.of(
                new Token(Token.TokenTypes.INTERFACE, 1, 9),
                new Token(Token.TokenTypes.WORD, 1, 18, "someName"),
                new Token(Token.TokenTypes.NEWLINE, 2, 0),
                new Token(Token.TokenTypes.INDENT, 2, 4),
                new Token(Token.TokenTypes.WORD, 2, 10, "square"),
                new Token(Token.TokenTypes.LPAREN, 2, 11),
                new Token(Token.TokenTypes.RPAREN, 2, 12),
                new Token(Token.TokenTypes.COLON, 2, 14),
                new Token(Token.TokenTypes.WORD, 2, 21, "number"),
                new Token(Token.TokenTypes.WORD, 2, 23, "s"),
                new Token(Token.TokenTypes.NEWLINE, 3, 0),
                new Token(Token.TokenTypes.DEDENT, 3, 0),
                new Token(Token.TokenTypes.CLASS, 3, 5),
                new Token(Token.TokenTypes.WORD, 3, 17, "TranExample"),
                new Token(Token.TokenTypes.IMPLEMENTS, 3, 28),
                new Token(Token.TokenTypes.WORD, 3, 37, "someName"),
                new Token(Token.TokenTypes.NEWLINE, 4, 0),
                new Token(Token.TokenTypes.INDENT, 4, 4),
                new Token(Token.TokenTypes.WORD, 4, 10, "number"),
                new Token(Token.TokenTypes.WORD, 4, 12, "m"),
                new Token(Token.TokenTypes.NEWLINE, 5, 0),
                new Token(Token.TokenTypes.WORD, 4, 10, "string"),
                new Token(Token.TokenTypes.WORD, 4, 12, "str"),
                new Token(Token.TokenTypes.NEWLINE, 5, 0),
                new Token(Token.TokenTypes.WORD, 5, 9, "start"),
                new Token(Token.TokenTypes.LPAREN, 5, 10),
                new Token(Token.TokenTypes.RPAREN, 5, 11),
                new Token(Token.TokenTypes.NEWLINE, 6, 0),
                new Token(Token.TokenTypes.INDENT, 6, 8),
                new Token(Token.TokenTypes.WORD, 6, 14, "number"),
                new Token(Token.TokenTypes.WORD, 6, 16, "x"),
                new Token(Token.TokenTypes.NEWLINE, 7, 0),
                new Token(Token.TokenTypes.WORD, 7, 14, "number"),
                new Token(Token.TokenTypes.WORD, 7, 16, "y"),
                new Token(Token.TokenTypes.NEWLINE, 8, 0),
                new Token(Token.TokenTypes.DEDENT, 8, 4),
                new Token(Token.TokenTypes.DEDENT, 8, 4)
        );

        var tokens = new LinkedList<>(list); // converting list to linked list so the token manager can handle this
        var p = new Parser(tran, tokens);
        p.Tran();

        var clazz = tran.Classes.getFirst();
        Assertions.assertEquals("s", tran.Interfaces.get(0).methods.getFirst().returns.get(0).name);
        Assertions.assertEquals("someName", clazz.interfaces.getFirst());
        Assertions.assertEquals(2, tran.Classes.getFirst().members.size());
        Assertions.assertEquals("m", tran.Classes.getFirst().members.getFirst().declaration.name);

        // New assertion to check that the method name is "start"
        Assertions.assertEquals("start", tran.Classes.getFirst().methods.getFirst().name);

        Assertions.assertEquals(2, tran.Classes.getFirst().methods.getFirst().locals.size());
        Assertions.assertEquals("x", tran.Classes.getFirst().methods.getFirst().locals.get(0).name);
        Assertions.assertEquals("y", tran.Classes.getFirst().methods.getFirst().locals.get(1).name);
    }
}
