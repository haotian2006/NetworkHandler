package Networking;


import java.io.IOError;
import java.io.IOException;
import java.util.function.Function;

import Networking.Signal.*;
// THIS CAN BE OPTIMIZED FURTHER IF WE SEND A UNIQUE REMOTE ID THATS AN UINT8 FOR EACH REMOTE
public abstract class Remote {
    public final String name;
    protected Signal<Packet> onMessageSignal;
    public final Event<Packet> onMessage;


    protected Remote(String name) {
        this.name = name;
        onMessageSignal = new Signal<Packet>();
        onMessage = onMessageSignal.event;
    }

    public abstract void fireServer(Packet data) throws IOException;
    
    public abstract void fireClient(NetworkUser client, Packet data) throws IOException;

    public abstract void fireAllClients(Packet data) throws IOException;

    public abstract void fireAllClientsExcept(NetworkUser exclude,Packet data) throws IOException;
    
}

