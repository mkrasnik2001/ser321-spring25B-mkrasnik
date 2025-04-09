package Assign32starter;

import java.awt.Dimension;

import org.json.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input text box,
 * a button, and a text area for status. 
 * 
 * Methods of Interest
 * ----------------------
 * show(boolean modal) - Shows the GUI frame with current state
 *     -> modal means that it opens GUI and suspends background processes. 
 * 		  Processing still happens in the GUI. If it is desired to continue processing in the 
 *        background, set modal to false.
 * newGame(int dimension) - Start a new game with a grid of dimension x dimension size
 * insertImage(String filename, int row, int col) - Inserts an image into the grid
 * appendOutput(String message) - Appends text to the output panel
 * submitClicked() - Button handler for the submit button in the output panel
 * 
 * Notes
 * -----------
 * > Does not show when created. show() must be called to show he GUI.
 * 
 */
public class ClientGui implements Assign32starter.OutputPanel.EventHandlers {
	JDialog frame;
	PicturePanel picPanel;
	OutputPanel outputPanel;
	String currentMess;
	Socket sock;
	OutputStream out;
	ObjectOutputStream os;
	BufferedReader bufferedReader;
	private String clientState;
	String clientName;

	// TODO: SHOULD NOT BE HARDCODED change to spec
	String host = "localhost";
	int port = 9000;

	/**
	 * Construct dialog
	 * @throws IOException 
	 */
	public ClientGui(String host, int port) throws IOException {
		clientState = "initName";
		clientName = "";
		this.host = host; 
		this.port = port; 
		
		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(1000, 600));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// setup the top picture frame
		picPanel = new PicturePanel();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.25;
		frame.add(picPanel, c);

		// setup the input, button, and output area
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.75;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		outputPanel = new OutputPanel();
		outputPanel.addEventHandlers(this);
		frame.add(outputPanel, c);

		picPanel.newGame(1);

		open(); // opening server connection here

		JSONObject startRequest = new JSONObject();
		JSONObject header = new JSONObject();
		JSONObject payload = new JSONObject();

		header.put("type", "start");
		header.put("player", clientName);  // clientName will be empty in this init
		header.put("ok", true);
		startRequest.put("header", header);
		startRequest.put("payload", payload);

		try {
			os.writeObject(startRequest.toString());
		} catch (IOException e) {
			e.printStackTrace();
}

		String string = this.bufferedReader.readLine();
		System.out.println("Got a connection to server");
		JSONObject json = new JSONObject(string);
		outputPanel.appendOutput(json.getJSONObject("payload").optString("value"));

