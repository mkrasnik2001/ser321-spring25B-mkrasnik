package taskone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.JSONObject;

import netscape.javascript.JSException;
import taskone.JsonUtils;
import taskone.NetworkUtils;

public class ThreadedServer {

    static Performer performer;

    public static void main(String[] args) throws Exception {
        int port = 8000;
        String host = "localhost";
        StringList strings = new StringList(); 
        performer = new Performer(strings);  

        if (args.length >= 1) {
            host = args[0];
        }
        
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                System.out.println("[Port] must be an integer");
                System.exit(2);
            }
        }

        ServerSocket server = new ServerSocket(port);
        System.out.println("Server Started...");
        while (true) {
            System.out.println("Accepting a Request...");
            Socket conn = server.accept();
            Thread clientWorker = new Thread(new ClientWorker(conn));
            clientWorker.start();
        }
}

    static class ClientWorker implements Runnable {
        private Socket conn;

        public ClientWorker(Socket conn) {
            this.conn = conn;
        }

        @Override
        public void run() {
            boolean quit = false;
            try {
                OutputStream out = conn.getOutputStream();
                InputStream in = conn.getInputStream();
                System.out.println("Server connected to client:");
                int choice = 9999;

                while (!quit) {
                    byte[] messageBytes = NetworkUtils.receive(in);
                    JSONObject message = JsonUtils.fromByteArray(messageBytes);
                    JSONObject returnMessage = new JSONObject();

                    try {
                        choice = message.getInt("selected");
                    } catch (JSException e) {
                        returnMessage = performer.error("Must be an int try again!");
                    }

                    synchronized (performer) {
                        switch (choice) {
                            case (1):
                                String inStr = message.getString("data");
                                returnMessage = performer.add(inStr);
                                break;
                            case (3):
                                returnMessage = performer.display();
                                break;
                            case (4):
                                returnMessage = performer.count();
                                break;
                            case (0):
                                returnMessage = performer.quit();
                                quit = true;
                                break;
                            default:
                                returnMessage = performer.error("Invalid selection: " + choice + " is not an option");
                                break;
                        }
                    }

                    byte[] output = JsonUtils.toByteArray(returnMessage);
                    NetworkUtils.send(out, output);
                }
                System.out.println("close the resources of client ");
                out.close();
                in.close();
                conn.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
