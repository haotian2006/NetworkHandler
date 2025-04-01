package Networking;


import java.io.*;
import java.net.*;

import Engine.Enums.ServerCloseReason;
import Networking.NetworkHandler.NetworkRemote;
import Signal.*;

import java.util.concurrent.CompletableFuture;

import javax.management.DescriptorKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The NetworkHandler class handles network communications for a client-server application.
 * It manages TCP connections and provides signals for client additions, removals, and server closures.
 * @author haotian
 */
/**
 * The NetworkHandler class handles network operations and communication between clients and servers.
 * It provides methods for managing clients, creating remotes, setting network settings, and handling network events.
 */
public class NetworkHandler {
    
    /** The timeout duration for network operations, in milliseconds. */
    protected static final int TIMEOUT = 15000; // 15 seconds
    
    /** The interval duration for network checks, in milliseconds. */
    protected static final int INTERVAL = 1000; // 1 second

    /** The timeout duration for client connection, in milliseconds. */
    protected static final int CLIENT_TIMEOUT = 8000; // 8 second

    /** The local client user. */
    protected NetworkUser localClient;
    
    /** The target IP address for connections. */
    private String TargetIp = "127.0.0.1";
    
    /** The target port for connections. */
    private int Port = 1080;

    /** Flag indicating if this instance is a server. */
    private boolean IsServer = false;
    
    /** Flag indicating if crash handling is enabled. */
    private boolean HandleCrashFlag = false;
    
    /** Flag indicating if the network handler is running. */
    private boolean Running = false;
    
    /**
     * Flag indicating if UDP is supported.
     * <p>
     * Note: UDP is not implemented for this project.
     */
    private boolean SupportsUDP = false;

    /** The current user ID for the local client. */
    protected short CurrentUserID = Short.MIN_VALUE;
    
    /** The current remote ID for the remote client. */
    protected byte CurrentRemoteID = Byte.MIN_VALUE;

    /** A map of client IDs to NetworkUser objects. */
    private ConcurrentHashMap<Short, NetworkUser> Clients = new ConcurrentHashMap<Short, NetworkUser>(){};
    
    /** A map of remote identifiers to NetworkRemote objects using string keys. */
    private HashMap<String, NetworkRemote> RemoteStringPair = new HashMap<String, NetworkRemote>();
    
    /** A map of remote identifiers to NetworkRemote objects using byte keys. */
    private HashMap<Byte, NetworkRemote> RemoteBytePair = new HashMap<Byte, NetworkRemote>();

    /** The TCP client for network communications. */
    private TCPClient tcpClient;
    
    /** The TCP server for network communications. */
    private TCPServer tcpServer;

    /** The UDP client for network communications.
     * <p>
     * Note: UDP is not implemented for this project.
     */
    private Communicator udpClient;
    
    /** The UDP server for network communications.
     * <p>
     * Note: UDP is not implemented for this project.
     */
    private Communicator udpServer;

    /** Signal for client addition events. */
    private Signal<NetworkUser> ClientAddedSignal = new Signal<NetworkUser>();
    
    /** Event triggered when a client is added. */
    public final Event<NetworkUser> ClientAdded = ClientAddedSignal.event;

    /** Signal for client removal events. */
    private Signal<NetworkUser> ClientLeftSignal = new Signal<NetworkUser>();
    
    /** Event triggered when a client is removed. */
    public final Event<NetworkUser> ClientRemoved =  ClientLeftSignal.event;

    /** Signal for server close events. */
    private Signal<ServerCloseReason> ServerCloseSignal = new Signal<ServerCloseReason>();
    
    /** Event triggered when the server closes. */
    public final Event<ServerCloseReason> OnServerClose =  ServerCloseSignal.event;
    /**
     * Creates a NetworkHandler with the given remotes.
     *
     * @param remotes the remotes to create
     */
    public NetworkHandler(String[] remotes){
        createRemote("ClientLeft");
        createRemote("ClientAdded");
        createRemote("CheckForTimeOut");

        for (String remote : remotes) {
            createRemote(remote);
            if (CurrentRemoteID >= Byte.MAX_VALUE) {
                break;
            }
        }
    }

