package org.distrib;

import org.json.JSONObject;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/*
 * This class is responsible for the client side providing the list to sum up 
 * that will be sent to the leader
 */
public class Client {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Client usage: leaderHost clientPort");
            System.exit(1);
        }
        String leaderHost = args[0];
        int clientPort = Integer.parseInt(args[1]);
        Scanner scanner = new Scanner(System.in);
        List<Integer> ls= new ArrayList<>();
        while (true) {
            System.out.print("Enter the list you wish to sum up seperate by commas: ");
            String line = scanner.nextLine().trim();
            try {
                ls.clear();
                for (String s : line.split(",")) {
                    ls.add(Integer.parseInt(s.trim()));
                }
                break;
            } catch (NumberFormatException e) {
                System.out.print("Invalid input try again:\n");
            }
        }
        System.out.print("Enter delay the delay in milliseconds: ");
        long delay;
        while (true) {
            try {
                delay = Long.parseLong(scanner.nextLine().trim());
                break;
            } catch (Exception e) {
                System.out.print("Enter delay in milliseconds and his has to be numbers! Try again:\n");
            }
        }

        Socket sock = new Socket(leaderHost, clientPort);
        DataOutputStream os = new DataOutputStream(sock.getOutputStream());
        DataInputStream in = new DataInputStream(sock.getInputStream());

        JSONObject payload = new JSONObject()
            .put("slice", JsonHelper.convertToJsonArray(ls))
            .put("delay", delay);
        JSONObject req = JsonHelper.createMessage("TASK", payload);
        String reqJson = req.toString();
        System.out.println("[Client -> Leader] " + reqJson);
        os.writeUTF(reqJson);
        os.flush();
        os.writeUTF(req.toString());
        os.flush();

        JSONObject resp = new JSONObject(in.readUTF());
        System.out.println("[Client <- Leader] " + resp);
        String responseHeader = JsonHelper.getHeader(resp);
        JSONObject responsePayload = JsonHelper.getPayload(resp);
        
        if ("RESULT".equals(responseHeader)) {
            long sum = responsePayload.getLong("singleSum");
            long singleThreadResTime = responsePayload.getLong("singleThreadResTime");
            long distribThreadResTime = responsePayload.getLong("distribThreadResTime");
            System.out.printf(
                "The final sum of the list is: %d%nSingle-threaded time: %d ms%nDistributed time: %d ms%n",
                sum, singleThreadResTime, distribThreadResTime
            );
        } else {
            System.err.println("Error: " + responsePayload.getString("error"));
        }
        sock.close();
    }
}
