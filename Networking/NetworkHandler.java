package Networking;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.UUID;

import Networking.NetworkHandler.NetworkRemote;
import Networking.Signal.*;

import java.util.concurrent.CompletableFuture;

import javax.management.DescriptorKey;



import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import java.util.*;


public class NetworkHandler {
    protected static final int TIMEOUT = 15000; //15 seconds
    protected static final int INTERVAL = 1000; //1 second

    protected NetworkUser localClient;
    private String TargetIp = "127.0.0.1";
    private int Port = 1080;

    private boolean IsServer = false;
    private boolean HandleCrashFlag = false;
    private boolean Running = false;
    
    
    //TODO: document
    private boolean SupportsUDP = false;

    protected short CurrentUserID = Short.MIN_VALUE;
    protected byte CurrentRemoteID = Byte.MIN_VALUE;

    private HashMap<Short, NetworkUser> Clients = new HashMap<Short, NetworkUser>(){};
    private HashMap<String, NetworkRemote> RemoteStringPair = new HashMap<String, NetworkRemote>();
    private HashMap<Byte, NetworkRemote> RemoteBytePair = new HashMap<Byte, NetworkRemote>();

    private TCPClient tcpClient;
    private TCPServer tcpServer;

    // I Decided that I will not implement UDP for this project 
    private Communicator udpClient;
    private Communicator udpServer;


    private Signal<NetworkUser> ClientAddedSignal = new Signal<NetworkUser>();
    public final Event<NetworkUser> ClientAdded = ClientAddedSignal.event;

    
    private Signal<NetworkUser> ClientLeftSignal = new Signal<NetworkUser>();
    public final Event<NetworkUser> ClientRemoved =  ClientLeftSignal.event;

    private Signal<ServerCloseReason> ServerCloseSignal = new Signal<ServerCloseReason>();
    public final Event<ServerCloseReason> OnServerClose =  ServerCloseSignal.event;


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

    public NetworkUser getLocal()
    {
        return localClient;
    }

    public Remote getRemote(String name){
        return RemoteStringPair.get(name);
    }

    protected void SetSupportsUDP(boolean value){
        SupportsUDP = value;
    }

    protected NetworkRemote getNetworkRemote(Byte id){
        return RemoteBytePair.get(id);
    }

    protected short getNextId(){
        return ++CurrentUserID;
    }
    
    protected Remote createRemote(String name){
        return createRemote(name,false);
    }

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


    public void setTargetIpAddress(String ip){
        TargetIp = ip;
    }

    public String getTargetIpAddress(){
        return TargetIp;
    }

    public String getUdpIpAddress(){
        return TargetIp;
    }
    public void setPort(int port){
        Port = port;
    }

    public int getPort(){
        return Port;
    }

    public int getUdpPort(){
        return Port;
    }

    protected void addClient(NetworkUser client){
        Clients.put(client.getId(), client);
        ClientAddedSignal.fire(client);
    }

    public void removeClient(NetworkUser client){
        Clients.remove(client.getId());
        ClientLeftSignal.fire(client);
    }

    protected String generateClientName(){
        int idx = Clients.size();
        while(getClient("Player " + idx) != null){
            idx++;
        }
        return "Player " + idx;
    }

    public Boolean isValidName(String name){
        if(getClient(name) == null){
            return true;
        }
        return false;
    }



    public NetworkUser getClient(Short id){
        return Clients.get(id);
    }
    
    public NetworkUser getClient(Packet packet){
        return Clients.get(packet.getClientID());
    }

    public NetworkUser getClient(String name){
       for (Short id : Clients.keySet()) {
           if (Clients.get(id).getName().equals(name)) {
               return Clients.get(id);
           }
       }
       return null;
    }

    public NetworkUser[] getClients(){
        return Clients.values().toArray(new NetworkUser[Clients.size()]);
    }





    public void StartServer() throws IOException {
        localClient =  new NetworkUser("Host", getNextId(),true,this);
        addClient(localClient);
        IsServer = true;
        try {
            tcpServer = new TCPServer(this);
            tcpServer.Start();
            Running = true;
        } catch (IOException e) {
            throw e;
        }

    }

