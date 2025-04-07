import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.*;



/**
 * A class to demonstrate a simple client-server connection using sockets.
 *
 */
public class SockServer {
  static List<JSONObject> quizQs = new ArrayList<>();
  static long quizGameStartTime;
  static String currQuizAns= null;
  static Socket sock;
  static DataOutputStream os;
  static ObjectInputStream in;

  static int port = 8888;

  public static void main (String args[]) {

    if (args.length != 1) {
      System.out.println("Expected arguments: <port(int)>");
      System.exit(1);
    }

    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      //open socket
      ServerSocket serv = new ServerSocket(port);
      System.out.println("Server ready for connections");

      /**
       * Simple loop accepting one client and calling handling one request.
       *
       */


      while (true){
        System.out.println("Server waiting for a connection");
        sock = serv.accept(); // blocking wait
        System.out.println("Client connected");

        // setup the object reading channel
        in = new ObjectInputStream(sock.getInputStream());

        // get output channel
        OutputStream out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new DataOutputStream(out);

        boolean connected = true;
        while (connected) {
          String s = "";
          try {
            s = (String) in.readObject(); // attempt to read string in from client
          } catch (Exception e) { // catch rough disconnect
            System.out.println("Client disconnect");
            connected = false;
            continue;
          }

          JSONObject res = isValid(s);

          if (res.has("ok")) {
            writeOut(res);
            continue;
          }

          JSONObject req = new JSONObject(s);

          res = testField(req, "type");
          if (!res.getBoolean("ok")) { // no "type" header provided
            res = noType(req);
            writeOut(res);
            continue;
          }
          // check which request it is (could also be a switch statement)
          if (req.getString("type").equals("echo")) {
            res = echo(req);
          } else if (req.getString("type").equals("add")) {
            res = add(req);
          } else if (req.getString("type").equals("addmany")) {
            res = addmany(req);
          } else if (req.getString("type").equals("stringconcatenation")){
            res = stringConcat(req);
          } else if (req.getString("type").equals("quizgame")){
            res = quizGame(req);
          }else {
            res = wrongType(req);
          }
          writeOut(res);
        }
        // if we are here - client has disconnected so close connection to socket
        overandout();
      }
    } catch(Exception e) {
      e.printStackTrace();
      overandout(); // close connection to socket upon error
    }
  }


  /**
   * Checks if a specific field exists
   *
   */
  static JSONObject testField(JSONObject req, String key){
    JSONObject res = new JSONObject();

    // field does not exist
    if (!req.has(key)){
      res.put("ok", false);
      res.put("message", "Field " + key + " does not exist in request");
      return res;
    }
    return res.put("ok", true);
  }

  // handles the simple echo request
  static JSONObject echo(JSONObject req){
    System.out.println("Echo request: " + req.toString());
    JSONObject res = testField(req, "data");
    if (res.getBoolean("ok")) {
      if (!req.get("data").getClass().getName().equals("java.lang.String")){
        res.put("ok", false);
        res.put("message", "Field data needs to be of type: String");
        return res;
      }

      res.put("type", "echo");
      res.put("echo", "Here is your echo: " + req.getString("data"));
    }
    return res;
  }

  // handles the simple add request with two numbers
  static JSONObject add(JSONObject req){
    System.out.println("Add request: " + req.toString());
    JSONObject res1 = testField(req, "num1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }

    JSONObject res2 = testField(req, "num2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    JSONObject res = new JSONObject();
    res.put("ok", true);
    res.put("type", "add");
    try {
      res.put("result", req.getInt("num1") + req.getInt("num2"));
    } catch (org.json.JSONException e){
      res.put("ok", false);
      res.put("message", "Field num1/num2 needs to be of type: int");
    }
    return res;
  }

  // implement me in assignment 3
  static JSONObject stringConcat(JSONObject req) {
    System.out.println("String concat request: " + req.toString());

    JSONObject res = testField(req, "string1");
    if (!res.getBoolean("ok")) {
        res.put("type", "stringconcatenation");
        res.put("ok", false);
        return res;
    }
    
    res = testField(req, "string2");
    if (!res.getBoolean("ok")) {
        res.put("type", "stringconcatenation");
        res.put("ok", false);
        return res;
    }
    
    if (!req.get("string1").getClass().getName().equals("java.lang.String") ||
        !req.get("string2").getClass().getName().equals("java.lang.String")) {
        
        res = new JSONObject();
        res.put("type", "stringconcatenation");
        res.put("ok", false);
        
        if (!req.get("string1").getClass().getName().equals("java.lang.String") &&
            !req.get("string2").getClass().getName().equals("java.lang.String")) {
              res.put("message", "Fields string1 and string2 must be of type String");
        } else if (!req.get("string1").getClass().getName().equals("java.lang.String")) {
          res.put("message", "Field string1 has to be a String type");
        } else { 
          res.put("message", "Field string2 needs to be of type: String");
        }
        return res;
    }
    
    // Reach here means all params are valid
    String concatenated = req.getString("string1") + req.getString("string2");
    res = new JSONObject();
    res.put("type", "stringconcatenation");
    res.put("ok", true);
    res.put("result", concatenated);
    return res;
}

  // implement me in assignment 3
  static JSONObject quizGame(JSONObject req) {
    System.out.println("QuizGame request: " + req.toString());
    JSONObject res = new JSONObject();
    if (req.has("addQuestion")) {
        boolean addQuestion = req.getBoolean("addQuestion");
        if (addQuestion) {
            res = testField(req, "question");
            if (!res.getBoolean("ok")) {
                res.put("type", "quizgame");
                res.put("ok", false);
                return res;
            }
            res = testField(req, "answer");
            if (!res.getBoolean("ok")) {
                res.put("type", "quizgame");
                res.put("ok", false);
                return res;
            }

            if (req.getString("answer").equals("") || req.getString("question").equals("")){
              res.put("type", "quizgame");
              res.put("ok", false);
              res.put("message", "Either the answer or question provided is empty. Try again!");
              return res;
            }
            if (!req.get("question").getClass().getName().equals("java.lang.String") ||
                !req.get("answer").getClass().getName().equals("java.lang.String")) {
                res = new JSONObject();
                res.put("type", "quizgame");
                res.put("ok", false);
                if (!req.get("question").getClass().getName().equals("java.lang.String") &&
                    !req.get("answer").getClass().getName().equals("java.lang.String")) {
                    res.put("message", "Fields question and answer must be of a String type");
                } else if (!req.get("question").getClass().getName().equals("java.lang.String")) {
                    res.put("message", "Field question has to be of a String type");
                } else {
                    res.put("message", "Field answer needs to be of a String type");
                }
                return res;
            }
            JSONObject newQa = new JSONObject();
            newQa.put("question", req.getString("question"));
            newQa.put("answer", req.getString("answer"));
            quizQs.add(newQa);
            res = new JSONObject();
            res.put("type", "quizgame");
            res.put("ok", true);
            return res;
        } else {
            if (quizQs.isEmpty()) {
                res = new JSONObject();
                res.put("type", "quizgame");
                res.put("ok", false);
                res.put("message", "There are no questions at the moment. Add a question to play!");
                return res;
            }
            quizGameStartTime = System.currentTimeMillis();
            System.out.println("Quiz started current time: " + quizGameStartTime);
            int index = new Random().nextInt(quizQs.size());
            JSONObject newQa = quizQs.get(index);
            currQuizAns = newQa.getString("answer");
            res = new JSONObject();
            res.put("type", "quizgame");
            res.put("ok", true);
            res.put("question", newQa.getString("question"));
            return res;
        }
    }
    
    if (req.has("answer")) {
      if (!req.get("answer").getClass().getName().equals("java.lang.String")) {
          res = new JSONObject();
          res.put("type", "quizgame");
          res.put("ok", false);
          res.put("message", "Field answer needs to be of a String type");
          return res;
      }
      long currentTime = System.currentTimeMillis();
      if (currentTime - quizGameStartTime >= 120000) {
          res = new JSONObject();
          res.put("type", "quizgame");
          res.put("ok", true);
          res.put("message", "2 min quiz times up!. Game over.");
          return res;
      }
      String providedAns = req.getString("answer");
      boolean result = (currQuizAns != null && providedAns.equals(currQuizAns));
      res = new JSONObject();
      res.put("type", "quizgame");
      res.put("result", result);
      res.put("ok", true);
      if (result) {
          // remove only correct questions
          for (int i = 0; i < quizQs.size(); i++) {
              JSONObject qa = quizQs.get(i);
              if (qa.getString("answer").equals(currQuizAns)) {
                  quizQs.remove(i);
                  break;
              }
          }
      }
      if (System.currentTimeMillis() - quizGameStartTime >= 10000) {
        res = new JSONObject();
        res.put("type", "quizgame");
        res.put("ok", false);
        res.put("message", "Time limit ran out! Game over.");
        return res;
    }
    if (quizQs.isEmpty()) {
        res = new JSONObject();
        res.put("type", "quizgame");
        res.put("ok", false);
        res.put("message", "No more questions left! Game over.");
        return res;
    } else {
        int index = new Random().nextInt(quizQs.size());
        JSONObject newQa = quizQs.get(index);
        currQuizAns = newQa.getString("answer");
        res.put("question", newQa.getString("question"));
        return res;
    }
    
}
    res = new JSONObject();
    res.put("type", "quizgame");
    res.put("ok", false);
    res.put("message", "Something went wrong with the quizgame request.");
    return res;
}











  // handles the simple addmany request
  static JSONObject addmany(JSONObject req){
    System.out.println("Add many request: " + req.toString());
    JSONObject res = testField(req, "nums");
    if (!res.getBoolean("ok")) {
      return res;
    }

    int result = 0;
    JSONArray array = req.getJSONArray("nums");
    for (int i = 0; i < array.length(); i ++){
      try{
        result += array.getInt(i);
      } catch (org.json.JSONException e){
        res.put("ok", false);
        res.put("message", "Values in array need to be ints");
        return res;
      }
    }

    res.put("ok", true);
    res.put("type", "addmany");
    res.put("result", result);
    return res;
  }

  // creates the error message for wrong type
  static JSONObject wrongType(JSONObject req){
    System.out.println("Wrong type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "Type " + req.getString("type") + " is not supported.");
    return res;
  }

  // creates the error message for no given type
  static JSONObject noType(JSONObject req){
    System.out.println("No type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "No request type was given.");
    return res;
  }

  // From: https://www.baeldung.com/java-validate-json-string
  public static JSONObject isValid(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      try {
        new JSONArray(json);
      } catch (JSONException ne) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "req not JSON");
        return res;
      }
    }
    return new JSONObject();
  }

  // sends the response and closes the connection between client and server.
  static void overandout() {
    try {
      os.close();
      in.close();
      sock.close();
    } catch(Exception e) {e.printStackTrace();}

  }

  // sends the response and closes the connection between client and server.
  static void writeOut(JSONObject res) {
    try {
      os.writeUTF(res.toString());
      // make sure it wrote and doesn't get cached in a buffer
      os.flush();

    } catch(Exception e) {e.printStackTrace();}

  }
}