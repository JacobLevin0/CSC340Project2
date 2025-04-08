
import java.io.*;
import java.io.ObjectOutputStream.PutField;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

/**
 * 
 * @author cjaiswal
 *
 * 
 */

public class TCPServerFile 
{
    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private DataInputStream inStream = null;
    private DataOutputStream outStream = null;
	private DatagramSocket udpSocket;
	private List<BuzzMessage> buzzQueue = new ArrayList<>();
    private final Map<Integer, ClientInfo> clientMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> playerAnswers = new ConcurrentHashMap<>();
    private final Set<Integer> currentParticipants = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> ipToNodeId = new ConcurrentHashMap<>();
    private int nextNodeId = 1;


    public TCPServerFile() {
		try {
            udpSocket = new DatagramSocket(8765);
        	//socket = new DatagramSocket(9876); // Bind tcpserver to port 9876
            //executor = Executors.newFixedThreadPool(3); need to make dynamic 
            //configLoader = new ConfigLoader(); // need to update class to work for trivia
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

	private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return bos.toByteArray();
    }

	private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    private synchronized int assignNodeId() {
        return nextNodeId++;
    }

    public void registerClient(int nodeId, String ip, int port) {
        ClientInfo existing = clientMap.get(nodeId);
        if (existing != null) {
            existing.setIp(ip);
            existing.setPort(port);
            existing.setActive(true);
            System.out.println("Reconnected Node " + nodeId + " at " + ip + ":" + port);
        } else {
            clientMap.put(nodeId, new ClientInfo(nodeId, ip, port));
            System.out.println("Registered new Node " + nodeId + " at " + ip + ":" + port);
        }
    }

    // Set active status
    public void setClientActive(int nodeId, boolean active) {
        ClientInfo client = clientMap.get(nodeId);
        if (client != null) client.setActive(active);
    }

    // Update client score
    public void updateClientScore(int nodeId, int score) {
        ClientInfo client = clientMap.get(nodeId);
        if (client != null) client.setScore(score);
    }

    // Get full client info
    public ClientInfo getClientInfo(int nodeId) {
        return clientMap.get(nodeId);
    }

    // Get list of all clients
    public Collection<ClientInfo> getAllClients() {
        return clientMap.values();
    }

    public void createSocket() {
        try {
            serverSocket = new ServerSocket(3339);
            System.out.println("Server is listening on port 3339...");
    
            while (true) {
                Socket client = serverSocket.accept();
                Thread clientThread = new Thread(new ClientHandler(client));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendQuestionToAll(String question, String[] choices) {
        TCPPacket questionPacket = new TCPPacket(0, "question", prependQuestionToChoices(question, choices), 0);
    
        for (ClientInfo client : getAllClients()) {
            try {
                Socket clientSocket = new Socket(client.getIp(), client.getPort());
                OutputStream outStream = clientSocket.getOutputStream();
                byte[] data = serialize(questionPacket);
                outStream.write(data);
                outStream.flush();
            } catch (IOException e) {
                System.err.println("Failed to send question to Node " + client.getNodeId());
                e.printStackTrace();
            }
        }
    }
    
    private String[] prependQuestionToChoices(String question, String[] choices) {
        String[] result = new String[choices.length + 1];
        result[0] = question;
        System.arraycopy(choices, 0, result, 1, choices.length);
        return result;
    }

    public void runTriviaGame() {
        QuestionsLoader loader = new QuestionsLoader();
        loader.readFIle();
    
        List<Map.Entry<Integer, QuestionsLoader.QuestionInfo>> entries =
            new ArrayList<>(loader.getQuestions().entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getKey)); // Ensure order
    
        for (Map.Entry<Integer, QuestionsLoader.QuestionInfo> entry : entries) {
            QuestionsLoader.QuestionInfo q = entry.getValue();
    
            String[] choices = {q.option1, q.option2, q.option3, q.option4};
            String[] fullPacket = prependQuestionToChoices(q.question, choices);
            TCPPacket questionPacket = new TCPPacket(0, "question", fullPacket, 0);
            TCPPacket timerPacket = new TCPPacket(0, "timer", null, 15);
    
            System.out.println("\nSending Question #" + entry.getKey());
    
            // Step 1: Capture current participants BEFORE question starts
            Set<Integer> currentParticipants = ConcurrentHashMap.newKeySet();
            for (ClientInfo client : getAllClients()) {
                currentParticipants.add(client.getNodeId());
            }
    
            // Step 2: Send question + 15s timer to current participants
            for (ClientInfo client : getAllClients()) {
                if (!currentParticipants.contains(client.getNodeId())) continue;
    
                try (Socket clientSocket = new Socket(client.getIp(), client.getPort());
                     OutputStream out = clientSocket.getOutputStream()) {
    
                    out.write(serialize(questionPacket));
                    out.flush();
    
                    out.write(serialize(timerPacket));
                    out.flush();
    
                } catch (IOException e) {
                    System.err.println("Failed to send question/timer to Node " + client.getNodeId());
                    e.printStackTrace();
                }
            }
    
            // Wait 15 seconds for buzz-in
            try {
                Thread.sleep(15_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    
            // Step 3: Answer phase — cycle through buzzQueue
            while (!buzzQueue.isEmpty()) {
                BuzzMessage buzz = buzzQueue.remove(0);
                int nodeId = buzz.getClientID();
                ClientInfo answeringClient = getClientInfo(nodeId);
                if (answeringClient == null) continue;
    
                // Send negative-ack to all others still in the buzz queue
                for (BuzzMessage b : buzzQueue) {
                    int otherId = b.getClientID();
                    if (otherId == nodeId) continue;
    
                    ClientInfo otherClient = getClientInfo(otherId);
                    if (otherClient != null) {
                        try (Socket socket = new Socket(otherClient.getIp(), otherClient.getPort());
                             OutputStream out = socket.getOutputStream()) {
    
                            TCPPacket nack = new TCPPacket(otherId, "negative-ack", null, 0);
                            out.write(serialize(nack));
                            out.flush();
                            System.out.println("Sent negative-ack to Node " + otherId);
    
                        } catch (IOException e) {
                            System.err.println("Could not send negative-ack to Node " + otherId);
                        }
                    }
                }
    
                // Send ack and 10s timer to current responder
                try (Socket socket = new Socket(answeringClient.getIp(), answeringClient.getPort());
                     OutputStream out = socket.getOutputStream()) {
    
                    out.write(serialize(new TCPPacket(nodeId, "ack", null, 0)));
                    out.write(serialize(new TCPPacket(nodeId, "timer", null, 10)));
                    out.flush();
    
                } catch (IOException e) {
                    System.err.println("Could not send ACK/timer to node " + nodeId);
                    continue;
                }
    
                // Wait for their answer
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
    
                String givenAnswer = playerAnswers.get(nodeId);
                String correctAnswer = q.answer.trim();
    
                try (Socket socket = new Socket(answeringClient.getIp(), answeringClient.getPort());
                     OutputStream out = socket.getOutputStream()) {
    
                    if (givenAnswer == null) {
                        updateClientScore(nodeId, answeringClient.getScore() - 20);
                        out.write(serialize(new TCPPacket(nodeId, "NA", null, 0)));
                        System.out.println("Node " + nodeId + " did not answer.");
                    } else if (givenAnswer.equalsIgnoreCase(correctAnswer)) {
                        updateClientScore(nodeId, answeringClient.getScore() + 10);
                        out.write(serialize(new TCPPacket(nodeId, "correct", null, 0)));
                        out.flush();
                        System.out.println("Node " + nodeId + " answered correctly.");
                        break; // Stop at first correct answer
                    } else {
                        updateClientScore(nodeId, answeringClient.getScore() - 10);
                        out.write(serialize(new TCPPacket(nodeId, "wrong", null, 0)));
                        System.out.println("Node " + nodeId + " answered incorrectly.");
                    }
    
                    out.flush();
                } catch (IOException e) {
                    System.err.println("Could not send result to node " + nodeId);
                    e.printStackTrace();
                }
    
                playerAnswers.remove(nodeId);
            }
    
            // Step 4: Reset state for next question
            buzzQueue.clear();
            playerAnswers.clear();
            System.out.println("Finished evaluating Question #" + entry.getKey());
        }
    
        // Step 5: End of Game — Print and send leaderboard
        System.out.println("\nFinal Leaderboard:");
        List<ClientInfo> sortedClients = new ArrayList<>(getAllClients());
        sortedClients.sort(Comparator.comparingInt(ClientInfo::getScore).reversed());
    
        List<String> leaderboardLines = new ArrayList<>();
    
        for (ClientInfo client : sortedClients) {
            String line = "Node " + client.getNodeId() + " - Score: " + client.getScore();
            leaderboardLines.add(line);
            System.out.println(line);
        }
    
        String[] leaderboardData = leaderboardLines.toArray(new String[0]);
        for (ClientInfo client : sortedClients) {
            try (Socket socket = new Socket(client.getIp(), client.getPort());
                 OutputStream out = socket.getOutputStream()) {
    
                TCPPacket resultsPacket = new TCPPacket(client.getNodeId(), "results", leaderboardData, 0);
                out.write(serialize(resultsPacket));
                out.flush();
    
            } catch (IOException e) {
                System.err.println("Could not send leaderboard to Node " + client.getNodeId());
                e.printStackTrace();
            }
        }
    
        System.out.println("\nAll questions complete!");
    }

	private Runnable listenerTask = () -> {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    try {
        while (true) {
            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            udpSocket.receive(packet);

            Object received = deserialize(packet.getData());
            
            if (received instanceof BuzzMessage) {
                BuzzMessage incoming = (BuzzMessage) received;
                int clientID = incoming.getClientID();
                LocalDateTime incomingTime = LocalDateTime.parse(incoming.getTime(), formatter);

                Optional<BuzzMessage> existing = buzzQueue.stream()
                        .filter(msg -> msg.getClientID() == clientID)
                        .findFirst();

                if (existing.isPresent()) {
                    LocalDateTime existingTime = LocalDateTime.parse(existing.get().getTime(), formatter);

                    if (incomingTime.isBefore(existingTime)) {
                        // Replace with earlier buzz
                        buzzQueue.remove(existing.get());
                        buzzQueue.add(incoming);
                        System.out.println("Replaced Node " + clientID + " with earlier Buzz.");
                    } else {
                        System.out.println("Ignored later Buzz from Node " + clientID);
                    }
                } else {
                    buzzQueue.add(incoming);
                    System.out.println("Buzz received from Node " + clientID + ". Added to buzzQueue.");
                }

                // Sort the queue based on time
                buzzQueue.sort(Comparator.comparing(b -> LocalDateTime.parse(b.getTime(), formatter)));

                // Optional: display current queue
                System.out.println("Current Buzz Queue:");
                buzzQueue.forEach(msg ->
                        System.out.println("  Node " + msg.getClientID() + " at " + msg.getTime()));
            } else {
                System.err.println("Received unknown object type.");
            }
        }
    } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
    }
};

private class ClientHandler implements Runnable {
    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private int nodeId;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;

        try {
            this.in = new DataInputStream(clientSocket.getInputStream());
            this.out = new DataOutputStream(clientSocket.getOutputStream());

            String clientIp = clientSocket.getInetAddress().getHostAddress();
            int clientPort = clientSocket.getPort();

            // Check if this client has connected before
            Integer existingId = ipToNodeId.get(clientIp);
            if (existingId != null) {
                this.nodeId = existingId;
            } else {
                this.nodeId = assignNodeId();
                ipToNodeId.put(clientIp, this.nodeId);
            }

            // Register or update the client
            registerClient(nodeId, clientIp, clientPort);

            // Send nodeId back to client
            TCPPacket idPacket = new TCPPacket(nodeId, "id", null, 0);
            out.write(serialize(idPacket));
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        System.out.println("Client connected: " + clientSocket.getInetAddress());
        try {
            while (!clientSocket.isClosed()) {
                byte[] readBuffer = new byte[200];
                int bytesRead = in.read(readBuffer);
                if (bytesRead == -1) {
                    break; // Client disconnected
                }
    
                TCPPacket packet = (TCPPacket) deserialize(readBuffer);
    
                if ("My Answer".equals(packet.getMessage())) {
                    String[] data = packet.getData();
                    if (data != null && data.length > 0) {
                        playerAnswers.put(packet.getClientId(), data[0]);
                        System.out.println("Received answer from Node " + packet.getClientId() + ": " + data[0]);
                    }
                }
    
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected.");
        } finally {
            try {
                clientSocket.close();
                System.out.println("Closed client socket.");
        
                // Mark as inactive
                ClientInfo client = getClientInfo(nodeId);
                if (client != null) {
                    client.setActive(false);
                    System.out.println("Node " + nodeId + " marked inactive.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
public static void main(String[] args) {
    TCPServerFile fileServer = new TCPServerFile();
    new Thread(fileServer.listenerTask).start(); // UDP listener
    fileServer.createSocket(); // TCP listener

    new Thread(() -> {
        try {
            Thread.sleep(5000); // give time for clients to connect
            fileServer.runTriviaGame();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }).start();
}
}