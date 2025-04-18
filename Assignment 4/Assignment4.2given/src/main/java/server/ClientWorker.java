package server;

import java.io.*;
import java.net.Socket;
import buffers.RequestProtos.*;
import buffers.ResponseProtos.*;

public class ClientWorker implements Runnable {
    private final Socket sock;
    private final int id;
    private final Game game;
    private boolean isForGrading;
    private String name = "";
    private InputStream in;
    private OutputStream out;
    private static String menuOptions = "\nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game";
    private static String gameOptions = "\nChoose an action: \n (1-9) - Enter an int to specify the row you want to update \n c - Clear number \n r - New Board";

    public ClientWorker(Socket sock, int id, boolean isForGrading) throws IOException {
        this.sock = sock;
        this.id = id;
        this.isForGrading = isForGrading;
        this.in = sock.getInputStream();
        this.out = sock.getOutputStream();
        this.game = new Game();
    }

    private void sendResp(Response resp) throws IOException {
        resp.writeDelimitedTo(out);
        out.flush();
    }

    private Response errorResp(int code, String field) {
        String msg = switch (code) {
            case 1 -> "Following field missing: " + field;
            case 2 -> "Request is not supported: " + field;
            default -> "Server error";
        };
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.ERROR)
                .setErrorType(code)
                .setMessage(msg)
                .setNext(1)
                .build();
    }

    private Response handlePlayerName(Request r){
        String playerName = r.getName().trim();
        if (playerName.isEmpty()) return errorResp(1, "name");
        LeaderboardSingleton.LB_INSTANCE.newLogin(playerName);
        name = playerName;
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.GREETING)
                .setMessage("Welcome " + playerName)
                .setMenuoptions(menuOptions)
                .setNext(2)
                .build();
    }

    private Response handleLb(){
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.LEADERBOARD)
                .addAllLeader(LeaderboardSingleton.LB_INSTANCE.convertProto())
                .setNext(2)
                .build();
    }
    



    @Override
    public void run() {
        System.out.println("Client-" + id + " connected");
        try { handleFlow(); } catch (IOException e) {
            System.err.println("Client-" + id + " crashed: " + e);
        } finally { finishAll(); }
    }




private void finishAll(){
    try{
        sock.close();
    } catch (IOException e){e.printStackTrace();}
}


/**
 * Handles the main flow of requests coming in from the client and generates
 * appropriate response
 * @throws IOException
 */
private void handleFlow() throws IOException{
    while (true){
        Request request = Request.parseDelimitedFrom(in);
        if (request == null) return;
        Response response;
        System.out.println("[DEBUG] -> Request Op Type: " + request.getOperationType());
        switch (request.getOperationType()){
            case NAME -> response = handlePlayerName(request);
            case LEADERBOARD -> response = handleLb();
            default -> response = errorResp(2, request.getOperationType().name());
        }

        sendResp(response);
    }

}

}