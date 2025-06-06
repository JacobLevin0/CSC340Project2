
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
import java.util.Arrays;
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
 * TCPServerFile.java
 * 
 * This class serves as the server-side controller for a multiplayer trivia game.
 * It handles both TCP and UDP communication with clients, manages player sessions,
 * distributes questions, tracks buzz-ins and scores, and supports reconnections.
 * 
 * Key features:
 * - Uses TCP for structured communication (questions, scores, results, timers).
 * - Uses UDP for low-latency buzz-in detection.
 * - Dynamically manages connected clients and unique node IDs.
 * - Runs a real-time, multi-player trivia game loop.
 * 
 * Author: Ethan Kulawiak
 */

public class TCPServerFile {
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
    private final Set<Integer> answeredList = ConcurrentHashMap.newKeySet();
    private int nextNodeId = 1;

    public TCPServerFile() {
        try {
            udpSocket = new DatagramSocket(8765);
            // socket = new DatagramSocket(9876); // Bind tcpserver to port 9876
            // executor = Executors.newFixedThreadPool(3); need to make dynamic
            // configLoader = new ConfigLoader(); // need to update class to work for trivia
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
        if (client != null)
            client.setActive(active);
    }

    // Update client score
    public void updateClientScore(int nodeId, int score) {
        ClientInfo client = clientMap.get(nodeId);
        if (client != null)
            client.setScore(score);
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

        List<Map.Entry<Integer, QuestionsLoader.QuestionInfo>> entries = new ArrayList<>(
                loader.getQuestions().entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getKey));

        for (Map.Entry<Integer, QuestionsLoader.QuestionInfo> entry : entries) {
            QuestionsLoader.QuestionInfo q = entry.getValue();

            String[] choices = { q.option1, q.option2, q.option3, q.option4 };
            String[] fullPacket = prependQuestionToChoices(q.question, choices);
            TCPPacket questionPacket = new TCPPacket(0, "question", fullPacket, 0);
            TCPPacket timerPacket = new TCPPacket(0, "timer", null, 15);

            System.out.println("\nSending Question #" + entry.getKey());

            Set<Integer> currentParticipants = ConcurrentHashMap.newKeySet();
            for (ClientInfo client : getAllClients()) {
                currentParticipants.add(client.getNodeId());
            }

            for (ClientInfo client : getAllClients()) {
                if (!currentParticipants.contains(client.getNodeId()))
                    continue;
                ObjectOutputStream out = client.getOutputStream();
                if (out != null) {
                    try {
                        out.writeObject(questionPacket);
                        out.flush();
                        out.writeObject(timerPacket);
                        out.flush();
                    } catch (IOException e) {
                        System.err.println("Failed to send to Node " + client.getNodeId());
                    }
                }
            }
    
            try { Thread.sleep(15000); } catch (InterruptedException e) { e.printStackTrace(); }

            answeredList.clear();
    
            while (!buzzQueue.isEmpty()) {
                BuzzMessage buzz = buzzQueue.remove(0);
                int nodeId = buzz.getClientID();
                if (answeredList.contains(nodeId)) continue;

                answeredList.add(nodeId);

                ClientInfo answeringClient = getClientInfo(nodeId);
                if (answeringClient == null)
                    continue;

                for (BuzzMessage b : buzzQueue) {
                    int otherId = b.getClientID();
                    if (otherId == nodeId)
                        continue;
                    if (otherId == nodeId || answeredList.contains(otherId)) continue;
                    ClientInfo otherClient = getClientInfo(otherId);
                    ObjectOutputStream out = otherClient != null ? otherClient.getOutputStream() : null;
                    if (out != null) {
                        try {
                            out.writeObject(new TCPPacket(otherId, "negative-ack", null, 0));
                            out.flush();
                        } catch (IOException ignored) {
                        }
                    }
                }


                ObjectOutputStream out = answeringClient.getOutputStream();
                if (out != null) {
                    try {
                        out.writeObject(new TCPPacket(nodeId, "ack", null, 0));
                        out.writeObject(new TCPPacket(nodeId, "timer", null, 10));
                        out.flush();
                    } catch (IOException e) {
                        System.err.println("Could not send ACK/timer to node " + nodeId);
                        continue;
                    }
                }

                try { Thread.sleep(10000); } catch (InterruptedException e) { e.printStackTrace(); }

                String givenAnswer = playerAnswers.get(nodeId);
                String correctAnswer = q.answer.trim();
                out = answeringClient.getOutputStream();
                if (out != null) {
                    try {
                        if (givenAnswer == null) {
                            updateClientScore(nodeId, answeringClient.getScore() - 20);
                            out.writeObject(new TCPPacket(nodeId, "NA", null, 0));
                        } else if (givenAnswer.equalsIgnoreCase(correctAnswer)) {
                            updateClientScore(nodeId, answeringClient.getScore() + 10);
                            out.writeObject(new TCPPacket(nodeId, "correct", null, 0));
                            out.flush();
                            break;
                        } else {
                            updateClientScore(nodeId, answeringClient.getScore() - 10);
                            out.writeObject(new TCPPacket(nodeId, "wrong", null, 0));
                        }
                        out.flush();
                    } catch (IOException ignored) {
                    }
                }
                playerAnswers.remove(nodeId);
            }

            buzzQueue.clear();
            playerAnswers.clear();
            answeredList.clear(); 
            for (ClientInfo client : getAllClients()) {
                ObjectOutputStream out = client.getOutputStream();
                if (out != null) {
                    try {
                        out.writeObject(new TCPPacket(client.getNodeId(), "negative-ack", null, 0));
                        out.flush();
                    } catch (IOException ignored) {}
                }
            }
            TCPPacket resetPacket = new TCPPacket(0, "next-question", null, 0);
            for (ClientInfo client : getAllClients()) {
                ObjectOutputStream out = client.getOutputStream();
                if (out != null) {
                    try {
                        out.writeObject(resetPacket);
                        out.flush();
                    } catch (IOException ignored) {
                    }
                }
            }
            System.out.println("Finished evaluating Question #" + entry.getKey());
        }

        // Print + send leaderboard
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
            ObjectOutputStream out = client.getOutputStream();
            if (out != null) {
                try {
                    out.writeObject(new TCPPacket(client.getNodeId(), "results", leaderboardData, 0));
                    out.flush();
                } catch (IOException ignored) {
                }
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
                    buzzQueue
                            .forEach(msg -> System.out.println("  Node " + msg.getClientID() + " at " + msg.getTime()));
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
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private int nodeId;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;

            try {
                this.out = new ObjectOutputStream(clientSocket.getOutputStream());
                this.in = new ObjectInputStream(clientSocket.getInputStream());

                String clientIp = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();

                Integer existingId = ipToNodeId.get(clientIp);
                if (existingId != null) {
                    this.nodeId = existingId;
                } else {
                    this.nodeId = assignNodeId();
                    ipToNodeId.put(clientIp, this.nodeId);
                }

                registerClient(nodeId, clientIp, clientPort);

                ClientInfo client = getClientInfo(nodeId);
                if (client != null) {
                    client.setOutputStream(out);
                }

                out.writeObject(new TCPPacket(nodeId, "id", null, 0));
                out.writeObject(new TCPPacket(nodeId, "score", null, getClientInfo(nodeId).getScore()));
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            System.out.println("Client connected: " + clientSocket.getInetAddress());
            try {
                while (!clientSocket.isClosed()) {
                    TCPPacket packet = (TCPPacket) in.readObject();

                    if ("My Answer".equals(packet.getMessage())) {
                        String[] data = packet.getData();
                        if (data != null && data.length > 0) {
                            playerAnswers.put(packet.getClientId(), data[0]);
                            System.out.println("Received answer from Node " + packet.getClientId() + ": " + data[0]);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client " + nodeId + " disconnected.");
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("Closed client socket.");
                    ClientInfo client = getClientInfo(nodeId);
                    if (client != null) {
                        client.setActive(false);
                        client.setOutputStream(null);
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
        new Thread(fileServer::createSocket).start();

    new Thread(() -> {
        try {
            Thread.sleep(10000); // give time for clients to connect
            System.out.println("Starting trivia game...");
            fileServer.runTriviaGame();
        } catch (InterruptedException e) {
            System.err.println("Trivia thread was interrupted.");
            e.printStackTrace();
        }
    }).start();
}
}