package org.distrib;

import java.io.DataInputStream;
import org.json.JSONObject;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * This class is responsible for distributing the slicing tasks into seperate threads for the different nodes
 */
public class DistribSum {

    public static long[] execDistribution(List<Socket> nodeSocks, List<List<Integer>> slices, long delay) {
        int n = nodeSocks.size();
        long[] results = new long[n];
        Thread[] threads = new Thread[n];
        
        for (int i = 0; i < n; i++) {
            final int idx = i;
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        Socket sock = nodeSocks.get(idx);
                        DataOutputStream os = new DataOutputStream(sock.getOutputStream());
                        DataInputStream  in = new DataInputStream(sock.getInputStream());

                        JSONObject taskObj = JsonHelper.createMessage("TASK",
                            new JSONObject()
                                .put("intSlice", JsonHelper.convertToJsonArray(slices.get(idx)))
                                .put("delay", delay)
                        );
                        System.out.println("[Leader -> Node " + idx + "] " + taskObj.toString());
                        os.writeUTF(taskObj.toString());
                        os.flush();

                        JSONObject resp = new JSONObject(in.readUTF());
                        results[idx] = resp.getJSONObject("payload").getLong("nodeResSum");
                    } catch (Exception e) {
                        e.printStackTrace();
                        results[idx] = 0;
                    }
                }
            });
            threads[i].start();
        }


        // wait until all threads are done
        for (Thread tds : threads) {
            try { tds.join(); } catch ( Exception e) {}
        }

        return results;
    }
}
