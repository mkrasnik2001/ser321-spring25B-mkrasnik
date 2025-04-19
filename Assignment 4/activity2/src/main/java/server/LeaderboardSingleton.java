package server;

import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.*;
import buffers.ResponseProtos;


public enum LeaderboardSingleton {
    LB_INSTANCE;
    private static final Path FILE = Paths.get("lb.json");
    private final Map<String,int[]> board = new ConcurrentHashMap<>();

    // this loads the leaderboard from file
    public void loadLb() {
        if (!Files.exists(FILE)) return;
        try (BufferedReader br = Files.newBufferedReader(FILE)) {
            JSONArray arr = new JSONArray(br.lines().collect(Collectors.joining()));
            for (Object o : arr) {
                JSONObject obj = (JSONObject) o;
                board.put(obj.getString("name"),
                          new int[]{obj.getInt("points"), obj.getInt("logins")});
            }
        } catch (Exception e) {
            System.err.println("Could not read lb.json");
        }
    }

    public void newLogin(String name) {
        board.compute(name, (k,v)-> v==null ? new int[]{0,1} : new int[]{v[0], v[1]+1});
        saveLb();
    }

    public void addPoints(String name, int d) {
        board.computeIfAbsent(name, k->new int[]{0,0})[0] += d;
        saveLb();
    }

    public List<ResponseProtos.Entry> convertProto() {
        return board.entrySet().stream()
                .sorted((a,b)->Integer.compare(b.getValue()[0], a.getValue()[0]))
                .map(e -> ResponseProtos.Entry.newBuilder()
                        .setName(e.getKey())
                        .setPoints(e.getValue()[0])
                        .setLogins(e.getValue()[1])
                        .build())
                .collect(Collectors.toList());
    }

    // Saves updated leaderboard and writes to file
    private synchronized void saveLb() {
        JSONArray arr = new JSONArray();
        board.forEach((name,val)-> {
            JSONObject obj = new JSONObject();
            obj.put("name",   name);
            obj.put("points", val[0]);
            obj.put("logins", val[1]);
            arr.put(obj);
        });
        try (BufferedWriter bw = Files.newBufferedWriter(FILE,
                 StandardOpenOption.CREATE,
                 StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write(arr.toString(2));         // pretty print, 2‑space indent
        } catch (IOException e) {
            System.err.println("Could not write lb.json: " + e);
        }
    }
}
