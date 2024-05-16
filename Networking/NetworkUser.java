package Networking;

import java.net.Socket;
import java.util.UUID;




import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class NetworkUser implements Serializable {

    public final boolean isLocal;
    private String ipAddress;
    private Socket tcpConnection;
    private Short id;
    private String name;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    private NetworkHandler handler;

    protected Long lastPing = System.currentTimeMillis();

    protected NetworkUser(String name, Short id,boolean isLocal, NetworkHandler handler) {
        this.isLocal = isLocal;
        this.name = name;
        this.id = id;
        this.handler = handler;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkUser client = (NetworkUser) o;

        return id.equals(client.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }



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

    
    public void setName(String name) {
        this.name = name;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getName() {
        return name;
    }

    public Short getId() {
        return id;
    }

    
    protected ObjectInputStream getIn()  {
        return in;
    }
    
    protected ObjectOutputStream getOut() {
            return out;
    }

    protected Socket getConnection() {
        return tcpConnection;
    }
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeShort(id);
        out.writeObject(name);
    }


    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        id = (Short)(in.readShort());
        name = (String) in.readObject();
    }

    public String toString() {
        return name + " | " + isLocal + " | " + id;
    }

}
