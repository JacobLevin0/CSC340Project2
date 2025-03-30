import java.io.Serializable;

public class TCPPacket implements Serializable {
    

    
    private int clientId;

    private String message;

    //Array of objects
    private String data[];

    private int score;

    

    public TCPPacket(int clientId, String message, int score) {
        this.clientId = clientId;
        this.message = message;
        this.score = score;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }



    
}
