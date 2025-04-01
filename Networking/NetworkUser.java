package Networking;

import java.net.Socket;
import java.util.UUID;




import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

/**
 * A class that represents a client in the network. This class is used to store information such as the client's ID, name, and IP address.
 * It also stores the TCP connection used to send data to the client.
 * @author Haotian
 */
public class NetworkUser implements Serializable { 

    /**
     * True if the client is local, false otherwise.
     */
    public final boolean isLocal;

    /**
     * True if the client is the host, false otherwise.
     */
    private boolean isHost;

    /**
     * The client's IP address.
     */
    private String ipAddress;

    /**
     * The TCP connection used to send data to the client.
     */
    private Socket tcpConnection;

    /**
     * The client's ID.
     */
    private Short id;

    /**
     * The client's name.
     */
    private String name;

    /**
     * The object input stream used to read data from the client.
     */
    private ObjectInputStream in;

    /**
     * The object output stream used to write data to the client.
     */
    private ObjectOutputStream out;
    
    /**
     * The handler used to handle the client's data.
     */
    private NetworkHandler handler;

    /**
     * The time when the client last sent a ping.
     */
    protected Long lastPing = System.currentTimeMillis();

    /**
     * The time when the server last sent a ping to the client.
     */
    protected Long lastServerPing = System.currentTimeMillis();

    /**
     * Constructor for the NetworkUser class.
     * @param name The client's name.
     * @param id The client's ID.
     * @param isLocal True if the client is local, false otherwise.
     * @param handler The handler used to handle the client's data.
     */
    protected NetworkUser(String name, Short id,boolean isLocal, NetworkHandler handler) {
        if (name.equals("Host")){
            this.isHost = true;
        } else {
            this.isHost = false;
        }
        this.isLocal = isLocal;
        this.name = name;
        this.id = id;
        this.handler = handler;
    }

    /**
     * Equals method for the NetworkUser class.
     * @param o The object to compare to.
     * @return True if the two objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkUser client = (NetworkUser) o;

        return id.equals(client.id);
    }

    /**
     * HashCode method for the NetworkUser class.
     * @return The hash code of the object.
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Constructor for the NetworkUser class.
     * @param connection The TCP connection used to send data to the client.
     * @param handler The handler used to handle the client's data.
     */
    protected NetworkUser(Socket connection, NetworkHandler handler) {
        this.isLocal = false;
        this.ipAddress = connection.getInetAddress().getHostAddress();
        this.tcpConnection = connection;
        this.id = handler.getNextId(); 

        try {
            in = new ObjectInputStream(connection.getInputStream());
            out = new ObjectOutputStream(connection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the client's name.
     * @param name The client's new name.
     */
    public void setName(String name) {

        this.name = name;
        
    }

    /**
     * Get the client's IP address.
     * @return The client's IP address.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Get the client's name.
     * @return The client's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the client's ID.
     * @return The client's ID.
     */
    public Short getId() {
        return id;
    }

    
    /**
     * Get the object input stream used to read data from the client.
     * @return The object input stream used to read data from the client.
     */
    protected ObjectInputStream getIn()  {
        return in;
    }
    
    /**
     * Get the object output stream used to write data to the client.
     * @return The object output stream used to write data to the client.
     */
    protected ObjectOutputStream getOut() {
            return out;
    }

    /**
     * Get the TCP connection used to send data to the client.
     * @return The TCP connection used to send data to the client.
     */
    protected Socket getConnection() {
        return tcpConnection;
    }

    /**
     * Send a ping to the client.
     * @throws IOException If there is an error sending the ping.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeShort(id);
        out.writeObject(name);
        out.writeBoolean(isHost);
    }

    /**
     * Read a ping from the client.
     * @throws IOException If there is an error reading the ping.
     * @throws ClassNotFoundException If the class of the object being read is not found.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        id = (Short)(in.readShort());
        name = (String) in.readObject();
        isHost = in.readBoolean();
    }

    /**
     * Ping the client.
     * @throws IOException If there is an error pinging the client.
     */
    protected void pingClient() throws IOException{
        if (System.currentTimeMillis()-lastServerPing < 1000){
            return;
        }
       out.writeObject("PING");
       out.flush();
    }

    /**
     * Update the client's last ping time.
     */
    protected void updatePing(){
        lastServerPing = System.currentTimeMillis();
    }

    /**
     * Is the client the host?
     * @return True if the client is the host, false otherwise.
     */
    public boolean isHost() {
        return isHost;
    }

    /**
     * String representation of the client.
     * @return The string representation of the client.
     */
    public String toString() {
        return name + " | " + isLocal + " | " + id + " | " + isHost;
    }
}