    /**
     * Returns the local client of the NetworkHandler.
     *
     * @return the local client
     */
    public NetworkUser getLocal()
    {
        return localClient;
    }

    /**
     * Gets a remote user based on the given name.
     *
     * @param name the name of the remote user to get
     * @return the remote user with the given name, or null if it doesn't exist
     */
    public Remote getRemote(String name){
        return RemoteStringPair.get(name);
    }

    /**
     * Sets whether this network handler supports UDP or not.
     *
     * @param value whether this network handler supports UDP or not
     */
    protected void SetSupportsUDP(boolean value){
        SupportsUDP = value;
    }

    /**
     * Gets a network remote user based on the given id.
     *
     * @param id the id of the network remote user to get
     * @return the network remote user with the given id, or null if it doesn't exist
     */
    protected NetworkRemote getNetworkRemote(Byte id){
        return RemoteBytePair.get(id);
    }

    /**
     * Gets the next available id for a network user.
     *
     * @return the next available id for a network user
     */
    protected short getNextId(){
        return ++CurrentUserID;
    }
    
    /**
     * Creates a new remote user with the given name and UDP status.
     *
     * @param name the name of the remote user to create
     * @param isUDP whether the remote user supports UDP or not
     * @return the created remote user
     */
    protected Remote createRemote(String name){
        return createRemote(name,false);
    }

    /**
     * Creates a new remote user with the given name and UDP status.
     *
     * @param name the name of the remote user to create
     * @param isUDP whether the remote user supports UDP or not
     * @return the created remote user
     */
    @Deprecated
    protected Remote createRemote(String name,boolean isUDP){
        if (RemoteStringPair.containsKey(name)) {
            return RemoteStringPair.get(name);
        }
        NetworkRemote remote = new NetworkRemote(name,false,this);
        RemoteStringPair.put(name, remote);
        RemoteBytePair.put(remote.id, remote);
        return remote;
    }


     /**
     * Sets the target IP address for the network.
     *
     * @param ip the target IP address
     */
    public void setTargetIpAddress(String ip){
        TargetIp = ip;
    }

    /**
     * Gets the target IP address for the network.
     *
     * @return the target IP address
     */
    public String getTargetIpAddress(){
        return TargetIp;
    }

    /**
     * Gets the UDP IP address for the network.
     *
     * @return the UDP IP address
     */
    public String getUdpIpAddress(){
        return TargetIp;
    }

    /**
     * Sets the port for the network.
     *
     * @param port the port to set
     */
    public void setPort(int port){
        Port = port;
    }

    /**
     * Gets the port for the network.
     *
     * @return the port
     */
    public int getPort(){
        return Port;
    }

    /**
     * Gets the UDP port for the network.
     *
     * @return the UDP port
     */
    public int getUdpPort(){
        return Port;
    }

    /**
     * Adds a client to the network.
     *
     * @param client the client to add
     */
    protected void addClient(NetworkUser client){
        Clients.put(client.getId(), client);
        ClientAddedSignal.fire(client);
    }

    /**
     * Removes a client from the network.
     *
     * @param client the client to remove
     */
    public void removeClient(NetworkUser client){
        synchronized(Clients){ 
            ClientLeftSignal.fire(client);
            Clients.remove(client.getId());
            if (isServer()){
                tcpServer.removeClient(client);
            }
        }
    }

    /**
     * Removes a client from the network Used by tcpServer
     *
     * @param client the client to remove
     * @param x overload to differentiate from the other removeClient
     */
    public void removeClient(NetworkUser client,boolean x){
        synchronized(Clients){ 
            ClientLeftSignal.fire(client);
            Clients.remove(client.getId());
        }
    }

    /**
     * Gets all clients in the network.
     *
     * @return all clients in the network
     */
    public NetworkUser[] getAllClients(){ 
        return Clients.values().toArray(new NetworkUser[Clients.size()]);
    }

    protected String generateClientName(){
        int idx = Clients.size();
        while(getClient("Player " + idx) != null){
            idx++;
        }
        return "Player " + idx;
    }

    /**
     * Checks if the given name is valid.
     *
     * @param  name  the name to be checked
     * @return       true if the name is valid, false otherwise
     */
    public Boolean isValidName(String name){
        if(getClient(name) == null){
            return true;
        }
        return false;
    }



