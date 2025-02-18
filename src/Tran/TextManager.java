package Tran;

// This is the Text Manager Class, to process a given text.
public class TextManager {

    // The instance field to store a given text.
    private final String text;

    // The instance field to store the position of the character of a given text.
    private int position;

    /**
     * This constructor takes in a String and stores it in the text instance field and assigns the starting position
     * of 0 for the given text.
     *
     * @param input The String text.
     */
    public TextManager(String input) {
        text = input;
        position = 0;
    }

    /**
     * This method is to check if the current position of the given text has reached the end.
     *
     * @return The status of whether we have reached the end of the text.
     */
    public boolean isAtEnd() {
        if(position == text.length())
            return true;
        else
            return false;
    }

    /**
     * This method returns the character at the current position of the given text. If the
     * position has reached the end of the given text it will return a null terminator.
     *
     * @return The character at the current position.
     */
    public char peekCharacter() {
        char nullTerm = '\0';
        if(position == text.length())
            return nullTerm;
        else
            return text.charAt(position);
    }

    /**
     * This method accepts a number which is added to the current position to return the corresponding character.
     * If the given new position has reached the end of the text it will return a null terminator.
     *
     * @param dist The distance to return the corresponding character.
     * @return The character at the specified position.
     */
    public char peekCharacter(int dist) {
        char nullTerm = '\0';
        if((dist + position) == text.length())
            return nullTerm;
        else
            return text.charAt(dist + position);

    }

    /**
     * This method returns the character at the current position in the text while afterward incrementing position
     * to the next available character. If the position has reached the end of the text it will return a null
     * terminator.
     *
     * @return The character at the current position.
     */
    public char getCharacter() {
        char nullTerm = '\0';
        if(position == text.length())
            return nullTerm;
        else
            return text.charAt(position++);
    }
}
