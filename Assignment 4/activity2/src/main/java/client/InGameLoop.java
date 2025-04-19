package client;

import buffers.RequestProtos.*;
import buffers.ResponseProtos.*;

import java.io.*;
import java.net.Socket;

/**
 * This class handles the in game loop functionality and covers
 * the following protocol req types: QUIT, CLEAR and UPDATE and these for the
 * responses: BYE, WON, QUIT and PLAY from the server
 */
public final class InGameLoop {

    private final BufferedReader stdInput = new BufferedReader(
            new InputStreamReader(System.in));

    public Response runLoop(Socket sock, InputStream in, OutputStream out,
                    Response startResp) throws IOException, Exception {

        Response resp = startResp;
        while (true) {
            System.out.println("\n=== Sudoku Game ===");
            System.out.println(resp.getBoard());
            System.out.println("Points: " + resp.getPoints());
            System.out.println(resp.getMenuoptions());
            
            System.out.print("> ");
            String prompt = stdInput.readLine().trim();
            Request req;
            
            if (prompt.equalsIgnoreCase("exit")) {
                req = Request.newBuilder()
                        .setOperationType(Request.OperationType.QUIT)
                        .build();

            } else if (prompt.equalsIgnoreCase("c")) {
                int[] c = SockBaseClient.boardSelectionClear();
                if (c[0] == Integer.MIN_VALUE) continue;
                req = Request.newBuilder()
                        .setOperationType(Request.OperationType.CLEAR)
                        .setRow(c[0]).setColumn(c[1]).setValue(c[2])
                        .build();

            } else if (prompt.equalsIgnoreCase("r")) {
                req = Request.newBuilder()
                        .setOperationType(Request.OperationType.CLEAR)
                        .setRow(-1).setColumn(-1).setValue(6)
                        .build();
            
            
            // r c provided
            } else {
                String[] tokenArray = prompt.split("\\s+");
                if (tokenArray.length != 3) {
                    System.out.println("You must format it like: row col value");
                    continue;
                }
                int row = Integer.parseInt(tokenArray[0]);
                int col = Integer.parseInt(tokenArray[1]);
                int val = Integer.parseInt(tokenArray[2]);
                req = Request.newBuilder()
                        .setOperationType(Request.OperationType.UPDATE)
                        .setRow(row).setColumn(col).setValue(val)
                        .build();
            }

            req.writeDelimitedTo(out);
            out.flush();
            resp = Response.parseDelimitedFrom(in);


            switch (resp.getResponseType()) {
                case BYE -> {
                    System.out.println(resp.getMessage());
                    System.exit(0);
                }
                case WON -> {
                    System.out.println(resp.getMessage());
                    System.out.println(resp.getBoard());
                    System.out.println(resp.getMenuoptions());
                    return resp;
                }
                case PLAY -> {continue;}
                case ERROR  -> {
                    System.out.println(resp.getMessage());
                    continue;
                }
                default -> {
                    System.out.println("Something went wrong with this response: " + resp);
                    return resp;
                }
            }
        }
    }
}
