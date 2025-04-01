package Networking;


import java.io.IOError;
import java.io.IOException;
import java.util.function.Function;

import Signal.*;
// THIS CAN BE OPTIMIZED FURTHER IF WE SEND A UNIQUE REMOTE ID THATS AN UINT8 FOR EACH REMOTE (DONE!)

/**
 * Used to for communications
 * @author haotian
 */
public abstract class Remote {
    /**
     * Name of the remote
     */
    public final String name;

    /**
     * a event that is fired when a message is retrieved
     */
    protected Signal<Packet> onMessageSignal;
    public final Event<Packet> onMessage;



    /**
     * @param name name of the remote
     */
    protected Remote(String name) {
        this.name = name;
        onMessageSignal = new Signal<Packet>();
        onMessage = onMessageSignal.event;
    }


    /**
     * Sends a message to the Server
     * @param data the packet to send
     */
    public abstract void fireServer(Packet data);

    /**
     * Sends a message to the given Client
     * @param client the client to send to
     * @param data the packet to send
     */
    public abstract void fireClient(NetworkUser client, Packet data);

    /**
     * Sends a message to all the Clients
     * @param data the packet to send
     */
    public abstract void fireAllClients(Packet data);

    /**
     * Sends a message to all the Clients except one
     * @param exclude the client to exclude
     * @param data the packet to send
     */
    public abstract void fireAllClientsExcept(NetworkUser exclude,Packet data);

    /**
     * Returns the name of the remote in a string format
     * @return the name of the remote
     */
    public String toString() {
        return name;
    }
    
}