    public void StopServer(){
        IsServer = false;
        Running = false;
        tcpServer.Stop();
    }

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
    public void StopClient(){
        Running = false;
        tcpClient.Stop(ServerCloseReason.userLeft);
    }

    protected void handleServerClose(ServerCloseReason reason){
        if (HandleCrashFlag || IsServer)    return;
        HandleCrashFlag = true;
        Running = false;
        ServerCloseSignal.fire(reason);
        //System.out.println("ServerCrashed "+reason);
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


        protected void onRetrieve(Packet data) {
            new Thread(){
                public void run() {
                    onMessageSignal.fire(data);
                }
            }.start(); 
        }

        public void fireServer(Packet data) {
            if (!handler.Running){
                return;
            }
            if (handler.IsServer){
                System.out.println("Cannot call fireServer clients from server");
            }
            Packet toSend = data.attachInfo(id);
            if (!isUDP){
                handler.tcpClient.SendPacket(toSend);
            }else{

            }

        }

        public void fireClient(NetworkUser client, Packet data) {
            if (!handler.Running){
                return;
            }
            if (!handler.IsServer){
                System.out.println("Cannot call fireClient from client");
            }
            if (!client.isLocal()){
                System.out.println("Cannot fire local client");
            }
            Packet toSend = data.attachInfo(id);
            if (!isUDP){
                handler.tcpServer.SendPacket(toSend);
            }else{

            }

        }

        public void fireAllClients(Packet data) {
            if (!handler.Running){
                return;
            }
            if (!handler.IsServer){
                System.out.println("Cannot call fireAllClients from client");
            }
            for(NetworkUser client : handler.Clients.values()){
                if (client.isLocal()) continue;
                Packet toSend = data.attachInfo(id,client.getId());
                if (!isUDP){

                    handler.tcpServer.SendPacket(toSend);
                }
            }
        }

        public void fireAllClientsExcept( NetworkUser exclude,Packet data)  {
            if (!handler.Running){
                return;
            }
            if (!handler.IsServer){
                System.out.println("Cannot call fireAllClients from client");
                
            }
            for(NetworkUser client : handler.Clients.values()){
                if (client.isLocal()) continue;
                if (client == exclude) continue;
                Packet toSend = data.attachInfo(id,client.getId());
                if (!isUDP){

                    handler.tcpServer.SendPacket(toSend);
                }
            }
        }
        
    }
}


interface Communicator {
    public void Start();
    public void Stop();
    public void SendPacket(Packet packet);
}


class TCPClient implements Communicator{
    private NetworkHandler handler;
    private Socket server;
    private ObjectInputStream  in;
    private ObjectOutputStream  out;
    private boolean closed = false;

    private List<CompletableFuture> threads = new ArrayList<>();
    
    private Long lastPing = System.currentTimeMillis();

