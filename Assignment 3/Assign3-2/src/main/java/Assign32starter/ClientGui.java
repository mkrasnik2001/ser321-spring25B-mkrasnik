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
		frame.setMinimumSize(new Dimension(500, 500));
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
		insertImage("img/TheDarkKnight1.png", 0, 0);

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
		outputPanel.appendOutput(json.getString("value")); // putting the message in the outputpanel

		// reading out the image (abstracted here as just a string)
		System.out.println("Pretend I got an image: " + json.getString("image"));
		/// would put image in picture panel
		close(); //closing the connection to server

		// Now Client interaction only happens when the submit button is used, see "submitClicked()" method
	}

	/**
	 * Shows the menu options for the player once they send their name to the server
	 * @param makeModal
	 */
	public void showMenu(){
		// Options to be shown in the dialog
		String[] options = { "Quit", "Leaderboard", "Start" };

		// Display option dialog on the Event Dispatch Thread (EDT)
		int option = JOptionPane.showOptionDialog(
			frame, 
			"Please select from the menu:", 
			"Main Menu", 
			JOptionPane.DEFAULT_OPTION, 
			JOptionPane.QUESTION_MESSAGE, 
			null, 
			options, 
			options[2]  
		);

		// Use a switch statement to handle each selection
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
			clientState = "startGame";
			outputPanel.appendOutput("Starting game...");
			sendStartGame();
			break;
		default: 
			outputPanel.appendOutput("Nothing selected! Please try again.");
			showMenu();
			break;
		}
	}

	private void sendStartGame() {
		try {
			JSONObject request = new JSONObject();
			JSONObject header = new JSONObject();
			JSONObject payload = new JSONObject();
	
			header.put("type", "startGame");
			header.put("player", clientName);
			header.put("ok", true);
			payload.put("value", "Requesting to start the game");
			request.put("header", header);
			request.put("payload", payload);
	
			os.writeObject(request.toString());
			System.out.println("Start game request sent: " + request.toString());
	
			String response = bufferedReader.readLine();
			JSONObject respJson = new JSONObject(response);
			String message = respJson.getJSONObject("payload").optString("value");
			outputPanel.appendOutput(message);
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
	 * 
	 * TODO: This is where your logic will go or where you will call appropriate methods you write. 
	 * Right now this method opens and closes the connection after every interaction, if you want to keep that or not is up to you. 
	 */
	@Override
	public void submitClicked() {
		try {
			open();
			String input = outputPanel.getInputText().trim();
			JSONObject request = new JSONObject();
			JSONObject reqHeader = new JSONObject();
			JSONObject reqPayload = new JSONObject();
			
			// Make name request
			if (clientState.equals("initName")) {
				reqHeader.put("type", "name");
				reqHeader.put("playerName", input);
				reqHeader.put("ok", true);
				reqPayload.put("value", input);
			} else {
				//TODO
				reqHeader.put("type", "unknown");
				reqHeader.put("ok", false);
				reqPayload.put("value", input);
			}
			System.out.println("Client state: " + clientState);

			// Build request
			request.put("header", reqHeader);
			request.put("payload", reqPayload);
			
			os.writeObject(request.toString());
			System.out.println("Request sent -> " + request);
			
			// Read the server response
			String response = bufferedReader.readLine();
			JSONObject respJson = new JSONObject(response);
			System.out.println("[DEBUG] -> Server Response Received: " + respJson.toString());
			
			JSONObject respHeader = respJson.getJSONObject("header");
			JSONObject respPayload = respJson.getJSONObject("payload");
			String message = respPayload.optString("value");
			outputPanel.appendOutput(message);
			
			// Handle error from server
			if (respHeader.optBoolean("ok", true)) {
				clientState = "menuOpts";
				clientName = input;
				System.out.println("[DEBUG] -> Server status ok: true, moving to next state: " + clientState);
				showMenu();
			} else {
				clientState = "initName";
				System.out.println("[DEBUG] -> Server status ok: false, client state: " + clientState);
			}
			
			close();
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