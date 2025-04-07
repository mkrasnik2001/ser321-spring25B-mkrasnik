import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 */
class SockClient {
  static Socket sock = null;
  static String host = "localhost";
  static int port = 8888;
  static OutputStream out;
  // Using and Object Stream here and a Data Stream as return. Could both be the same type I just wanted
  // to show the difference. Do not change these types.
  static ObjectOutputStream os;
  static DataInputStream in;
  public static void main (String args[]) {

    if (args.length != 2) {
      System.out.println("Expected arguments: <host(String)> <port(int)>");
      System.exit(1);
    }

    try {
      host = args[0];
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      connect(host, port); // connecting to server
      System.out.println("Client connected to server.");
      boolean requesting = true;
      while (requesting) {
        System.out.println("What would you like to do: 1 - echo, 2 - add, 3 - addmany, 4 - string concatenation, 5 - quizz (0 to quit)");
        Scanner scanner = new Scanner(System.in);
        int choice = Integer.parseInt(scanner.nextLine());
        // You can assume the user put in a correct input, you do not need to handle errors here
        // You can assume the user inputs a String when asked and an int when asked. So you do not have to handle user input checking
        JSONObject json = new JSONObject(); // request object
        switch(choice) {
          case 0:
            System.out.println("Choose quit. Thank you for using our services. Goodbye!");
            requesting = false;
            break;
          case 1:
            System.out.println("Choose echo, which String do you want to send?");
            String message = scanner.nextLine();
            json.put("type", "echo");
            json.put("data", message);
            break;
          case 2:
            System.out.println("Choose add, enter first number:");
            String num1 = scanner.nextLine();
            json.put("type", "add");
            json.put("num1", num1);

            System.out.println("Enter second number:");
            String num2 = scanner.nextLine();
            json.put("num2", num2);
            break;
          case 3:
            System.out.println("Choose addmany, enter as many numbers as you like, when done choose 0:");
            JSONArray array = new JSONArray();
            String num = "1";
            while (!num.equals("0")) {
              num = scanner.nextLine();
              array.put(num);
              System.out.println("Got your " + num);
            }
            json.put("type", "addmany");
            json.put("nums", array);
            break;
          case 4:
            System.out.println("Enter 2 values to concatenate (each can be a String or an int): ");
            array = new JSONArray();
            json.put("type", "stringconcatenation");
            while (array.length() != 2) {
                String input = scanner.nextLine();
                if (input.isEmpty()) {
                    System.out.println("Warning - no input provided for this string field.");
                    array.put(JSONObject.NULL);
                } else {
                    try {
                        int intValue = Integer.parseInt(input);
                        array.put(intValue);
                        if (array.length() == 1) {
                            json.put("string1", intValue);
                        } else {
                            json.put("string2", intValue);
                        }
                    } catch (NumberFormatException e) {
                        array.put(input);
                        if (array.length() == 1) {
                            json.put("string1", input);
                        } else {
                            json.put("string2", input);
                        }
                    }
                    System.out.println("Got value: '" + input + "'");
                }
              }
              break;
          case 5:
            System.out.println("Quiz Game:");
            System.out.println("Type 1 to add a new question. Type 0 (or any other key) to play the quiz game.");
            System.out.print("Please Note: Quiz questions will be removed from the available questions IF AND ONLY IF they are answered correctly! The game will end either when that happens or your 2 minute quiz timer expires when you start your quiz!");
            String usrChoice = scanner.nextLine();
            json = new JSONObject();
            json.put("type", "quizgame");
            if (usrChoice.equals("1")) {
                json.put("addQuestion", true);
                System.out.println("Enter the new quiz question:");
                String question = scanner.nextLine();
                json.put("question", question);
                System.out.println("Enter the answer for the question:");
                String answer = scanner.nextLine();
                json.put("answer", answer);
            } else {
                json.put("addQuestion", false);
            }
            
            os.writeObject(json.toString());
            os.flush();
            
            String response = (String) in.readUTF();
            JSONObject res = new JSONObject(response);
            System.out.println("Got response: " + res);
            
            if (!json.getBoolean("addQuestion") && res.getBoolean("ok") && res.has("question")) {
              do {
                  System.out.println("Answer the following question: " + res.getString("question"));
                  String userAnswer = scanner.nextLine();
                  JSONObject answerJson = new JSONObject();
                  answerJson.put("type", "quizgame");
                  answerJson.put("answer", userAnswer);
                  os.writeObject(answerJson.toString());
                  os.flush();
                  String answerResponse = (String) in.readUTF();
                  res = new JSONObject(answerResponse);
                  System.out.println("Got response: " + res);
              } while (res.has("question") && (!res.has("message") || !res.getString("message").contains("Game over")));
          }
          continue;

        }
        if(!requesting) {
          continue;
        }

        // write the whole message
        os.writeObject(json.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        // handle the response
        // - not doing anything other than printing payload
        // !! you will most likely need to parse the response for the other 2 services!
        String i = (String) in.readUTF();
        JSONObject res = new JSONObject(i);
        System.out.println("Got response: " + res);
        if (res.getBoolean("ok")) {
          String type = res.getString("type");
          if (type.equals("echo")) {
            System.out.println(res.getString("echo"));
          } else if (type.equals("stringconcatenation")) {
            System.out.println(res.getString("result"));
          } else if (type.equals("quizgame")) {
            if (res.has("result")) {
              System.out.println("Result: " + res.getBoolean("result"));
            }
            if (res.has("message")) {
              System.out.println("Message: " + res.getString("message"));
            }
          } else {
            System.out.println(res.getInt("result"));
          }
        } else {
          if (res.has("message")) {
            System.out.println(res.getString("message"));
          } else {
            System.out.println("Something went wrong while parsing response!");
          }
        }
      }
      // want to keep requesting services so don't close connection
      //overandout();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void overandout() throws IOException {
    //closing things, could
    in.close();
    os.close();
    sock.close(); // close socked after sending
  }

  public static void connect(String host, int port) throws IOException {
    // open the connection
    sock = new Socket(host, port); // connect to host and socket on port 8888

    // get output channel
    out = sock.getOutputStream();

    // create an object output writer (Java only)
    os = new ObjectOutputStream(out);

    in = new DataInputStream(sock.getInputStream());
  }
}