package Assign32starter;

import java.io.File;
import java.io.FileOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class LeaderboardManager {

    private static final String LEADERBOARD_FILE = "leaderboard.xml";
    
    public static void updateLeaderboard(String playerName, int points, int durationSec) {
        // We're assuming 1 correct guess = 10 points
        double correctGuesses = points / 10.0;
        double leaderboardScore = (correctGuesses / durationSec) * 100;

        try {
            File file = new File(LEADERBOARD_FILE);
            Document doc;
            Element root;
            if (file.exists()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.parse(file);
                root = doc.getDocumentElement();
            } else {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.newDocument();
                root = doc.createElement("leaderboard");
                doc.appendChild(root);
            }

            NodeList players = root.getElementsByTagName("player");
            Element playerElement = null;
            for (int i = 0; i < players.getLength(); i++) {
                Element el = (Element) players.item(i);
                if (el.getAttribute("name").equalsIgnoreCase(playerName)) {
                    playerElement = el;
                    break;
                }
            }

            if (playerElement == null) {
                playerElement = doc.createElement("player");
                playerElement.setAttribute("name", playerName);
                playerElement.setAttribute("score", String.valueOf(leaderboardScore));
                root.appendChild(playerElement);
            } else {
                double existingScore = Double.parseDouble(playerElement.getAttribute("score"));
                if (leaderboardScore > existingScore) {
                    playerElement.setAttribute("score", String.valueOf(leaderboardScore));
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            FileOutputStream fos = new FileOutputStream(LEADERBOARD_FILE);
            StreamResult result = new StreamResult(fos);
            transformer.transform(source, result);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
