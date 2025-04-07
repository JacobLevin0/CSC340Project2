public class ClientInfo {
    private int nodeId;
    private String ip;
    private int port;
    private boolean isActive;
    private int score;

    public ClientInfo(int nodeId, String ip, int port) {
        this.nodeId = nodeId;
        this.ip = ip;
        this.port = port;
        this.isActive = true;
        this.score = 0;
    }

    // Getters
    public int getNodeId() { return nodeId; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public boolean isActive() { return isActive; }
    public int getScore() { return score; }

    // Setters
    public void setActive(boolean active) { isActive = active; }
    public void setScore(int score) { this.score = score; }

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
