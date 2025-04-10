package Assign32starter;

import java.awt.BorderLayout;
import java.io.File;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public class LeaderboardPanel extends JPanel {

    public LeaderboardPanel(Runnable backToMenuCallback) {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Leaderboard", SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);
        
        String[] columnNames = {"Player", "Score"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        
        try {
            File xmlFile = new File("leaderboard.xml");
            if (xmlFile.exists()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(xmlFile);
                doc.getDocumentElement().normalize();
                
                NodeList nList = doc.getElementsByTagName("player");
                for (int i = 0; i < nList.getLength(); i++) {
                    Element playerElement = (Element) nList.item(i);
                    String name = playerElement.getAttribute("name");
                    String score = playerElement.getAttribute("score");
                    model.addRow(new Object[] { name, score });
                }
            } else {
                model.addRow(new Object[] { "N/A", "N/A" });
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addRow(new Object[] { "Error", "Error" });
        }
        
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        JButton backButton = new JButton("Go Back");
        backButton.addActionListener(e -> {
            if (backToMenuCallback != null) {
                backToMenuCallback.run();
            }
        });
        add(backButton, BorderLayout.SOUTH);
    }
    
    public LeaderboardPanel() {
        this(null);
    }

    
}
