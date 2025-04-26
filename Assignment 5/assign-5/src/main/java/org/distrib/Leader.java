package org.distrib;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * This class is the leader that is responsible for distributing the computation to the nodes
 * and communicating with the client on the results
 */
public class Leader {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Leader usage: clientPort nodePort");
            System.exit(1);
        }
        int clientPort = Integer.parseInt(args[0]);
        int nodePort = Integer.parseInt(args[1]);

        ServerSocket nodeServer = new ServerSocket(nodePort);
        ServerSocket clientServ = new ServerSocket(clientPort);
        //accept the client connection
        Socket clientSock = clientServ.accept();
        DataInputStream in = new DataInputStream(clientSock.getInputStream());
        DataOutputStream out = new DataOutputStream(clientSock.getOutputStream());

        nodeServer.setSoTimeout(1000);
        List<Socket> nodeSockets = new ArrayList<>();
        while (nodeSockets.size() < 3) {
            try {
                Socket ns = nodeServer.accept();
                nodeSockets.add(ns);
                System.out.println("[DEBUG] -> Node sockets accepted: " + nodeSockets.size());
            } catch (SocketTimeoutException e) {
                break;
            }
        }

        if (nodeSockets.size() < 3) {
            JSONObject errPayload = new JSONObject();
            errPayload.put("error", "Need at least 3 nodes, but only " + nodeSockets.size() + " connected");
            out.writeUTF(JsonHelper.createMessage("ERROR", errPayload).toString());
            out.flush();
            clientSock.close();
            clientServ.close();
            nodeServer.close();
            return;
        }
        System.out.println("Continueing...");

        String request = in.readUTF();
        System.out.println("[Leader <- Client] " + request);
        JSONObject req = new JSONObject(request);
        JSONObject payload = JsonHelper.getPayload(req);
        List<Integer> numbers = JsonHelper.convertFromArray(payload.getJSONArray("slice"));
        long delay = payload.getLong("delay");
        
        // Single thread computation time to compare with distributed version
        long t0 = System.nanoTime();
        long singleSum = calcSum(numbers, delay);
        long singleThreadResTime  = (System.nanoTime() - t0) / 1_000_000;

        List<List<Integer>> slices = divideIntoSlices(numbers, nodeSockets.size());

        long tDistStart = System.nanoTime();
        long[] partialSums = DistribSum.execDistribution(nodeSockets, slices, delay);
        long distribThreadResTime = (System.nanoTime() - tDistStart) / 1_000_000;
        long distSum = 0;
        for (long s : partialSums) distSum += s;
        boolean isConsensusMade = DistribConsensus.checkConsensus(nodeSockets, slices, partialSums);

        JSONObject respPayload = new JSONObject();
        if (isConsensusMade) {
            long improvement = singleThreadResTime - distribThreadResTime;
            double perc = singleThreadResTime > 0
                ? (improvement * 100.0) / singleThreadResTime
                : 0.0;
            System.out.printf("[LEADER ANALYSIS] -> Distributed was %d ms faster (%.2f%% improvement)%n", improvement, perc);
            respPayload
               .put("distSum", distSum)
               .put("singleSum", singleSum)
               .put("singleThreadResTime", singleThreadResTime)
               .put("distribThreadResTime", distribThreadResTime);
            out.writeUTF(JsonHelper.createMessage("RESULT", respPayload).toString());
        } else {
            respPayload.put("error", "Nodes did not come to a consensus");
            out.writeUTF(JsonHelper.createMessage("ERROR", respPayload).toString());
        }
        out.flush();
        clientSock.close();
        clientServ.close();
        nodeServer.close();
    }

    // calculate the sum of the list w/ a delay added
    private static long calcSum(List<Integer> list, long delay) {
        long res = 0;
        for (int i : list) {
            res += i;
            if (delay > 0) {
                try { Thread.sleep(delay); } catch ( Exception e) {}
            }
        }
        return res;
    }

    private static List<List<Integer>> divideIntoSlices(List<Integer> list, int parts) {
        List<List<Integer>> slices = new ArrayList<>();
        int n = list.size(), base = n / parts, rem = n % parts, idx = 0;
        for (int i = 0; i < parts; i++) {
            int size = base + (i < rem ? 1 : 0);
            slices.add(new ArrayList<>(list.subList(idx, idx + size)));
            idx += size;
        }
        return slices;
    }
}
