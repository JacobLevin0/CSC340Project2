import java.io.Serializable;

/*
 * Represents a TCP packet used for communication between the client and server
 * in a trivia game. This class holds a client ID, a message string, an array of
 * string data, and a score value. It implements Serializable to allow the object
 * to be sent over a network.
 * 
 * <p>Typical usage includes transferring question data, answers, and game state.</p>
 * 
 * @author Omar Fofana
 */
public class TCPPacket implements Serializable {

    private int clientId;
    private String message;
    private String data[];
    private int score;

    /**
     * Constructs a new TCPPacket with the specified client ID, message, data array, and score.
     *
     * @param clientId the ID of the client sending or receiving the packet
     * @param message  the type or content of the message
     * @param data     an array of strings containing packet data (e.g., question, options, results)
     * @param score    the score of the client at the time of packet creation
     */
    public TCPPacket(int clientId, String message, String[] data, int score) {
        this.clientId = clientId;
        this.message = message;
        this.score = score;
        this.data = data;
    }

    /**
     * Returns the ID of the client.
     *
     * @return the client ID
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * Sets the client ID.
     *
     * @param clientId the client ID to set
     */
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    /**
     * Returns the message associated with the packet.
     *
     * @return the message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message content.
     *
     * @param message the message string to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the score value in the packet.
     *
     * @return the client's score
     */
    public int getScore() {
        return score;
    }

    /**
     * Sets the score value in the packet.
     *
     * @param score the score to set
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Returns the data array associated with the packet.
     *
     * @return an array of strings containing packet data
     */
    public String[] getData() {
        return data;
    }
}