    /**
     * Retrieves the NetworkUser object associated with the given ID.
     *
     * @param  id  the ID of the client to retrieve
     * @return     the NetworkUser object with the given ID, or null if not found
     */
    public NetworkUser getClient(Short id){
        return Clients.get(id);
    }
    
    /**
     * Retrieves the NetworkUser object associated with the given Packet's client ID.
     *
     * @param  packet  the Packet object containing the client ID
     * @return         the NetworkUser object corresponding to the client ID, or null if not found
     */
    public NetworkUser getClient(Packet packet){
        return Clients.get(packet.getClientID());
    }

   /**
    * Retrieves a NetworkUser object based on the given name.
    *
    * @param  name  the name of the client to retrieve
    * @return       the NetworkUser object with the given name, or null if not found
    */
    public NetworkUser getClient(String name){
       for (Short id : Clients.keySet()) {
           if (Clients.get(id).getName().equals(name)) {
               return Clients.get(id);
           }
       }
       return null;
    }

    /**
     * Allows or denies joining the server based on the given boolean value.
     *
     * @param  allow  true if joining is allowed, false if joining is denied
     */
    public void allowJoin(boolean allow){
        if (IsServer) {
            if (allow) {
                tcpServer.AllowJoining();
            } else {
                tcpServer.DenyJoining();
            }
        }
    }

    /**
     * Retrieves an array of NetworkUser objects representing the clients connected to the server.
     *
     * @return  an array of NetworkUser objects representing the clients
     */
    public NetworkUser[] getClients(){
        return Clients.values().toArray(new NetworkUser[Clients.size()]);
    }





    /**
     * Starts the server by setting the IsServer flag to true, creating a new NetworkUser instance with the name "Host",
     * calling the getNextId() method to get the next available ID, setting the isLocal flag to true, and passing this
     * NetworkHandler instance to the NetworkUser constructor. Then, the addClient() method is called to add the new
     * NetworkUser instance to the Clients map. After that, a new TCPServer instance is created with this NetworkHandler
     * instance, and the Start() method of the TCPServer instance is called. Finally, the Running flag is set to true.
     *
     * @throws IOException if an error occurs while starting the TCPServer
     */
    public void StartServer() throws IOException {
        IsServer = true;
        localClient =  new NetworkUser("Host", getNextId(),true,this);
        addClient(localClient);
        try {
            tcpServer = new TCPServer(this);
            tcpServer.Start();
            Running = true;
        } catch (IOException e) {
            throw e;
        }

    }

    /**
     * Stops the server or client based on the current state of the IsServer flag.
     *
     * If the IsServer flag is true, calls the StopServer() method to stop the server.
     * If the IsServer flag is false, calls the StopClient() method to stop the client.
     */
    public void Stop()
    {
        if (IsServer) {
            StopServer();
        } else {
            StopClient();
        }
    }
    /**
     * Stops the server by setting the IsServer flag to false, setting the Running flag to false, and stopping the TCP server.
     */
    public void StopServer(){
        IsServer = false;
        Running = false;
        if (tcpServer != null) tcpServer.Stop();
 
    }

    /**
     * Starts the client by setting the IsServer flag to false, creating a new TCPClient instance with this NetworkHandler instance,
     * starting the TCPClient, and setting the Running flag to true. If an IOException occurs during the process, it is thrown.
     *
     * @throws IOException if an error occurs while starting the TCPClient
     */
    public void StartClient() throws IOException {
        IsServer = false;
        try {
            tcpClient = new TCPClient(this);
            tcpClient.Start();
            Running = true;
        } catch (IOException e) {
            throw e;
        }
    }
    /**
     * Stops the client by setting the Running flag to false and stopping the TCP client with the reason "userLeft".
     *
     */
    public void StopClient(){
        Running = false;
        if (tcpClient != null)
            tcpClient.Stop(ServerCloseReason.userLeft);
    }

    /**
     * Handles the closure of the server.
     *
     * @param  reason  the reason for the server closure
     */
    protected void handleServerClose(ServerCloseReason reason){
        if (HandleCrashFlag || IsServer)    return;
        HandleCrashFlag = true;
        Running = false;
        ServerCloseSignal.fire(reason);
        System.out.println("ServerCrashed "+reason);
    }

