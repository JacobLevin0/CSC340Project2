import java.io.Serializable;

/**
 * Represents a message sent when a client "buzzes in" during a trivia game.
 * It contains the client ID, the question number being answered, the actual message content,
 * and a timestamp of when the buzz occurred.
 * <p>
 * This class implements Serializable so it can be sent over a network or saved to a file.
 * </p>
 * 
 * @author Omar Fofana
 */
public class BuzzMessage implements Serializable {
    
    private int clientID;
    private int questionNumber;
    private String message;
    private String time;

    /**
     * Constructs a new BuzzMessage with the given client ID, question number, message, and timestamp.
     * 
     * @param clientID        The ID of the client who buzzed in.
     * @param questionNumber  The number of the question being answered.
     * @param message         The content of the message (e.g., "buzz" or answer text).
     * @param time            The timestamp indicating when the buzz occurred.
     */
    public BuzzMessage(int clientID, int questionNumber, String message, String time) {
        this.clientID = clientID;
        this.questionNumber = questionNumber;
        this.message = message;
        this.time = time;
    }

    /**
     * Returns the ID of the client who sent the message.
     * 
     * @return The client ID.
     */
    public int getClientID() {
        return clientID;
    }

    /**
     * Sets the ID of the client.
     * 
     * @param clientID The client ID to set.
     */
    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    /**
     * Returns the question number associated with the buzz.
     * 
     * @return The question number.
     */
    public int getQuestionNumber() {
        return questionNumber;
    }

    /**
     * Sets the question number.
     * 
     * @param questionNumber The question number to set.
     */
    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    /**
     * Returns the message content.
     * 
     * @return The message content.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message content.
     * 
     * @param message The message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the timestamp for when the buzz occurred.
     * 
     * @return The timestamp string.
     */
    public String getTime() {
        return time;
    }

    /**
     * Sets the timestamp.
     * 
     * @param time The timestamp string to set.
     */
    public void setTime(String time) {
        this.time = time;
    }
}
