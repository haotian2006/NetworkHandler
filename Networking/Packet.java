package Networking;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Packet implements Serializable {
    protected Byte RemoteID;
    protected Short clientID;
    private List<Serializable> Payload;

    public Packet() {
        Payload = new ArrayList<Serializable>();
    }

    protected Packet(Short clientId) {
        this.clientID = clientId;
        Payload = new ArrayList<Serializable>();
    }

    
    private Packet(List<Serializable> Payload, Byte RemoteID,Short clientId) {
        this.clientID = clientId;
        this.Payload = Payload;
        this.RemoteID = RemoteID;
    }

    public Short getClientID() {
        return clientID;
    }

    public void addToPayLoad(Serializable object) {
        Payload.add(object);
    }

    public List<Serializable> getPayLoad() {
        return Payload;
    }



    protected Packet attachInfo(Byte remoteId, Short clientId) {
        return new Packet(Payload, remoteId,clientId);
    }

    protected Packet attachInfo(Byte remoteId) {
        return new Packet(Payload, remoteId,null);
    }
}