    /**
     * Returns whether the server is currently running or not.
     *
     * @return true if the server is running, false otherwise
     */
    public boolean isServer(){
        return IsServer;
    }

    protected class NetworkRemote extends Remote {
        private boolean isUDP ;
        private NetworkHandler handler;
        protected Byte id;

        public NetworkRemote(String name, NetworkHandler handler) {
            this(name, false, handler);
        }

        public NetworkRemote(String name, boolean isUDP, NetworkHandler handler) {
            super(name);
            this.handler = handler;


            this.isUDP = isUDP;
            id = ++handler.CurrentRemoteID;
        }


    /**
     * Handles the retrieval of a packet and fires the onMessageSignal event.
     *
     * @param  data  the packet to be retrieved
     */
        protected void onRetrieve(Packet data) {
            onMessageSignal.fire(data);
        }

    /**
     * Sends a packet to the server.
     *
     * @param  data  the packet to be sent
     * @return       void
     */
        public void fireServer(Packet data) {
            if (!handler.Running){
                return;
            }
            if (handler.IsServer){
                System.out.println("Cannot call fireServer clients from server");
            }
            Packet toSend = data.attachInfo(id,localClient.getId());
            if (!isUDP){
                handler.tcpClient.SendPacket(toSend);
            }else{

            }

        }

    /**
     * Sends a packet to a specific client.
     *
     * @param  client  the client to send the packet to
     * @param  data    the packet to send
     */
        public void fireClient(NetworkUser client, Packet data) {
            if (!handler.Running){
                return;
            }
            if (!handler.IsServer){
                System.out.println("Cannot call fireClient from client");
            }
            if (client.isLocal){
                System.out.println("Cannot fire local client");

            }
            Packet toSend = data.attachInfo(id,client.getId());
            if (!isUDP){
                handler.tcpServer.SendPacket(toSend);
            }else{

            }

        }

    /**
     * Sends a packet to all connected clients, except the local client.
     *
     * @param  data  the packet to send to the clients
     */
        synchronized  public void fireAllClients(Packet data) {
            if (!handler.Running){
                return;
            }
            if (!handler.IsServer){
                System.out.println("Cannot call fireAllClients from client");
            }
            for(NetworkUser client : handler.Clients.values()){
                if (client.isLocal) continue;
                Packet toSend = data.attachInfo(id,client.getId());
                if (!isUDP){

                    handler.tcpServer.SendPacket(toSend);
                }
            }
        }

        /**
         * Sends a message to all the Clients except one.
         *
         * @param exclude the client to exclude
         * @param data    the packet to send
         */
        public void fireAllClientsExcept( NetworkUser exclude,Packet data)  {
            if (!handler.Running){
                return;
            }
            if (!handler.IsServer){
                System.out.println("Cannot call fireAllClients from client");
                
            }
            for(NetworkUser client : handler.Clients.values()){
                if (client.isLocal) continue;
                if (client == exclude) continue;
                Packet toSend = data.attachInfo(id,client.getId());
                if (!isUDP){

                    handler.tcpServer.SendPacket(toSend);
                }
            }
        }
        
    }
}


/**
 * The Communicator interface represents an object that can communicate with a network.
 * It provides methods to start and stop the communication, as well as send packets.
 */
interface Communicator {
    /**
     * Starts the communication.
     */
    public void Start();

    /**
     * Stops the communication.
     */
    public void Stop();

    /**
     * Sends a packet over the network.
     * 
     * @param packet the packet to be sent
     */
    public void SendPacket(Packet packet);
}


/**
 * The TCPClient class implements the Communicator interface and represents a TCP client for network communication.
 * It establishes a connection with a server, sends and receives packets, and handles server close events.
 * @author haotian
 */
class TCPClient implements Communicator{
    
    /**
     * The handler for managing network operations.
     */
    private NetworkHandler handler;

    /**
     * The socket connection to the server.
     */
    private Socket server;

    /**
     * The input stream for receiving data from the server.
     */
    private final ObjectInputStream in;

    /**
     * The output stream for sending data to the server.
     */
    private final ObjectOutputStream out;

    /**
     * Indicates whether the connection is closed.
     */
    private boolean closed = false;

