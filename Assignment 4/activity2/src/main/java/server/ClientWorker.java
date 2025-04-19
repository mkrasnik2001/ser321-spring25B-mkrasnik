package server;

import java.io.*;
import java.net.Socket;
import java.util.List;

import buffers.RequestProtos.*;
import buffers.ResponseProtos.*;
import buffers.ResponseProtos.Response.ResponseType;

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
    private int currState = 1;

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
        // only called for code==2 now
        return Response.newBuilder()
            .setResponseType(Response.ResponseType.ERROR)
            .setErrorType(2)
            .setMessage("\nError: request not supported")
            .setNext(1)
            .build();
    }

    private Response handlePlayerName(Request r) {
        if (!r.hasName() || r.getName().trim().isEmpty()) {
            currState = 1;
            return Response.newBuilder()
                .setResponseType(Response.ResponseType.ERROR)
                .setErrorType(1)
                .setMessage("\nError: required field missing or empty")
                .setNext(1)
                .build();
        }
        String playerName = r.getName().trim();
        LeaderboardSingleton.LB_INSTANCE.newLogin(playerName);
        name = playerName;
        currState = 2;
    
        return Response.newBuilder()
            .setResponseType(Response.ResponseType.GREETING)
            .setMessage("Hello " + playerName + " and welcome to a simple game of Sudoku.")
            .setMenuoptions(menuOptions)
            .setNext(2)
            .build();
    }

    private Response handleLb(Request r) {
        List<Entry> entries = LeaderboardSingleton.LB_INSTANCE.convertProto();
        currState = 2;
        return Response.newBuilder()
            .setResponseType(Response.ResponseType.LEADERBOARD)
            .addAllLeader(entries)
            .setMenuoptions(menuOptions)
            .setNext(2)
            .build();
        }
    
    private Response handleStart(Request r){
        int difficulty = 1;
        if (r.hasDifficulty()){
            difficulty = r.getDifficulty();
        }
        System.out.println("[DEBUG] -> Difficulty received: " + difficulty);
        if (difficulty < 1 || difficulty > 20) {
            currState= 2;
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(5)
                    .setMessage("\nError: difficulty is out of range")
                    .setMenuoptions(menuOptions)
                    .setNext(2)
                    .build();
        }
        game.newGame(isForGrading, difficulty);
        currState = 3;
        // for grading here is the solution board
        System.out.println("[GRADING] -> Solution Board for Client " + id + ":");
        System.out.println(game.getSolBoard());
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.START)
                .setMessage("\nStarting new game.")
                .setBoard(game.getDisplayBoard())
                .setMenuoptions(gameOptions)
                .setPoints(game.getPoints())
                .setNext(3)
                .build();
    }

    private Response handleUpdate(Request r) {
        int row = r.getRow() - 1;
        int col = r.getColumn() - 1;
        int val = r.getValue();
        if (row < 0 || row >= 9 || col < 0 || col >= 9 || val < 1 || val > 9) {
            return outOfBoundsErr(Response.ResponseType.PLAY);
        }
        int code = game.updateBoard(row, col, val, 0);
        Response.EvalType et = EvalTypeMapper.mapFromCode(code);
    
        if (code != 0) game.setPoints(-2);
        //update lb
        LeaderboardSingleton.LB_INSTANCE.addPoints(name, game.getPoints());
        
        // game won handle
        if (game.getWon()) {
            currState = 2;
            game.setPoints(+20);
            //update lb
            LeaderboardSingleton.LB_INSTANCE.addPoints(name, game.getPoints());
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.WON)
                    .setBoard(game.getDisplayBoard())
                    .setMenuoptions(menuOptions)
                    .setMessage("You solved the current puzzle, good job!")
                    .setPoints(game.getPoints())
                    .setType(Response.EvalType.UPDATE)
                    .setNext(2)
                    .build();
        }
        currState = 3;
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.PLAY)
                .setBoard(game.getDisplayBoard())
                .setMenuoptions(gameOptions)
                .setPoints(game.getPoints())
                .setType(et)
                .setNext(3)
                .build();
    }

    private Response handleClear(Request r) {
        int val = r.getValue();
        int row = r.getRow();
        int col = r.getColumn();
        System.out.println("[DEBUG] -> Clear val: " + val);
        System.out.println("[DEBUG] -> Row: " + row);
        System.out.println("[DEBUG] -> Col: " + col);

        boolean invalid = switch (val) {
            case 1 -> (row < 1 || row > 9) || (col < 1 || col > 9);
            case 2 -> (row < 1 || row > 9) || (col != -1);
            case 3 -> (row != -1) || (col < 1 || col > 9);
            case 4 -> (row < 1 || row > 9) || (col != -1);
            case 5 -> !(row == -1 && col == -1);
            default -> false;
        };
        if (invalid) {
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(3)
                    .setMessage("Error: row or column out of bounds for clear type " + val)
                    .setBoard(game.getDisplayBoard())
                    .setMenuoptions(gameOptions)
                    .setNext(3)
                    .build();
        }

        if (val == 6) {
            if (!isForGrading) {
                game.create();
                game.prepareForPlay();
            } else {
                game.newBoard(true);
            }
            System.out.println("[GRADING] -> Solution Board for Client " + id);
            System.out.println(game.getSolBoard());
        } else {
            game.updateBoard(r.getRow()-1, r.getColumn()-1, 0, val);
        }
    
        game.setPoints(-5);
        LeaderboardSingleton.LB_INSTANCE.addPoints(name, game.getPoints());
    
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.PLAY)
                .setBoard(game.getDisplayBoard())
                .setMenuoptions(gameOptions)
                .setPoints(game.getPoints())
                .setType(EvalTypeMapper.mapFromVal(val))
                .setNext(3)
                .build();
    }
    

    private Response handleQuit(){
        currState = 2;
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.BYE)
                .setMessage("Have a nice day, " + name + "!")
                .build();
    }

    private Response outOfBoundsErr(Response.ResponseType type){
        String options = menuOptions;
        if (type == Response.ResponseType.PLAY){
            options = gameOptions;
        }
        return Response.newBuilder()
            .setResponseType(Response.ResponseType.ERROR)
            .setMessage("Error: row, column or value is out of bounds")
            .setErrorType(3)
            .setBoard(game.getDisplayBoard())
            .setMenuoptions(options)
            .setNext(type == Response.ResponseType.PLAY ? 3 : 2)
            .build();
    }

    private Response errorOutOfSequence() {
        String opts = (currState == 3 ? gameOptions : menuOptions);
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.ERROR)
                .setErrorType(2)
                .setMessage("\nError: required field missing or empty")
                .setMenuoptions(opts)
                .setNext(currState)
                .build();
    }





    @Override
    public void run() {
        System.out.println("Client -> " + id + " connected");
        try { handleFlow(); } catch (IOException e) {
            System.err.println("Client -> " + id + " crashed: " + e);
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
private void handleFlow() throws IOException {
    while (true) {
        Request request = Request.parseDelimitedFrom(in);
        if (request == null) return;
        Response response;
        switch (request.getOperationType()) {

            case NAME:
                if (currState != 1) {
                    response = errorOutOfSequence();
                } else {
                    response = handlePlayerName(request);
                    //currState = 2;
                }
                break;

            case LEADERBOARD:
                if (currState != 2) {
                    response = errorOutOfSequence();
                } else {
                    response = handleLb(request);
                    // stay in main menu
                }
                break;

            case START:
                if (currState != 2) {
                    response = errorOutOfSequence();
                } else {
                    response = handleStart(request);
                    //currState = 3;
                }
                break;

            case UPDATE:
                if (currState != 3) {
                    response = errorOutOfSequence();
                } else {
                    response = handleUpdate(request);
                    if (response.getResponseType() == Response.ResponseType.WON) {
                        currState = 2;
                    }
                }
                break;

            case CLEAR:
                if (currState != 3) {
                    response = errorOutOfSequence();
                } else {
                    response = handleClear(request);
                }
                break;

            case QUIT:
                if (currState != 2 && currState != 3) {
                    response = errorOutOfSequence();
                    sendResp(response);
                    continue;
                }
                response = handleQuit();
                sendResp(response);
                return;

            default:
                    response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.ERROR)
                            .setErrorType(2)
                            .setMessage("\nError: request not supported")
                            .setNext(currState == 1 ? 1 : currState)
                            .build();
                //response = errorResp(2, request.getOperationType().name());
        }

        sendResp(response);
    }
}

}