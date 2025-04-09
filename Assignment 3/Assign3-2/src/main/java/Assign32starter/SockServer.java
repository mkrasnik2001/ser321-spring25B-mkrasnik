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

	public static void main (String args[]) {
		Socket sock;
		try {
			
			//opening the socket here, just hard coded since this is just a bas example
			ServerSocket serv = new ServerSocket(8888); // TODO, should not be hardcoded
			System.out.println("Server ready for connetion");

			// placeholder for the person who wants to play a game
			String name = "";
			int points = 0;

			// read in one object, the message. we know a string was written only by knowing what the client sent. 
			// must cast the object from Object to desired type to be useful
			while(true) {
				sock = serv.accept(); // blocking wait

				// could totally use other input outpur streams here
				ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
				OutputStream out = sock.getOutputStream();

				String s = (String) in.readObject();
				JSONObject response = new JSONObject(); // response to send out
				JSONObject respHeader = new JSONObject();
				JSONObject respPayload = new JSONObject();


				JSONObject jsonRecieved = new JSONObject(s); // the requests that is received
				JSONObject headerRecieved = jsonRecieved.getJSONObject("header");
				JSONObject payloadReceived = jsonRecieved.getJSONObject("payload");

				System.out.println("[DEBUG] -> Client Request Received: " + jsonRecieved);
				String requestType = headerRecieved.getString("type");
				//Handshake
				if (requestType.equals("start")){
					
					System.out.println("- Got a start");
				
					response.put("type","hello" );
					response.put("value","[MoviePixel Inc]: Hello, please tell me your name." );
					sendImg("img/hi.png", response); // calling a method that will manipulate the image and will make it send ready
					
				
				// Handle Player Name
				} else if (requestType.equals("name")) {
					System.out.println("[DEBUG] -> Current player set: " + playerNames);
					String newPlayerName = payloadReceived.optString("value", "Player").trim();
					
					if (newPlayerName.isEmpty() || newPlayerName.matches("\\d+")) {
						System.out.println("Invalid player name received: " + newPlayerName);
						
						respHeader.put("type", "error");
						respHeader.put("ok", false);
						respPayload.put("value", "[MoviePixel Inc]: Invalid name. Name cannot be a number or empty!");
					
					} else if (playerNames.contains(newPlayerName)) {
                        // Duplicate name found
                        respHeader.put("type", "error");
                        respHeader.put("ok", false);
                        respPayload.put("value", "[MoviePixel Inc]: Name already in use. Please choose a different name."); 

					}else {
						System.out.println("Got new player: " + newPlayerName);
						
						respHeader.put("type", "menuOpts");
						respHeader.put("ok", true);
						respHeader.put("playerName", newPlayerName);
						respPayload.put("value", "[MoviePixel Inc]: Hello " + newPlayerName + ", welcome to the Movie Game! Please select above what you want to do...");
						playerNames.add(newPlayerName);
					}
				}







				else {
					System.out.println("not sure what you meant");
					// response.put("type","error" );
					// response.put("message","unknown response" );
				}
				response.put("header", respHeader);
				response.put("payload", respPayload);


				PrintWriter outWrite = new PrintWriter(sock.getOutputStream(), true); // using a PrintWriter here, you could also use and ObjectOutputStream or anything you fancy
				outWrite.println(response.toString());
			}
			
		} catch(Exception e) {e.printStackTrace();}
	}

	/* TODO this is for you to implement, I just put a place holder here */
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