    /**
     * A list of CompletableFuture objects representing ongoing network operations.
     */
    private List<CompletableFuture> threads = new ArrayList<>();

    /**
     * The timestamp of the last ping to the server.
     */
    private Long lastPing = System.currentTimeMillis();


    /**
     * Constructs a TCPClient object with the specified NetworkHandler.
     * 
     * @param handler the NetworkHandler object to associate with the TCPClient
     * @throws IOException if an I/O error occurs while creating the TCPClient
     */
     public TCPClient(NetworkHandler handler) throws IOException {
        this.handler = handler;
        synchronized (this) {
            server = new Socket();
            SocketAddress address = new InetSocketAddress(handler.getTargetIpAddress(), handler.getPort());
            server.connect(address, NetworkHandler.CLIENT_TIMEOUT);

            out = new ObjectOutputStream(server.getOutputStream());

            in = new ObjectInputStream(server.getInputStream());

            String message = null;
            try {
                message = (String) in.readObject();
            } catch (ClassNotFoundException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (message.equals("FAIL")) {
                Stop();
                throw new IOException("Server is not allowing joining");
            }

            String uuidString = message;
            String nameString = null;
            try {
                nameString = (String) in.readObject();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }

            NetworkUser localClient = new NetworkUser(nameString, Short.parseShort(uuidString), true, handler);

            handler.addClient(localClient);
            handler.localClient = localClient;


            try {
                Object[] players = (Object[]) in.readObject();
                for (Object player : players) {
                    NetworkUser client = (NetworkUser) player;
                    if (client.getId().equals(localClient.getId())) continue;
                    handler.addClient((NetworkUser) player);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            handler.getRemote("ClientAdded").onMessage.connect(callBack -> {
                NetworkUser client = (NetworkUser) callBack.getPayLoad();
                if (handler.getClient(client.getId()) != null) {

                    return;
                }
                handler.addClient(client);

            });
        }

            
        
    }

    /**
     * Starts the network handler by initiating the listening process and pinging.
     */
    public void Start() {
        startListening();
        StartPinging();
    }

    /**
     * Starts pinging the server at regular intervals.
     * This method sends a "PING" message to the server and checks for a response.
     * If the server does not respond within the specified timeout, the connection is closed.
     * The pinging process continues until the connection is closed or an error occurs.
     */
    public void StartPinging(){
        threads.add(CompletableFuture.runAsync(() -> {
                while (!closed) {
                    synchronized (this){
                    try {
                        //System.out.println("PingServer");
                        out.writeObject("PING");
                        out.flush();
                    } catch (IOException  e) {
                        Stop(ServerCloseReason.crashed);

                        return;
                    }
                 }
                
                    if (System.currentTimeMillis() - lastPing > NetworkHandler.TIMEOUT) {
                        Stop(ServerCloseReason.timeOut);
                        return;
                    }

                    try {
                        Thread.sleep(NetworkHandler.INTERVAL);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
    }));
    }
    
/**
 * Starts listening for incoming packets on the network.
 * This method runs asynchronously in a separate thread and continuously listens for incoming packets until the network connection is closed.
 * When a packet is received, it checks if it is a valid packet or a control message.
 * If it is a valid packet, it retrieves the corresponding network remote and calls its 'onRetrieve' method.
 * If it is a control message indicating server closure or user kick, it stops the network connection accordingly.
 * 
 * @throws ClassNotFoundException if the remote corresponding to the received packet is not found
 * @throws OptionalDataException if there is an issue with the data being read
 * @throws StreamCorruptedException if there is an issue with the stream being read
 * @throws IOException if there is an issue with the input/output operations
 */
   synchronized private void startListening(){
        threads.add(CompletableFuture.runAsync(() -> {
            while (!closed) {
                try {
                   // System.out.println("receieving packet");
                    Object obj = in.readObject();
                 //   System.out.println("Received: "+ obj.toString());
                    lastPing = System.currentTimeMillis();
                    if (obj instanceof Packet) {
                        Packet packet = (Packet) obj;
                        NetworkRemote  remote = handler.getNetworkRemote(packet.RemoteID);
                      //  System.out.println("Had remote "+ remote);
                        if (remote == null) {
                            throw new ClassNotFoundException(String.format("Remote %s not found", packet.RemoteID));
                        }
                         
                        remote.onRetrieve(packet);

                    }else if (obj instanceof String){

                        String message = (String)obj;
                        if (message.equals("SERVER_CLOSE")){
                            Stop(ServerCloseReason.closed);
                            return;
                        }else if(message.equals("KICKED")){
                            Stop(ServerCloseReason.userKicked);
                            return;
                        }
                    }
                } catch (OptionalDataException ode) {
                    System.out.println(ode.eof + " " + ode.getLocalizedMessage()+ " " + ode.length);
                    ode.printStackTrace();
                    //Stop(ServerCloseReason.crashed);
                } catch (StreamCorruptedException e) {
                    e.printStackTrace();
                //    Stop(ServerCloseReason.crashed);
                } catch (IOException e) {
                   // e.printStackTrace();
                    Stop(ServerCloseReason.crashed);
                    return;
                } catch (ClassNotFoundException e) {

                    e.printStackTrace();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }));
    }
    /**
     * Sends a packet over the network.
     *
     * @param packet the packet to be sent
     */
    @Override
    synchronized public void SendPacket(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            Stop(ServerCloseReason.crashed);
            e.printStackTrace();
          
        }
    }
    /**
     * Stops the network handler.
     * This method stops the network handler and closes the server with an unknown reason.
     */
    @Override
    public void Stop(){
        Stop(ServerCloseReason.unknown);
    }
    /**
     * Stops the network handler and performs necessary cleanup operations.
     * 
     * @param level the reason for closing the server
     */
    synchronized public void Stop(ServerCloseReason level) {
        
        closed = true;

        try{
            out.writeObject("LEAVING");
            out.flush();
        }catch (IOException e){

        }
        
        try{
            for (CompletableFuture thread : threads) {
                thread.cancel(true);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

        try {
            server.close();
            in.close();
            out.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
        handler.handleServerClose(level);
    }
}

/**
 * The TCPServer class implements the Communicator interface and represents a TCP server.
 * It handles incoming client connections, pinging clients, and sending/receiving packets.
 * @author haotian
 */
class TCPServer implements Communicator{
   
    /**
     * The handler for managing network operations.
     */
    private NetworkHandler handler;

    /**
     * The server socket for accepting client connections.
     */
    private ServerSocket server;

    /**
     * A list of CompletableFuture objects representing ongoing network operations.
     */
    private List<CompletableFuture> threads;

    /**
     * Indicates whether joining is allowed.
     */
    private boolean allowJoining;

    /**
     * Indicates whether the connection is closed.
     */
    private boolean closed = false;

    /**
     * Constructs a NetworkManager.
     */
    public TCPServer() {
        this.threads = new ArrayList<>();
    }

    /**
     * Sets the network handler for managing network operations.
     * 
     * @param handler the network handler
     */
    public void setHandler(NetworkHandler handler) {
        this.handler = handler;
    }

    /**
     * Starts the server with the specified server socket.
     * 
     * @param server the server socket
     */
    public void startServer(ServerSocket server) {
        this.server = server;
    }

    /**
     * Closes the network connection and sets the closed flag to true.
     */
    public void close() {
        closed = true;
        // Add logic to close the server socket and clean up resources
    }

    /**
     * Allows or disallows new clients to join.
     * 
     * @param allow whether to allow joining
     */
    public void setAllowJoining(boolean allow) {
        this.allowJoining = allow;
    }

    /**
     * Adds a CompletableFuture to the list of ongoing network operations.
     * 
     * @param future the CompletableFuture to add
     */
    public void addThread(CompletableFuture future) {
        threads.add(future);
    }

    /**
     * Returns whether the connection is closed.
     * 
     * @return true if the connection is closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns whether joining is allowed.
     * 
     * @return true if joining is allowed, false otherwise
     */
    public boolean isAllowJoining() {
        return allowJoining;
    }

    /**
     * Returns the list of ongoing network operations.
     * 
     * @return the list of CompletableFuture objects
     */
    public List<CompletableFuture> getThreads() {
        return threads;
    }



    /**
     * Constructs a TCPServer object with the specified NetworkHandler.
     * 
     * @param handler the NetworkHandler object to be associated with the server
     * @throws IOException if an I/O error occurs when opening the server socket
     * @throws IllegalArgumentException if the port parameter is outside the specified range of valid port values
     */
    public TCPServer(NetworkHandler handler) throws IOException, IllegalArgumentException{
        server = new ServerSocket(handler.getPort());
        this.handler = handler;
        allowJoining = true;
        threads = new ArrayList<>();

    }   

    /**
     * Starts the network handler by listening for clients and initiating pinging.
     */
    public void Start(){
        listenForClient();
        StartPinging();
    }

    /**
     * Starts pinging the connected clients to check their availability.
     * If a client does not respond within the specified timeout period,
     * it will be removed from the list of clients.
     */
    private void StartPinging(){
        threads.add(CompletableFuture.runAsync(() -> {
            while (!closed) {
               // System.out.println("pingAll");

                synchronized (this){
                    for (NetworkUser client : handler.getClients()) {
                        if (client.isLocal) continue;
                        if (System.currentTimeMillis() - client.lastPing > NetworkHandler.TIMEOUT) {

                            removeClient(client);
                        }
    
                        try {
                            client.getOut().writeObject("PING");
                            client.getOut().flush();
                        } catch (IOException e) {
                           // e.printStackTrace();
                           System.out.println( "Failed to ping client |" + client.toString() );
                           removeClient(client);
                           continue;
                        }
                    }
                }
              
                try {
                    Thread.sleep(NetworkHandler.INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));
    }
    /**
     * Listens for incoming client connections and handles them accordingly.
     * This method runs asynchronously in a separate thread.
     */
    private void listenForClient(){
        threads.add(CompletableFuture.runAsync(() -> {
            while (!closed) {
                try {
                    Socket client = server.accept();
                   synchronized(this){
                        NetworkUser clientObj = new NetworkUser(client,handler);
                        ObjectOutputStream out = clientObj.getOut();
                        if(!allowJoining){
                            out.writeObject("FAIL");
                            out.flush();
                            client.close();
                            continue; 
                        }
                        String Name = handler.generateClientName();
                        clientObj.setName(Name);

                        out.writeObject(clientObj.getId().toString());
                        out.writeObject(Name);
                        out.writeObject(handler.getClients());
                        out.flush();
                        handler.addClient(clientObj);
                        Remote remote = handler.getRemote("ClientAdded");
                        Packet data = new Packet();
                        data.addToPayLoad(clientObj);
                        remote.fireAllClients(data);
                        startListening(clientObj);
                   }
                     
                 
                } catch (IOException e) {
                  //  e.printStackTrace();
                    continue;
                }
            }
        }));
    }

/**
 * Removes a client from the network.
 * If the client is local, the method returns without performing any action.
 * Otherwise, it sends a "KICKED" message to the client and closes the connection.
 * 
 * @param client The client to be removed from the network.
 */
   synchronized public void removeClient(NetworkUser client){
        if (client.isLocal) return;
        try{
            client.getOut().writeObject("KICKED");
            client.getOut().flush();
        }catch (IOException e){

        }
        try {
            client.getConnection().close();
            client.getIn().close();
            client.getOut().close();
        } catch (IOException e) {
        }
        
        handler.removeClient(client,true);
    }

    /**
     * Starts listening for incoming network messages from the specified client.
     * 
     * @param client The NetworkUser object representing the client.
     */
    synchronized  public void startListening(NetworkUser client){
        ObjectInputStream  in = client.getIn();
        threads.add(CompletableFuture.runAsync(() -> {
            while (!closed) {
                try {
                    Object obj = in.readObject();
                    client.lastPing = System.currentTimeMillis();
                    if (obj instanceof Packet) {
                        Packet packet = (Packet) obj;
                        NetworkRemote  remote = handler.getNetworkRemote(packet.RemoteID);
                        if (remote == null) {
                            System.out.println(String.format("Remote %s not found", packet.RemoteID));
                            continue;
                        }
                        packet.clientID = client.getId();
                        remote.onRetrieve(packet);
                    }else if (obj instanceof String){
                        String message = (String)obj;
                        if (message.equals("LEAVING")){
                            System.out.println("leaving "+ client.toString());
                            removeClient(client);
                            return;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (ClassNotFoundException e) {

                    e.printStackTrace();
                }
            }
        }));
    }

    /**
     * Sends a packet to the specified client.
     * 
     * @param packet the packet to be sent
     */
    @Override
    synchronized public void SendPacket(Packet packet) {
        NetworkUser client = packet.getClientID() == null ? null : handler.getClient(packet.getClientID());
        //System.out.println(client);
        if (client == null){
            return;
        }
        try {
            //System.out.println("Sent Packet" + packet.toString());
            client.getOut().writeObject(packet);
            client.getOut().flush();
        } catch (IOException e) {
            if (e instanceof SocketException && e.getMessage().equals("Socket is closed")) {
            return;
            }
          System.out.println("Failed to send packet");
            e.printStackTrace();
        }
        
    }
    /**
     * Stops the network handler and closes all client connections.
     * This method sets the 'closed' flag to true, sends a "SERVER_CLOSE" message to all clients,
     * and closes the connections, input streams, and output streams of each client.
     * Finally, it closes the server socket and cancels all running threads.
     */
    @Override
    synchronized public void Stop() {
        closed = true;
        for (NetworkUser client : handler.getClients()) {
            if (client.isLocal) continue;
            try{
                client.getOut().writeObject("SERVER_CLOSE");
                client.getOut().flush();
            }catch (IOException e){
                //e.printStackTrace();
            }
            try {
                client.getConnection().close();
                client.getIn().close();
                client.getOut().close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            
            handler.removeClient(client);
        }
        try {
            server.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
          
        try{
            for (CompletableFuture thread : threads) {
                thread.cancel(true);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        handler.handleServerClose(ServerCloseReason.closed);
        
    }

    /**
     * Allows joining of new connections.
     */
    public void AllowJoining(){
        allowJoining = true;
    }

    /**
     * Sets the allowJoining flag to false, denying any further joining requests.
     */
    public void DenyJoining(){
        allowJoining = false;
    }

}





/**
 * The UDPServer class implements the Communicator interface and represents a UDP server for network communication.
 * It listens for incoming packets, handles them, and sends packets to clients. This was just a prototype.
 * 
 * 
 * @deprecated
 * @author haotian
 
 */
class UDPServer implements Communicator{

    private DatagramSocket socket;
    private NetworkHandler handler;
    private List<CompletableFuture> threads;
    private boolean allowJoining = true;
    private boolean closed = false;



    public UDPServer(NetworkHandler handler) throws IOException{
        this.handler = handler;
        socket = new DatagramSocket(handler.getPort());
        threads = new ArrayList<CompletableFuture>();

    }

    public void Start(){
        threads.add(CompletableFuture.runAsync(this::listenForClients));
    }


    private void listenForClients(){
        byte[] buffer = new byte[2^16];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        while(!closed){
            try {
                socket.receive(packet);
                handlePacket(packet);
            } catch (IOException e) {
                //e.printStackTrace();
                continue;
            }
        }
    }

    synchronized public void SendPacket(Packet packet) {
        NetworkUser client = packet.getClientID() == null ? null : handler.getClient(packet.getClientID());
        if (client == null){
            return;
        }
        InetAddress address = client.getConnection().getInetAddress();
        try {
            ByteArrayOutputStream byteStream  = new ByteArrayOutputStream();
            ObjectOutputStream outputSteam = new ObjectOutputStream(byteStream);
            outputSteam.writeObject(packet);
            outputSteam.flush();
            byte[] data = byteStream.toByteArray();
            outputSteam.close();
            DatagramPacket packetToSend = new DatagramPacket(data, data.length, address, handler.getUdpPort());
            socket.send(packetToSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePacket(DatagramPacket packet){
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
            Packet packetReceived = (Packet) inputStream.readObject();
            NetworkRemote  remote = handler.getNetworkRemote(packetReceived.RemoteID);
            remote.onRetrieve(packetReceived);
            inputStream.close();
        } catch (ClassNotFoundException | IOException e) {
            //e.printStackTrace();
        }
    }


    public void Stop(){
        closed = true;
        for(CompletableFuture thread : threads){
            thread.cancel(true);
        }
        socket.close();
    }
}

