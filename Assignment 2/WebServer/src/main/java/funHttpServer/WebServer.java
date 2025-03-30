/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a 
little easier is used. This is done so you see exactly how to pars the request and 
write a response back
*/

package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;

class WebServer {
  public static void main(String args[]) {
    WebServer server = new WebServer(9000);
  }

  /**
   * Main thread
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();

  /**
   * Reads in socket stream and generates a response
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) {

    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
        // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Would theoretically be a file but removed this part, you do not have to do anything with it for the assignment");
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 404 Not Found) Something went wrong: File not found: " + file);
          }
        } else if (request.contains("multiply")) {
          int questionMarkIdx = request.indexOf("?");
          if ((questionMarkIdx == -1) || request.length() <= questionMarkIdx + 1){
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Something went wrong: Missing parameters! You are missing either num1, num2 or both in your request!");
          }else{
          String queryString = request.substring(questionMarkIdx + 1);
          Map<String, String> query_pairs = null;

          try{
            query_pairs = splitQuery(queryString);
          }catch (UnsupportedEncodingException ex){
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Something went wrong: Invalid Request Format! You might be missing characters in your request!");


          }

          if (!query_pairs.containsKey("num1") || !query_pairs.containsKey("num2")){
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Something went wrong: Missing parameters! You are missing either num1, num2 or both in your request!");
          } else {
            try {
              // extract required fields from parameters
              Integer num1 = Integer.parseInt(query_pairs.get("num1"));
              Integer num2 = Integer.parseInt(query_pairs.get("num2"));

              // do math
              Integer result = num1 * num2;

              // Generate response
              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Result is: " + result);
            } catch (NumberFormatException e){
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("(HTTP/1.1 400 Bad Request) Something went wrong: Invalid numbers! 'num1' and 'num2' must be valid numbers!");
            }
          }
        }

      } else if (request.contains("github?")) {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        try {
          query_pairs = splitQuery(request.substring(request.indexOf("github?") + 7));
        } catch (UnsupportedEncodingException ex) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Something went wrong: Invalid Request Format! You might be missing characters in your request!");
            response = builder.toString().getBytes();
            return response;
        }
        String params = query_pairs.get("query");
        if (params == null || params.isEmpty()) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Missing query parameter 'query'.");
            response = builder.toString().getBytes();
            return response;
        }
        String json = fetchURL("https://api.github.com/" + params);
        if (json == null || json.isEmpty()) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 500 Internal Server Error\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 500 Internal Server Error) Could not fetch data from GitHub!");
            response = builder.toString().getBytes();
            return response;
        }
        json = json.trim();
        if (json.startsWith("[")) {
            json = json.substring(1);
        }
        if (json.endsWith("]")) {
            json = json.substring(0, json.length() - 1);
        }
        String[] repos = json.split("\\},\\s*\\{");
        StringBuilder output = new StringBuilder();
        for (String r : repos) {
            if (!r.startsWith("{")) {
                r = "{" + r;
            }
            if (!r.endsWith("}")) {
                r = r + "}";
            }
            String fullName = "";
            int fnIdx = r.indexOf("\"full_name\":");
            if (fnIdx != -1) {
                int start = r.indexOf("\"", fnIdx + 12);
                int end = r.indexOf("\"", start + 1);
                if (start != -1 && end != -1) {
                    fullName = r.substring(start + 1, end);
                }
            }
            String idStr = "";
            int idIdx = r.indexOf("\"id\":");
            if (idIdx != -1) {
                int start = idIdx + 5;
                while (start < r.length() && Character.isWhitespace(r.charAt(start))) {
                    start++;
                }
                int end = start;
                while (end < r.length() && Character.isDigit(r.charAt(end))) {
                    end++;
                }
                idStr = r.substring(start, end);
            }
            String ownerLogin = "";
            int ownerIdx = r.indexOf("\"owner\":");
            if (ownerIdx != -1) {
                int loginIdx = r.indexOf("\"login\":", ownerIdx);
                if (loginIdx != -1) {
                    int start = r.indexOf("\"", loginIdx + 8);
                    int end = r.indexOf("\"", start + 1);
                    if (start != -1 && end != -1) {
                        ownerLogin = r.substring(start + 1, end);
                    }
                }
            }
            output.append("Repo: " + fullName + ", ID: " + idStr + ", Owner: " + ownerLogin + "\n");
        }
        builder = new StringBuilder();
        builder.append("HTTP/1.1 200 OK\n");
        builder.append("Content-Type: text/plain; charset=utf-8\n");
        builder.append("\n");
        builder.append(output.toString());
        response = builder.toString().getBytes();
        

        // Individual Endpoint 1 (Weather)
      } else if (request.contains("weather?")) {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        try {
          query_pairs = splitQuery(request.substring(request.indexOf("weather?") + 8));
        } catch (UnsupportedEncodingException ex){
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Something went wrong: Invalid Request Format! You might be missing characters in your request!");
            response = builder.toString().getBytes();
            return response;
        }
        String params = query_pairs.get("query");
        if (params == null || params.isEmpty()) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Missing query parameter 'query'.");
            response = builder.toString().getBytes();
            return response;
        }
        String[] parts = params.split(",");
        if (parts.length < 2) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Missing lat and/or lon in query parameter. You need to provide the lon and lat of your location!");
            response = builder.toString().getBytes();
            return response;
        }
        double lat = 0, lon = 0;
        try {
          lat = Double.parseDouble(parts[0]);
          lon = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Invalid lat/lon values. They must be numbers!");
            response = builder.toString().getBytes();
            return response;
        }
        // Get the weather data
        String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true";
        String weatherJson = fetchURL(weatherUrl);
        if (weatherJson == null || weatherJson.isEmpty()) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 500 Internal Server Error\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 500 Internal Server Error) Could not fetch weather data.");
            response = builder.toString().getBytes();
            return response;
        }
        // Get temp
        int  weatherIdx= weatherJson.indexOf("\"current_weather\":");
        String tempStr = "";
        if (weatherIdx != -1) {
            int tempIdx = weatherJson.indexOf("\"temperature\":", weatherIdx);
            if (tempIdx != -1) {
                int colonIdx = weatherJson.indexOf(":", tempIdx) + 1;
                int commaIdx = weatherJson.indexOf(",", colonIdx);
                if (commaIdx == -1) {
                  commaIdx = weatherJson.indexOf("}", colonIdx);
                }
                tempStr = weatherJson.substring(colonIdx, commaIdx).trim();
            }
        }
        builder = new StringBuilder();
        builder.append("HTTP/1.1 200 OK\n");
        builder.append("Content-Type: text/plain; charset=utf-8\n");
        builder.append("\n");
        builder.append("Today's temperature: " + tempStr + "°C");
        response = builder.toString().getBytes();
        
        
        // Individual endpoint (BMI)
      } else if (request.contains("bmi?")) {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        try {
            query_pairs = splitQuery(request.substring(request.indexOf("bmi?") + 4));
        } catch (UnsupportedEncodingException ex) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Something went wrong: Invalid Request Format!");
            response = builder.toString().getBytes();
            return response;
        }
        String weightStr = query_pairs.get("weight");
        String heightStr = query_pairs.get("height");
        if (weightStr == null || weightStr.isEmpty() || heightStr == null || heightStr.isEmpty()) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Missing weight and/or height in query parameter. You need to provide your weight and height to calculate your bmi!");;
            response = builder.toString().getBytes();
            return response;
        }
        double weight = 0, height = 0;
        try {
            weight = Double.parseDouble(weightStr);
            height = Double.parseDouble(heightStr);
        } catch (NumberFormatException e) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Invalid number format for 'weight' or 'height'.");
            response = builder.toString().getBytes();
            return response;
        }
        if (height <= 0) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("(HTTP/1.1 400 Bad Request) Height must be more than zero.");
            response = builder.toString().getBytes();
            return response;
        }

        if (weight <= 0) {
          builder = new StringBuilder();
          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/plain; charset=utf-8\n");
          builder.append("\n");
          builder.append("(HTTP/1.1 400 Bad Request) Weight must be more than zero.");
          response = builder.toString().getBytes();
          return response;
      }
        double bmi = weight / (height * height);
        builder = new StringBuilder();
        builder.append("HTTP/1.1 200 OK\n");
        builder.append("Content-Type: text/plain; charset=utf-8\n");
        builder.append("\n");
        builder.append("Your BMI is: " + bmi + "\n");
        builder.append("Make sure you gave your height in meters and weight in kilograms!");
        response = builder.toString().getBytes();

      } else {
        builder = new StringBuilder();
        builder.append("HTTP/1.1 400 Bad Request\n");
        builder.append("Content-Type: text/html; charset=utf-8\n");
        builder.append("\n");
        builder.append("(HTTP/1.1 400 Bad Request) I am not sure what you want me to do...");
        response = builder.toString().getBytes();
      }

        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    String[] pairs = query.split("&");
    for (String pair : pairs) {
        int idx = pair.indexOf("=");
        if (idx == -1) {
            query_pairs.put(URLDecoder.decode(pair, "UTF-8"), "");
        } else {
            String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
            String value = "";
            if (pair.length() > idx + 1) {
                value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
            }
            query_pairs.put(key, value);
        }
    }
    return query_pairs;
}


  /**
   * Builds an HTML file list from the www directory
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte buffer[] = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   *
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   * 
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   *
   **/
  public String fetchURL(String aUrl) {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    try {
      URL url = new URL(aUrl);
      conn = url.openConnection();
      if (conn != null)
        conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
      if (conn != null && conn.getInputStream() != null) {
        in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
        BufferedReader br = new BufferedReader(in);
        if (br != null) {
          int ch;
          // read the next character until end of reader
          while ((ch = br.read()) != -1) {
            sb.append((char) ch);
          }
          br.close();
        }
      }
      in.close();
    } catch (Exception ex) {
      System.out.println("Exception in url request:" + ex.getMessage());
    }
    return sb.toString();
  }
}
