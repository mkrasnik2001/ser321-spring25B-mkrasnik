package Assign32starter;
import java.net.*;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import java.awt.image.BufferedImage;
import java.io.*;
import org.json.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Ser321 Foundations of Distributed Software Systems
 */
public class SockServer {
    static Stack<String> imageSource = new Stack<String>();
    static Set<String> playerNames = new HashSet<>();
    static Map<String, JSONObject> currentPlayers = new HashMap<>();

    public static void main (String args[]) {
        Socket sock;
        try {
            //opening the socket here, just hard coded since this is just a bas example
            ServerSocket serv = new ServerSocket(8888); // TODO, should not be hardcoded
            System.out.println("Server ready for connetion");

            while(true) {
                sock = serv.accept(); // blocking wait
                // For persistent connection, we will not close the socket for each request.
                // Instead, we keep reading until the client disconnects.
                try {
                    // Set up input and output streams
                    ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
                    OutputStream out = sock.getOutputStream();
                    
                    while (true) {
                        String s = (String) in.readObject();
                        JSONObject response = new JSONObject(); // final response to send out
                        JSONObject respHeader = new JSONObject();
                        JSONObject respPayload = new JSONObject();

                        JSONObject jsonRecieved = new JSONObject(s);
                        JSONObject headerRecieved = jsonRecieved.getJSONObject("header");
                        JSONObject payloadReceived = jsonRecieved.getJSONObject("payload");

                        System.out.println("[DEBUG] -> Client Request Received: " + jsonRecieved);
                        String requestType = headerRecieved.getString("type");

                        //Handle Handshake
                        if (requestType.equals("start")){
                            System.out.println("- Got a start");
                            // Build a hello response
                            respHeader.put("type", "hello");
                            respHeader.put("ok", true);
                            respPayload.put("value", "[MoviePixel Inc]: Hello, please tell me your name.");
                            sendImg("img/hi.png", respPayload);

                        // Handle Player Name
                        } else if (requestType.equals("name")) {
                            System.out.println("[DEBUG] -> Current player set: " + playerNames);
                            String newPlayerName = headerRecieved.optString("playerName").trim();

                            if (newPlayerName.isEmpty() || newPlayerName.matches("\\d+")) {
                                System.out.println("Invalid player name received: " + newPlayerName);
                                respHeader.put("type", "error");
                                respHeader.put("ok", false);
                                respPayload.put("value", "[MoviePixel Inc]: Invalid name. Name cannot be a number or empty!");
							} else if (currentPlayers.containsKey(newPlayerName)) {
								// Returning player: use the existing record
								System.out.println("Returning player: " + newPlayerName);
                                JSONObject playerState = currentPlayers.get(newPlayerName);
                                if (!playerState.has("highestscore")) {
                                    playerState.put("highestscore", 0);
                                }
                                int highestScore = playerState.optInt("highestscore", 0);
								respHeader.put("type", "menuOpts");
								respHeader.put("ok", true);
								respHeader.put("playerName", newPlayerName);
								respPayload.put("value", "[MoviePixel Inc]: Welcome back " + newPlayerName + ". Your highest score is: " + highestScore + " Please select what you want to do...");
                            } else {
                                System.out.println("Got new player: " + newPlayerName);
                                respHeader.put("type", "menuOpts");
                                respHeader.put("ok", true);
                                respHeader.put("playerName", newPlayerName);
                                respPayload.put("value", "[MoviePixel Inc]: Hello " + newPlayerName + ", welcome to the Movie Game! Please select above what you want to do...");
                                currentPlayers.put(newPlayerName, new JSONObject());
                            }

                        // Handl Prompt Length
                        } else if (requestType.equals("promptGameLength")) {
                            // Note: Use "playerName" since the client sends it that way
                            String playerName = headerRecieved.optString("playerName").trim();
                            respHeader.put("type", "promptGameLength");
                            respHeader.put("ok", true);
                            respHeader.put("playerName", playerName);
                            respPayload.put("value", "Please enter game duration: 'short' (30 sec), 'medium' (60 sec) or 'long' (90 sec).");


                        // Handle Game Length
                        } else if (requestType.equals("checkGameLength")) {
                            // Default duration values for a short game
                            String duration = payloadReceived.optString("value", "short").toLowerCase();
                            int skipsAllowed = 2;    
                            int gameSec = 30;

                            // Adjust values based on input
                            if (duration.equals("medium")) {
                                gameSec = 60;
                                skipsAllowed = 4;
                            } else if (duration.equals("long")) {
                                gameSec = 90;
                                skipsAllowed = 6;
                            } else if (!duration.equals("short")) {
                                respHeader.put("type", "error");
                                respHeader.put("ok", false);
                                respPayload.put("value", "[MoviePixel Inc]: Invalid duration given. Please enter 'short (30 sec game)', 'medium (60 sec game)' or 'long (90 sec game)'.");
                                respPayload.put("duration", 0);
                            }
                            
                            // Build success response if no error has been set
                            if (!respHeader.has("type") || !respHeader.getString("type").equals("error")) {
                                long roundEndTime = System.currentTimeMillis() + (gameSec * 1000);
                                
                                String playerName = headerRecieved.optString("playerName").trim();
								
                                JSONObject playerState;
								if (currentPlayers.containsKey(playerName)) {
									playerState = currentPlayers.get(playerName);
								} else {
									playerState = new JSONObject();
									playerState.put("highestscore", 0);
								}

                                playerState.put("duration", gameSec);
                                playerState.put("skipsAllowed", skipsAllowed);
                                playerState.put("gameEndTime", roundEndTime);
                                playerState.put("points", 0);
                                currentPlayers.put(playerName, playerState);
                                
                                System.out.println("[DEBUG] -> Updated Player Map: " + currentPlayers);
                                
                                respHeader.put("type", "gameStart");
                                respHeader.put("ok", true);
                                respHeader.put("playerName", playerName);
                                respPayload.put("value", "[MoviePixel Inc]: " + gameSec + " sec game started! Type in your guess (Can be upper case or lower case), type 'next' to view the next image, 'skip' to skip this image or 'remaining' to see remaining images left!");
                                respPayload.put("duration", gameSec);
                                respPayload.put("skipsAllowed", skipsAllowed);
                            }

                         // Handle Game Actions (next skip reamining guss)
						} else if (requestType.equals("game")) {
							String action = payloadReceived.optString("value", "").trim();
							String playerName = headerRecieved.optString("playerName", "").trim();
							
							// Get player dict of current player playing
							JSONObject playerState = currentPlayers.get(playerName);
							if (playerState == null) {
								respHeader.put("type", "error");
								respHeader.put("ok", false);
								respPayload.put("value", "[MoviePixel Inc]: Your player name was not found in my system. Please restart the game.");
							} else {
								// Check if the game time has expired
								long gameEndTime = playerState.optLong("gameEndTime", 0);
								if (System.currentTimeMillis() > gameEndTime) {
									int finalScore = playerState.optInt("points", 0);
									int highestScore = playerState.optInt("highestscore", 0);
									if (finalScore > highestScore) {
										highestScore = finalScore;
										playerState.put("highestscore", highestScore);
									}
									respHeader.put("type", "gameOver");
									respHeader.put("ok", true);
									respPayload.put("value", "[MoviePixel Inc]: Your time ran out Game over! Final score: " + finalScore +
													". Highest Score: " + highestScore);
								} else {
									if (action.equalsIgnoreCase("skip")) {
										int skipsRemaining = playerState.optInt("skipsAllowed", 0);
										if (skipsRemaining > 0) {
											skipsRemaining -= 1;
											playerState.put("skipsAllowed", skipsRemaining);
											playerState.put("currentImageLevel", 1);
											
											respHeader.put("type", "gameUpdate");
											respHeader.put("ok", true);
											respPayload.put("value", "[MoviePixel Inc]: Movie skipped. Here is a new movie image.");
											respPayload.put("imageLevel", 1);
											sendImg("img/TheDarkKnight1.png", respPayload);
										} else {
											respHeader.put("type", "gameUpdate");
											respHeader.put("ok", true);
											respPayload.put("value", "[MoviePixel Inc]: No skips remaining.");
										}
									} else if (action.equalsIgnoreCase("next")) {
										int currentImageLevel = playerState.optInt("currentImageLevel", 1);
										if (currentImageLevel < 4) {
											currentImageLevel++;
											playerState.put("currentImageLevel", currentImageLevel);
											respHeader.put("type", "gameUpdate");
											respHeader.put("ok", true);
											respPayload.put("value", "[MoviePixel Inc]: Here is a less pixelated image.");
											respPayload.put("imageLevel", currentImageLevel);
											sendImg("img/TheDarkKnight" + currentImageLevel + ".png", respPayload);
										} else {
											respHeader.put("type", "gameUpdate");
											respHeader.put("ok", true);
											respPayload.put("value", "[MoviePixel Inc]: No more 'nexts' available this is the clearest image you are given!.");
										}
									} else if (action.equalsIgnoreCase("remaining")) {
										int skipsRemaining = playerState.optInt("skipsAllowed", 0);
										respHeader.put("type", "info");
										respHeader.put("ok", true);
										respPayload.put("value", "[MoviePixel Inc]: Skips remaining: " + skipsRemaining);
									} else {
										// This is a guess
										String correctAnswer = "The Dark Knight"; // Hardcoded for this example.
										if (action.equalsIgnoreCase(correctAnswer)) {
											// Correct guess
											int points = playerState.optInt("points", 0);
											points += 10; // Award 10 points for a correct guess
											playerState.put("points", points);
											playerState.put("currentImageLevel", 1);
											
											respHeader.put("type", "gameUpdate");
											respHeader.put("ok", true);
											respPayload.put("value", "[MoviePixel Inc]: Correct! You've earned 10 points. Next movie:");
											respPayload.put("imageLevel", 1);
											respPayload.put("points", playerState.optInt("points", 0));
											sendImg("img/TheDarkKnight1.png", respPayload);
										} else {
											// Incorrect guess
											respHeader.put("type", "gameUpdate");
											respHeader.put("ok", true);
											respPayload.put("value", "[MoviePixel Inc]: Incorrect guess. Try again or type 'skip', 'next', or 'remaining'.");
											respPayload.put("points", playerState.optInt("points", 0));

										}
									}
								}
							}
						
						System.out.println("[DEBUG] -> Current Player Map: " + currentPlayers);	

						}else {
                            System.out.println("not sure what you meant");
                            respHeader.put("type", "error");
                            respHeader.put("ok", false);
                            respPayload.put("value", "[MoviePixel Inc]: Unknown request type.");
                        }

                        response.put("header", respHeader);
                        response.put("payload", respPayload);

                        PrintWriter outWrite = new PrintWriter(sock.getOutputStream(), true);
                        outWrite.println(response.toString());
                        System.out.println("Response sent: " + response.toString());
                } catch (EOFException eof) {
                    System.out.println("Client disconnected.");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try { sock.close(); } catch(Exception e) { e.printStackTrace(); }
                }
            } 
        } catch(Exception e) { e.printStackTrace(); }
    }

    /* TODO this is for you to implement, I just put a placeholder here */
    public static JSONObject sendImg(String filename, JSONObject obj) throws Exception {
        File file = new File(filename);

        if (file.exists()) {
            // import image
            // I did not use the Advanced Custom protocol
            // I read in the image and translated it into basically into a string and send it back to the client where I then decoded again
            obj.put("image", "Pretend I am this image: " + filename);
        } 
        return obj;
    }
}
