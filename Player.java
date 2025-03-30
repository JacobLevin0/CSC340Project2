import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
/**
 * 
 * @author cjaiswal
 *
 * 
 */
public class Player 
{
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;

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
        ClientWindow window = new ClientWindow();
    }

    public void createSocket()
    {
        try 
        {
        	//fetch the streams
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
            createReadThread();
            createWriteThread();
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
                            String recvedMessage = new String(arrayBytes, "UTF-8");
                            System.out.println("Received message :" + recvedMessage);
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
                    }
                }
            }
        };
        readThread.setPriority(Thread.MAX_PRIORITY);
        readThread.start();
    }

    public void createWriteThread() 
    {
        Thread writeThread = new Thread() 
        {
            public void run() 
            {
                while (socket.isConnected()) 
                {
                	try 
                	{
                        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                        sleep(100);
                        String typedMessage = inputReader.readLine();
                        if (typedMessage != null && typedMessage.length() > 0) 
                        {
                            synchronized (socket) 
                            {
                                outStream.write(typedMessage.getBytes("UTF-8"));
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
            }
        };
        writeThread.setPriority(Thread.MAX_PRIORITY);
        writeThread.start();
    }

    public static void main(String[] args) throws Exception 
    {
        Player myChatClient = new Player();
        myChatClient.createSocket();
        /*myChatClient.createReadThread();
�       myChatClient.createWriteThread();*/
    }
}