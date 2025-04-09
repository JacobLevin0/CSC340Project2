/**
 * ClientInfo.java
 * 
 * Represents information about a connected client in the trivia game server.
 * Stores network info (IP, port), a unique node ID, score tracking, active status,
 * and the client's output stream for TCP communication.
 * 
 * This class is used throughout the server to manage connected clients,
 * track their participation, and send them serialized data during gameplay.
 * 
 * Author: Ethan Kulawiak
 * 
 */

 import java.io.ObjectOutputStream;

 public class ClientInfo {
     private int nodeId;
     private String ip;
     private int port;
     private boolean isActive;
     private int score;
     private transient ObjectOutputStream outputStream; // Updated type
 
     /**
      * Constructs a new ClientInfo object with a node ID, IP, and port.
      * @param nodeId the unique identifier assigned to the client
      * @param ip the IP address of the client
      * @param port the TCP port used by the client
      */
     public ClientInfo(int nodeId, String ip, int port) {
         this.nodeId = nodeId;
         this.ip = ip;
         this.port = port;
         this.isActive = true;
         this.score = 0;
     }
 
     // Getters
 
     /**
      * Returns the client's node ID.
      * @return nodeId
      */
     public int getNodeId() {
         return nodeId;
     }
 
     /**
      * Returns the IP address of the client.
      * @return ip address
      */
     public String getIp() {
         return ip;
     }
 
     /**
      * Returns the TCP port of the client.
      * @return port number
      */
     public int getPort() {
         return port;
     }
 
     /**
      * Checks if the client is currently active.
      * @return true if active, false otherwise
      */
     public boolean isActive() {
         return isActive;
     }
 
     /**
      * Returns the client's current score.
      * @return score
      */
     public int getScore() {
         return score;
     }
 
     /**
      * Returns the client's output stream for TCP communication.
      * @return ObjectOutputStream or null if not set
      */
     public ObjectOutputStream getOutputStream() {
         return outputStream;
     }
 
     // Setters
 
     /**
      * Sets the client's active status.
      * @param active true to mark active, false otherwise
      */
     public void setActive(boolean active) {
         isActive = active;
     }
 
     /**
      * Updates the client's score.
      * @param score the new score
      */
     public void setScore(int score) {
         this.score = score;
     }
 
     /**
      * Sets the IP address of the client.
      * @param ip the new IP address
      */
     public void setIp(String ip) {
         this.ip = ip;
     }
 
     /**
      * Sets the TCP port of the client.
      * @param port the new port number
      */
     public void setPort(int port) {
         this.port = port;
     }
 
     /**
      * Sets the output stream for the client (used for TCP communication).
      * @param outputStream the ObjectOutputStream to store
      */
     public void setOutputStream(ObjectOutputStream outputStream) {
         this.outputStream = outputStream;
     }
 
     /**
      * Returns a string representation of this client, used for debugging.
      */
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