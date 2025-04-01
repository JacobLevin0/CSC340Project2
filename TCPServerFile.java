
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
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


    public TCPServerFile() {
		try {
            udpSocket = new DatagramSocket(1899);
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

    public void createSocket() 
    {
        try 
        {
        	//create Server and start listening
            serverSocket = new ServerSocket(3339);
            //accept the connection
            socket = serverSocket.accept();
            //fetch the streams
            inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connected");
        }
        catch (IOException io) 
        {
            io.printStackTrace();
        }
    }
    
    public void sendFile()
    {
    	final int MAX_BUFFER = 1000;
    	byte [] data = null;
    	int bufferSize = 0;
    	try
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
    	}
    }

	private Runnable listenerTask = () -> {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    try {
        while (true) {
            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            udpSocket.receive(packet);

            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
            Object received = ois.readObject();

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
    public static void main(String[] args)
    {
    	TCPServerFile fileServer = new TCPServerFile();
		new Thread(fileServer.listenerTask).start();
        fileServer.createSocket();
        fileServer.sendFile();
    }
}