		String base64Image = json.getJSONObject("payload").optString("imageBase64", "");
		try {
			picPanel.readBase64Img(base64Image, 0, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// reading out the image (abstracted here as just a string)
		/// would put image in picture panel
		//close(); //closing the connection to server

		// Now Client interaction only happens when the submit button is used, see "submitClicked()" method
	}

	/**
	 * Shows the menu options for the player once they send their name to the server
	 * 
	 */
	public void showMenu(){
		// Options to be shown in the dialog
		String[] options = { "Quit", "Leaderboard", "Start" };

		// Display option dialog on the Event Dispatch Thread (EDT)
		int option = JOptionPane.showOptionDialog(
			frame, 
			"Please select from the menu:", 
			"Welcome to MoviePixel", 
			JOptionPane.DEFAULT_OPTION, 
			JOptionPane.QUESTION_MESSAGE, 
			null, 
			options, 
			options[2]  
		);

		switch (option) {
		case 0: 
			clientState = "initName";
			outputPanel.appendOutput("You have quit. Please enter your name to start a new session.");
			break;
		case 1: 
			clientState = "leaderboard";
			outputPanel.appendOutput("Showing leaderboard... (TODO: implement leaderboard view)");
			showMenu();
			break;
		case 2: 
			clientState = "promptGameLength";
			outputPanel.appendOutput("Starting game...");

			sendStartGame();
			break;
		default: 
			outputPanel.appendOutput("Nothing selected! Please try again.");
			showMenu();
			break;
		}
	}

	/**
	 * Shows the menu options for an existing player once the game they played
	 * just ended.
	 * 
	 */
	public void showPlayAgainMenu(String endGameMessage){
		String[] options = { "Logout", "Leaderboard", "Play Again" };

		// Display option dialog on the Event Dispatch Thread (EDT)
		int option = JOptionPane.showOptionDialog(
			frame, 
			endGameMessage + " Do you want to play again or logout?", 
			"Game Over", 
			JOptionPane.DEFAULT_OPTION, 
			JOptionPane.QUESTION_MESSAGE, 
			null, 
			options, 
			options[2]  
		);

		switch (option) {
		case 0: 
			clientState = "initName";
			outputPanel.appendOutput("You have quit. Please enter your name to start a new session.");
			break;
		case 1: 
			clientState = "leaderboard";
			outputPanel.appendOutput("Showing leaderboard... (TODO: implement leaderboard view)");
			showPlayAgainMenu(endGameMessage);
			break;
		case 2: 
			clientState = "promptGameLength";
			outputPanel.appendOutput("Starting game...");

			sendStartGame();
			break;
		default: 
			outputPanel.appendOutput("Nothing selected! Please try again.");
			showPlayAgainMenu(endGameMessage);
			break;
		}
	}

	/**
	 * Send start request to the server of a new game
	 */
	private void sendStartGame() {
		try {
			JSONObject request = new JSONObject();
			JSONObject header = new JSONObject();
			JSONObject payload = new JSONObject();
	
			header.put("type", "promptGameLength");
			header.put("playerName", clientName);
			header.put("ok", true);
			payload.put("value", "Requesting to start the game");
			request.put("header", header);
			request.put("payload", payload);
	
			os.writeObject(request.toString());
			System.out.println("[DEBUG} -> Start game request sent: " + request.toString());
	
			// Read the server response
			String response = bufferedReader.readLine();
			JSONObject respJson = new JSONObject(response);
			//System.out.println("[DEBUG] -> Server Response Received: " + respJson.toString());
			JSONObject respPayload = respJson.getJSONObject("payload");
			String message = respPayload.optString("value");
			outputPanel.appendOutput(message);
			clientState = "checkGameLength";

		} catch (Exception e) {
			e.printStackTrace();
		}
	}






	/**
	 * Shows the current state in the GUI
	 * @param makeModal - true to make a modal window, false disables modal behavior
	 */
	public void show(boolean makeModal) {
		frame.pack();
		frame.setModal(makeModal);
		frame.setVisible(true);
	}

	/**
	 * Creates a new game and set the size of the grid 
	 * @param dimension - the size of the grid will be dimension x dimension
	 * No changes should be needed here
	 */
	public void newGame(int dimension) {
		picPanel.newGame(1);
		outputPanel.appendOutput("Started new game with a " + dimension + "x" + dimension + " board.");
	}

	/**
	 * Insert an image into the grid at position (col, row)
	 * 
	 * @param filename - filename relative to the root directory
	 * @param row - the row to insert into
	 * @param col - the column to insert into
	 * @return true if successful, false if an invalid coordinate was provided
	 * @throws IOException An error occured with your image file
	 */
	public boolean insertImage(String filename, int row, int col) throws IOException {
		System.out.println("Image insert");
		String error = "";
		try {
			// insert the image
			if (picPanel.insertImage(filename, row, col)) {
				// put status in output
				return true;
			}
			error = "File(\"" + filename + "\") not found.";
		} catch(PicturePanel.InvalidCoordinateException e) {
			// put error in output
			error = e.toString();
		}
		outputPanel.appendOutput(error);
		return false;
	}

	/**
	 * Submit button handling
	 * Handle sending appropriate requests to server after submit button is pressed.
	 */
	@Override
	public void submitClicked() {
		System.out.println("[DEBUG] -> Current Client State: " + clientState);
		try {
			String input = outputPanel.getInputText().trim();
			JSONObject request = new JSONObject();
			JSONObject reqHeader = new JSONObject();
			JSONObject reqPayload = new JSONObject();
	
			if (clientState.equals("initName")) {
				reqHeader.put("type", "name");
				reqHeader.put("playerName", input);
				reqHeader.put("ok", true);
				reqPayload.put("value", input);

			} else if (clientState.equals("checkGameLength")) {
				reqHeader.put("type", "checkGameLength");
				reqHeader.put("playerName", clientName); 
				reqHeader.put("ok", true);
				reqPayload.put("value", input);  

			} else if (clientState.equals("inGame")) {
				reqHeader.put("type", "game");
				reqHeader.put("playerName", clientName);
				reqHeader.put("ok", true);
				reqPayload.put("value", input);

			} else {
				reqHeader.put("type", "unknown");
				reqHeader.put("ok", false);
				reqPayload.put("value", input);
			}
			System.out.println("Client state: " + clientState);
			request.put("header", reqHeader);
			request.put("payload", reqPayload);
	
			os.writeObject(request.toString());
			System.out.println("Request sent -> " + request.toString());
	
			// Read the server response
			String response = bufferedReader.readLine();
			JSONObject respJson = new JSONObject(response);
			//System.out.println("[DEBUG] -> Server Response Received: " + respJson.toString());
	
			JSONObject respHeader = respJson.getJSONObject("header");
			JSONObject respPayload = respJson.getJSONObject("payload");
			String message = respPayload.optString("value");
			outputPanel.appendOutput(message);
			
			if (respPayload.has("points")) {
				int updatedPoints = respPayload.optInt("points", 0);
				outputPanel.setPoints(updatedPoints);
			}
			
			String base64Image = respPayload.optString("imageBase64", "");
			if (!base64Image.isEmpty()) {
				try {
					picPanel.readBase64Img(base64Image, 0, 0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Handle client state based on server response
			if (respHeader.optBoolean("ok", true)) {
				String type = respHeader.optString("type", "");
				if (type.equals("gameStart")) {
					clientState = "inGame";

				} else if (type.equals("gameUpdate") || type.equals("info")) {
					clientState = "inGame";

				} else if (type.equals("menuOpts") || (type.equals("gameOver") || type.equals("gameWin"))) {
					clientState = "menuOpts";
					clientName = reqHeader.optString("playerName", clientName);
					outputPanel.setPoints(0);
					if (type.equals("menuOpts")){
						showMenu();
					} else{
						String dialogMessage = respPayload.optString("value").replace("[MoviePixel Inc]:", "").trim();
						showPlayAgainMenu(dialogMessage);
					}
				}
			} else {
				if (clientState.equals("checkGameLength"))
					clientState = "checkGameLength";
				else
					clientState = "initName";
			}
		outputPanel.setInputText("");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	








	/**
	 * Key listener for the input text box
	 * 
	 * Change the behavior to whatever you need
	 */
	@Override
	public void inputUpdated(String input) {
		if (input.equals("surprise")) {
			outputPanel.appendOutput("You found me!");
		}
	}

	public void open() throws UnknownHostException, IOException {
		this.sock = new Socket(host, port); // connect to host and socket

		// get output channel
		this.out = sock.getOutputStream();
		// create an object output writer (Java only)
		this.os = new ObjectOutputStream(out);
		this.bufferedReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

	}
	
	public void close() {
        try {
            if (out != null)  out.close();
            if (bufferedReader != null)   bufferedReader.close(); 
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public static void main(String[] args) throws IOException {
		// create the frame


		try {
			String host = "localhost";
			int port = 8888;


			ClientGui main = new ClientGui(host, port);
			main.show(true);


		} catch (Exception e) {e.printStackTrace();}



	}
}