    public TCPClient(NetworkHandler handler) throws IOException {
        
        this.handler = handler;
        server = new Socket(handler.getTargetIpAddress(), handler.getPort());

        out = new ObjectOutputStream(server.getOutputStream());
        
        in = new ObjectInputStream(server.getInputStream());
        
        

        String message = in.readUTF();
        if (message.equals("FAIL")){
            Stop();
            throw new IOException("Server is not allowing joining");
        }
        
        String uuidString = message;
        String nameString = in.readUTF();
 
        NetworkUser localClient = new NetworkUser(nameString, Short.parseShort(uuidString),true,handler);

        handler.addClient(localClient);
        handler.localClient = localClient;

    
        try {
            Object[] players = (Object[]) in.readObject();
            for (Object player : players){
                NetworkUser client = (NetworkUser) player;
                if (client.getId().equals(localClient.getId())) continue;
                handler.addClient((NetworkUser) player);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        handler.getRemote("ClientAdded").onMessage.connect(callBack -> {
            NetworkUser client = (NetworkUser) callBack.getPayLoad().get(0);
            if (handler.getClient(client.getId()) != null) {

                return;
            }
            handler.addClient(client);
   
        });

            
        
    }

    public void Start() {
        startListening();
        StartPinging();
    }

    public void StartPinging(){
        threads.add(CompletableFuture.runAsync(() -> {
            while (!closed) {
                try {
                    Thread.sleep(NetworkHandler.INTERVAL);
                    out.writeObject("PING");
                    out.flush();
                } catch (IOException | InterruptedException e) {
                    Stop(ServerCloseReason.crashed);
                    return;
                }
                if (System.currentTimeMillis() - lastPing > NetworkHandler.TIMEOUT) {
                    System.out.println("ping Failed");
                    Stop(ServerCloseReason.timeOut);
                    return;
                }
            }
        }));
    }
    
    private void startListening(){
        threads.add(CompletableFuture.runAsync(() -> {
            while (!closed) {
                try {
                    Object obj = in.readObject();
                    lastPing = System.currentTimeMillis();
                    if (obj instanceof Packet) {
                        Packet packet = (Packet) obj;
                        NetworkRemote  remote = handler.getNetworkRemote(packet.RemoteID);
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
                } catch (IOException e) {
                    Stop(ServerCloseReason.crashed);
                    return;
                } catch (ClassNotFoundException e) {

                    e.printStackTrace();
                }
            }
        }));
    }
    @Override
    public void SendPacket(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            Stop(ServerCloseReason.crashed);
          
        }
    }
    @Override
    public void Stop(){
        Stop(ServerCloseReason.unknown);
    }
    public void Stop(ServerCloseReason level) {
        
        closed = true;

        try{
            out.writeObject("LEAVING");
            out.flush();
        }catch (IOException e){

        }
        

        for (CompletableFuture thread : threads) {
            thread.cancel(true);
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

class TCPServer implements Communicator{
    private NetworkHandler handler;
    private ServerSocket server;
    private List<CompletableFuture> threads;
    private boolean allowJoining;
    private boolean closed = false;



    public TCPServer(NetworkHandler handler) throws IOException, IllegalArgumentException{
        server = new ServerSocket(handler.getPort());
        this.handler = handler;
        allowJoining = true;
        threads = new ArrayList<>();

    }   

    public void Start(){
        listenForClient();
        StartPinging();
    }

    private void StartPinging(){
        threads.add(CompletableFuture.runAsync(() -> {
            while (!closed) {
                for (NetworkUser client : handler.getClients()) {
                    if (client.isLocal()) continue;
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
              
                try {
                    Thread.sleep(NetworkHandler.INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));
    }
    private void listenForClient(){
        threads.add(CompletableFuture.runAsync(() -> {
            while (!closed) {
                try {
                    Socket client = server.accept();
                    NetworkUser clientObj = new NetworkUser(client,handler);
                    ObjectOutputStream out = clientObj.getOut();
                    if(!allowJoining){
                        out.writeUTF("FAIL");
                        out.flush();
                        client.close();
                        continue; 
                    }
                    String Name = handler.generateClientName();
                    clientObj.setName(Name);

                    out.writeUTF(clientObj.getId().toString());
                    out.writeUTF(Name);
                    out.writeObject(handler.getClients());
                    out.flush();
                    handler.addClient(clientObj);
                    Remote remote = handler.getRemote("ClientAdded");
                    Packet data = new Packet();
                    data.addToPayLoad(clientObj);
                    remote.fireAllClients(data);

                    startListening(clientObj);
                     
                 
                } catch (IOException e) {
                    
                    continue;
                }
            }
        }));
    }

    public void removeClient(NetworkUser client){
        if (client.isLocal()) return;
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
        
        handler.removeClient(client);
    }

    public void startListening(NetworkUser client){
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

                    return;
                } catch (ClassNotFoundException e) {

                    e.printStackTrace();
                }
            }
        }));
    }

    @Override
    public void SendPacket(Packet packet) {
        NetworkUser client = packet.getClientID() == null ? null : handler.getClient(packet.getClientID());
        if (client == null){
            return;
        }
        try {
            client.getOut().writeObject(packet);
            client.getOut().flush();
        } catch (IOException e) {
          System.out.println("Failed to send packet");
            //e.printStackTrace();
        }
        
    }
    @Override
    public void Stop() {
        closed = true;
        for (NetworkUser client : handler.getClients()) {
            if (client.isLocal()) continue;
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
        for (CompletableFuture thread : threads) {
            thread.cancel(true);
        }
        try {
            server.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        
    }

    public void AllowJoining(){
        allowJoining = true;
    }

    public void DenyJoining(){
        allowJoining = false;
    }

}





//This was a test
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

    public void SendPacket(Packet packet) {
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
