package org.distrib;

import org.json.JSONObject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * This class is responsible for distributing the consensus checks across the nodes and handle their votes
 */
public class DistribConsensus {
    public static boolean checkConsensus(List<Socket> nodeSocks, List<List<Integer>> slices, long[] partialSums) {
        int n = nodeSocks.size();
        boolean[] nodeVotes = new boolean[n];
        Thread[] threads = new Thread[n];

        for (int i = 0; i < n; i++) {
            final int idx = i;
            int neighbor = (idx + 1) % n;
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        Socket sock = nodeSocks.get(idx);
                        DataOutputStream os = new DataOutputStream(sock.getOutputStream());
                        DataInputStream  in = new DataInputStream(sock.getInputStream());

                        JSONObject cons = JsonHelper.createMessage("CONSENSUS",
                            new JSONObject()
                                .put("intSlice", JsonHelper.convertToJsonArray(slices.get(neighbor)))
                                .put("reportedSum", partialSums[neighbor])
                        );
                        System.out.println("[Leader -> Node " + idx + "] " + cons.toString());
                        os.writeUTF(cons.toString());
                        os.flush();

                        JSONObject resp = new JSONObject(in.readUTF());
                        nodeVotes[idx] = resp.getJSONObject("payload").getBoolean("nodeCheckRes");
                    } catch (Exception e) {
                        nodeVotes[idx] = false;
                    }
                }
            });
            threads[i].start();
        }

        // wait until all threads are done
        for (Thread tds : threads) {
            try { tds.join(); } catch ( Exception e) {}
        }

        for (boolean v : nodeVotes) {
            if (!v) return false;
        }
        return true;
    }
}