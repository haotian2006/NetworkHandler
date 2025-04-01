package Networking;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * A packet class that is used to send data between clients and servers in a network.
 * This class is used to wrap the data being sent and add additional information such as the
 * client ID and the remote ID
 * @author haotian
 */
public class Packet implements Serializable {
    private static  long currentID = 0;
    protected Byte RemoteID;
    protected Short clientID;
    private Serializable Payload; 
    public long ID = 0;//currentID<|end_header|>

    /**
     * Constructor for a packet with no client ID or remote ID
     */
    public Packet() {
  
    }

    /**
     * Constructor for a packet with a client ID
     * @param clientId the client ID
     */
    protected Packet(Short clientId) {
        this.clientID = clientId;
    }

    
    /**
     * Constructor for a packet with a client ID and a remote ID
     * @param remoteId the remote ID
     * @param clientId the client ID
     */
    private Packet(Serializable Payload, Byte RemoteID,Short clientId) {
        this.clientID = clientId;
        this.Payload = Payload;
        this.RemoteID = RemoteID;
    }

    /**
     * Get the client ID of the packet
     * @return the client ID
     */
    public Short getClientID() {
        return clientID;
    }

    /**
     * Add an object to the payload of the packet
     * @param object the object to add
     */
    public void addToPayLoad(Serializable object) {
        Payload = object;
    }

    /**
     * Get the payload of the packet
     * @return the payload
     */
    public Serializable getPayLoad() {
        return Payload;
    }
    /**
     * Attach information to the packet such as the client ID and the remote ID
     * @param remoteId the remote ID
     * @param clientId the client ID
     * @return a new packet with the attached information
     */
    protected Packet attachInfo(Byte remoteId, Short clientId) {
        return new Packet(Payload, remoteId,clientId);
    }

    /**
     * Attach information to the packet such as the remote ID
     * @param remoteId the remote ID
     * @return a new packet with the attached information
     */
    protected Packet attachInfo(Byte remoteId) {
        return new Packet(Payload, remoteId,null);
    }

    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeShort(clientID);
        out.writeByte(RemoteID);

       // out.writeLong(ID);
        try {
            out.writeObject(Payload);
        }catch (java.io.NotSerializableException e){
            System.out.println("Not serializable");
            System.out.println(Payload);
            System.out.println(Payload.toString());
            System.out.println(e);
        }


    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        clientID = in.readShort();
        RemoteID = in.readByte();

       // ID = in.readLong();

        try {
            Payload = (Serializable)in.readObject();
        } catch (Exception e) {
            Payload = null;
            e.printStackTrace();
        }

    }

    /**
     * Get a string representation of the packet
     * @return a string representation of the packet
     */
    public String toString() {
        return String.format("Packet: %s ID: %s RemoteID: %s", Payload, ID, RemoteID-Byte.MIN_VALUE);
    }
}

