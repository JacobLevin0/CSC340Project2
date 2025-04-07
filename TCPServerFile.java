
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
        clientMap.put(nodeId, new ClientInfo(nodeId, ip, port));
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
    
    public void sendFile()
    {
    	final int MAX_BUFFER = 1000;
    	byte [] data = null;
    	int bufferSize = 0;
    	/*try
    	{
    		//	write the filename below in the File constructor
    		File file = new File("Assignment-5-Fall18.pdf");
    		FileInputStream fileInput = new FileInputStream(file);
    		//get the file length
    		long fileSize = file.length();
    		
    		System.out.println("File size at server is: " + fileSize + " bytes");
    		//first send the size of the file to the client
    		outStream.writeLong(fileSize);
    		outStream.flush();

    		//Now send the file contents
    		if(fileSize > MAX_BUFFER)
    			bufferSize = MAX_BUFFER;
    		else 
    			bufferSize = (int)fileSize;
    		
    		data = new byte[bufferSize];
    		
    		long totalBytesRead = 0;
    		while(true)
    		{
    			//read upto MAX_BUFFER number of bytes from file
    			int readBytes = fileInput.read(data);
    			//send readBytes number of bytes to the client
        		outStream.write(data);
        		outStream.flush();

        		//stop if EOF
    			if(readBytes == -1)//EOF
    				break;
    			
    			totalBytesRead = totalBytesRead + readBytes;
    			
    			//stop if fileLength number of bytes are read
    			if(totalBytesRead == fileSize)
    				break;
    			
    			////update fileSize for the last remaining block of data
    			if((fileSize-totalBytesRead) < MAX_BUFFER)
    				bufferSize = (int) (fileSize-totalBytesRead);
    			
    			//reinitialize the data buffer
    			data = new byte[bufferSize];
    		}
    		
    		fileInput.close();
    		serverSocket.close();
    		socket.close();
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}*/
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
        this.nodeId = assignNodeId();

        try {
            this.in = new DataInputStream(clientSocket.getInputStream());
            this.out = new DataOutputStream(clientSocket.getOutputStream());

            String clientIp = clientSocket.getInetAddress().getHostAddress();
            int clientPort = clientSocket.getPort();

            registerClient(nodeId, clientIp, clientPort);
            System.out.println("Registered client " + nodeId + " from " + clientIp + ":" + clientPort);
            
            // Optionally, send nodeId back to client
            out.writeInt(nodeId);
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
                // Handle data here if needed
                System.out.println("Received from client: " + new String(readBuffer, 0, bytesRead));
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        } finally {
            try {
                clientSocket.close();
                System.out.println("Closed client socket.");
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
}
}