import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import Networking.*;
 
public class Test {
    public static void Server(){
        try {
            NetworkHandler Server = new NetworkHandler(new String[]{"test","another"});
           
      
            Server.setTargetIpAddress("localhost");
            Server.setPort(1111);
            Remote serverRemote = Server.getRemote("test");
            Server.StartServer();

            serverRemote.onMessage.connect(Data -> {
                System.out.println("[SERVER]"+Data.getPayLoad()+"|"+Data.getClientID());
            });

            Remote anotherRemote = Server.getRemote("another");

            anotherRemote.onMessage.connect(Data -> {
                System.out.println("[SERVER] Another Remote");
            });


            
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Packet serverData = new Packet();
        serverData.addToPayLoad("Hello From Server");
        serverRemote.fireAllClients(serverData);
        anotherRemote.fireAllClients(serverData);
        System.out.println("[SERVER] Stop");
        Server.StopServer();
    
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void client(){
        try {
            NetworkHandler Client = new NetworkHandler(new String[]{"test","another"});
            Client.setTargetIpAddress("localhost");
            Client.setPort(1111);
            Remote clientRemote = Client.getRemote("test");
            Client.StartClient();
            
            Client.OnServerClose.connect(reason -> {
                System.out.println("Server Closed ["+ reason.toString()+']');
            });

            clientRemote.onMessage.connect(Data -> {
                System.out.println("[CLIENT]"+Data.getPayLoad());
            });

            Remote anotherRemote = Client.getRemote("another");

            anotherRemote.onMessage.connect(Data -> {
                System.out.println("[Client] Another Remote");
            });


                Packet serverData = new Packet();
                serverData.addToPayLoad("Hello From Client");
                clientRemote.fireServer(serverData);
                anotherRemote.fireServer(serverData);

                      
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //Client.StopClient();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                Server();
            }
        });
        Thread client = new Thread(new Runnable() {
            @Override
            public void run() {
                client();
            }
        });

        server.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        client.start();


        Thread client2 = new Thread(new Runnable() {
            @Override
            public void run() {
                client();
            }
        });


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //client2.start();
        try {
            Thread.sleep(15000000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
