import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
/**
 * This class creates a client for a trivia game 
 * @author cjaiswal, modified by jlevin
 * 
 * 
 */
public class Player 
{
    private Socket socket = null;
    private DatagramSocket UDPSocket = null;
    private ObjectInputStream inStream = null;
    private ObjectOutputStream outStream = null;
    private ClientWindow window; 
    private int clientID;

    /**
     * Serializes an object into a byte array for sending over UDP.
     * 
     * @param obj The object to serialize.
     * @return The serialized byte array.
     * @throws IOException If an I/O error occurs during serialization.
     */
    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return bos.toByteArray();
    }
    
    /*
     * Constructor for Player
     * 
     * @throws IOException If an I/O error occurs during serialization.
     * @throws UnknownHostException If the inputed IP can't be found
     */
    public Player() 
    {
    	//create a socket to connect to the server's IP on port 3339
        try 
        {
			socket = new Socket( "10.111.160.107", 3339); //EDIT THIS LINE IF YOU WANT TO CHANGE THE SERVER
            UDPSocket = new DatagramSocket(8766);
			System.out.println("Connected!");
		} 
        catch (UnknownHostException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        catch (IOException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //UDPSocket = new DatagramSocket();
        window = new ClientWindow(this);
    }

    /**
     * Defines the input and output streams and starts a reading thread
     * 
     * @throws IOException If an I/O error occurs during serialization.
     * @throws UnknownHostException If the inputed IP can't be found
     */
    public void createSocket()
    {
        try 
        {
        	//fetch the streams
            inStream = new ObjectInputStream(socket.getInputStream());
            outStream = new ObjectOutputStream(socket.getOutputStream());
            createReadThread();
        } 
        catch (UnknownHostException u) 
        {
            u.printStackTrace();
        } 
        catch (IOException io) 
        {
            io.printStackTrace();
        }
    }

    /**
     * Creates a thread that listens for incoming packets through TCP
     * 
     * @throws SocketException If there's trouble accessing a socket
     * @throws IOException If an I/O error occurs during serialization.
     * @throws ClassNotFoundException If the TCPPacket is not found as a class as the incoming object is cast to it
     */
    public void createReadThread() 
    {
        Thread readThread = new Thread() 
        {
            public void run() 
            {
                while (socket.isConnected()) 
                {
                    try 
                    {
                        //accept incoming packet
                        Object obj = inStream.readObject();
                        TCPPacket rec = (TCPPacket) obj;

                        switch (rec.getMessage()) { //depending on the received message, do various things
                            case "id": //set client id
                                clientID = rec.getClientId();
                                window.setStatus(false);
                                break;
                            case "question": //update client window with the next quesiton
                                window.updateQuestion(rec.getData());
                                break;
                            case "next-question": //reseting the client window following a quesiton
                                window.resetForNextQuestion();
                                break;
                            case "timer": //update the timer in the window
                                window.updateTimerDuration(rec.getScore());
                                break;
                            case "results": //display the result of the game
                                window.displayResults(rec.getData());
                                break;
                            case "correct": //+10 if user was correct
                                window.updateScore(10);
                                break;
                            case "wrong": //-10 if user was wrong
                                window.updateScore(-10);
                                break;
                            case "NA": //-20 if user didn't answer
                                window.updateScore(-20);
                                break;
                            case "ack": //able to answer
                                System.out.println(rec.getMessage());
                                window.setStatus(true);
                                break;
                            case "negative-ack": //not able to answer
                                System.out.println(rec.getMessage());
                                window.setStatus(false);
                                break;
                            case "score": //get score if returning or displaying results
                                window.updateScore(rec.getScore());
                            default:
                                System.out.println("Error: No message matched on recieving packet");
                                break;
                        }
                            
                        System.out.println("Received message :" + rec.getMessage());
                    }
                    catch (SocketException se)
                    {
                        System.exit(0);
                    }
                    catch (IOException i) 
                    {
                        i.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        };
        readThread.setPriority(Thread.MAX_PRIORITY);
        readThread.start();
    }

    /**
     * Creates a thread that sends outgoing packets through TCP
     * 
     * @param TCPMessage The message that goes in the outgoing packet
     * @throws IOException If an I/O error occurs during serialization.
     * @throws InterruptedException For when the thread is asleep
     */
    public void createWriteThread(String TCPMessage) 
    {
        Thread writeThread = new Thread() 
        {
            public void run() 
            {
                	try 
                	{
                        //make the packet based on the TCPMessage
                        TCPPacket packet = null;
                        switch (TCPMessage) {
                            case "My Answer":
                                String[] data = {window.getAnswer()};
                                packet = new TCPPacket(clientID, TCPMessage, data, 0);
                                break;
                            default:
                                System.out.println("Error: No packet with that message can be created");
                                break;
                        }
                        //send packet if not null
                        if (packet != null) 
                        {
                            synchronized (socket) 
                            {
                                outStream.writeObject(packet);
                            }
                            sleep(100);
                        }
                        else
                        {
                        	notifyAll();
                        }
                    } 
                	catch (IOException i) 
                	{
                        i.printStackTrace();
                    } 
                	catch (InterruptedException ie) 
                	{
                        ie.printStackTrace();
                    }
                }
        };
        writeThread.setPriority(Thread.MAX_PRIORITY);
        writeThread.start();
    }

    /**
     * Creates a thread that sends outgoing packets through UDP
     * 
     * @throws IOException If an I/O error occurs during serialization.
     * @throws Exception For any general exception
     */
    public void poll(){
        Thread buzzThread = new Thread(){
            public void run(){
                try {
                    //create custom packet
                    LocalDateTime time = LocalDateTime.now();
                    BuzzMessage packet = new BuzzMessage(clientID, 1, "buzz", time.toString());
                    InetAddress serverAddress = socket.getInetAddress(); //fill in
                    int serverPort = 8765; //fill in
                    try {
                        //create and send datagram packet
                        byte[] data = serialize(packet);
                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, serverPort);
                        UDPSocket.send(sendPacket);
                        System.out.println("Poll sent to server");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        buzzThread.start();
    }
    

    public static void main(String[] args) throws Exception 
    {
        Player player = new Player();
        player.createSocket();
    }
}