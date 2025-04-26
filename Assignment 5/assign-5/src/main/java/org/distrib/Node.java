package org.distrib;

import org.json.JSONObject;
import org.json.JSONArray;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

public class Node {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Node class usage is: leaderHost nodePort");
            System.exit(1);
        }
        String leaderHost = args[0];
        int nodePort = Integer.parseInt(args[1]);
        boolean genFault = "1".equals(System.getProperty("Fault"));

        Socket sock = new Socket(leaderHost, nodePort);
        DataOutputStream os = new DataOutputStream(sock.getOutputStream());
        DataInputStream in = new DataInputStream(sock.getInputStream());

        while (true) {
            String line;
            try {
                line = in.readUTF();
                System.out.println("[Node <- Leader] " + line);
            } catch (IOException e) {
                break;
            }
            JSONObject msg = new JSONObject(line);
            String header = JsonHelper.getHeader(msg);
            JSONObject payload = JsonHelper.getPayload(msg);
            System.out.println("[DEBUG] -> Header received to Node: " + header);
            System.out.println("[DEBUG] -> Payload received to Node: " + payload);


            if ("TASK".equals(header)) {
                List<Integer> intSlice = JsonHelper.convertFromArray(payload.getJSONArray("intSlice"));
                long delay = payload.getLong("delay");
                long sum = calcSum(intSlice, delay, genFault);
                JSONObject resp = JsonHelper.createMessage("RESPONSE", new JSONObject().put("nodeResSum", sum));
                System.out.println("[Node -> Leader] " + resp.toString());
                os.writeUTF(resp.toString());
                os.flush();



            } else if ("CONSENSUS".equals(header)) {
                List<Integer> cSlice = JsonHelper.convertFromArray(payload.getJSONArray("intSlice"));
                long reported = payload.getLong("reportedSum");
                long check = calcSum(cSlice, 0, false);
                boolean checkRes = false;
                if (reported == check) {
                    checkRes = true;
                }
                JSONObject vote = JsonHelper.createMessage("VOTE", new JSONObject().put("nodeCheckRes", checkRes));
                System.out.println("[Node -> Leader] " + vote.toString());
                os.writeUTF(vote.toString());
                os.flush();
            }
            
            }
        sock.close();
    }
        
    private static long calcSum(List<Integer> list, long delay, boolean fault) {
        System.out.println("[DEBUG] -> FAULT FLAG: " + fault);
        long res = 0;
        for (int i : list) {
            res += i;
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
        }
        if (fault) {
            int extra = new java.util.Random().nextInt(100) + 1;
            res += extra;
        }
        return res;
    }
}
