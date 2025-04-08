import java.io.ObjectOutputStream;

public class ClientInfo {
    private int nodeId;
    private String ip;
    private int port;
    private boolean isActive;
    private int score;
    private transient ObjectOutputStream outputStream; // Updated type

    public ClientInfo(int nodeId, String ip, int port) {
        this.nodeId = nodeId;
        this.ip = ip;
        this.port = port;
        this.isActive = true;
        this.score = 0;
    }

    // Getters
    public int getNodeId() {
        return nodeId;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getScore() {
        return score;
    }

    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    // Setters
    public void setActive(boolean active) {
        isActive = active;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setOutputStream(ObjectOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public String toString() {
        return "ClientInfo{" +
                "nodeId=" + nodeId +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", active=" + isActive +
                ", score=" + score +
                '}';
    }
}