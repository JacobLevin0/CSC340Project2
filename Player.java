import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalTime;
/**
 * 
 * @author cjaiswal
 *
 * 
 */
public class Player 
{
    private Socket socket = null;
    private DatagramSocket UDPSocket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
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

    /**
     * Deserializes a byte array back into an object.
     * 
     * @param data The byte array to deserialize.
     * @return The deserialized object.
     * @throws IOException If an I/O error occurs during deserialization.
     * @throws ClassNotFoundException If the class of the serialized object cannot be found.
     */
    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }
    
    public Player() 
    {
    	//create a socket to connect to localHost's (127.0.0.1) port 3339
        try 
        {
			socket = new Socket("localHost", 3339);
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

    public void createSocket()
    {
        try 
        {
        	//fetch the streams
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
            createReadThread();
            //createWriteThread();
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
                        byte[] readBuffer = new byte[200];
                        int num = inStream.read(readBuffer);
                        if (num > 0) 
                        {
                            byte[] arrayBytes = new byte[num];
                            System.arraycopy(readBuffer, 0, arrayBytes, 0, num);
                            TCPPacket rec = (TCPPacket) deserialize(arrayBytes);
                            //String recvedMessage = new String(arrayBytes, "UTF-8");
                            switch (rec.getMessage()) {
                                case "id":
                                    clientID = rec.getClientId();
                                    break;
                                case "question":
                                    window.updateQuestion(rec.getData());
                                    break;
                                case "timer":
                                    window.updateTimerDuration(rec.getScore());
                                    break;
                                case "results":
                                    System.out.println();
                                    break;
                                case "correct":
                                    window.updateScore(10);
                                    break;
                                case "wrong":
                                    window.updateScore(-10);
                                    break;
                                case "NA":
                                    window.updateScore(-20);
                                    break;
                                case "ack":
                                    System.out.println(rec.getMessage());
                                    window.setStatus(true);
                                    break;
                                case "negative-ack":
                                    System.out.println(rec.getMessage());
                                    window.setStatus(false);
                                    break;
                                default:
                                    System.out.println("Error: No message matched on recieving packet");
                                    break;
                            }
                            
                            System.out.println("Received message :" + rec);
                        }
                        else 
                        {
                        	notifyAll();
                        }
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

    public void createWriteThread(String TCPMessage) 
    {
        Thread writeThread = new Thread() 
        {
            public void run() 
            {
                	try 
                	{
                        /*BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                        sleep(100);
                        String typedMessage = inputReader.readLine();*/
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
                        
                        if (packet != null) 
                        {
                            byte[] sendPacket = serialize(packet);
                            synchronized (socket) 
                            {
                                outStream.write(sendPacket);
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

    public void poll(){
        Thread buzzThread = new Thread(){
            public void run(){
                int clientID = 1;
                
                try {
                    LocalDateTime time = LocalDateTime.now();
                    BuzzMessage packet = new BuzzMessage(clientID, 1, "buzz", time.toString());
                    InetAddress serverAddress = InetAddress.getLocalHost(); //fill in
                    int serverPort = 8765; //fill in
                    try {
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
        Player myChatClient = new Player();
        myChatClient.createSocket();
        /*myChatClient.createReadThread();
�       myChatClient.createWriteThread();*/
    }
}