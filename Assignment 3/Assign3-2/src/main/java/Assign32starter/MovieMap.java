package Assign32starter;
import java.util.*;
import org.json.*;

public class MovieMap {

   public static final List<String> movieOrder = Arrays.asList(
       "the dark knight",
       "back to the future",
       "lord of the rings",
       "the lion king",
       "jurassic park"
   );

   public static final Map<String, JSONObject> movieMap = new HashMap<>();

   static {
       JSONObject darkKnight = new JSONObject();
       darkKnight.put("answered", false);
       List<JSONObject> darkKnightImages = new ArrayList<>();
       
       JSONObject dklvl1 = new JSONObject();
       dklvl1.put("filename", "img/TheDarkKnight1.png");
       dklvl1.put("imageLevel", 1);
       darkKnightImages.add(dklvl1);
       
       JSONObject dklvl2 = new JSONObject();
       dklvl2.put("filename", "img/TheDarkKnight2.png");
       dklvl2.put("imageLevel", 2);
       darkKnightImages.add(dklvl2);
       
       JSONObject dklvl3 = new JSONObject();
       dklvl3.put("filename", "img/TheDarkKnight3.png");
       dklvl3.put("imageLevel", 3);
       darkKnightImages.add(dklvl3);
       
       JSONObject dklvl4 = new JSONObject();
       dklvl4.put("filename", "img/TheDarkKnight4.png");
       dklvl4.put("imageLevel", 4);
       darkKnightImages.add(dklvl4);
       
       darkKnight.put("images", darkKnightImages);
       movieMap.put("the dark knight", darkKnight);
       
       JSONObject backToTheFuture = new JSONObject();
       backToTheFuture.put("answered", false);
       List<JSONObject> backToTheFutureImages = new ArrayList<>();
       
       JSONObject btflvl1 = new JSONObject();
       btflvl1.put("filename", "img/BackToTheFuture1.png");
       btflvl1.put("imageLevel", 1);
       backToTheFutureImages.add(btflvl1);
       
       JSONObject btflvl2 = new JSONObject();
       btflvl2.put("filename", "img/BackToTheFuture2.png");
       btflvl2.put("imageLevel", 2);
       backToTheFutureImages.add(btflvl2);
       
       JSONObject btflvl3 = new JSONObject();
       btflvl3.put("filename", "img/BackToTheFuture3.png");
       btflvl3.put("imageLevel", 3);
       backToTheFutureImages.add(btflvl3);
       
       JSONObject btflvl4 = new JSONObject();
       btflvl4.put("filename", "img/BackToTheFuture4.png");
       btflvl4.put("imageLevel", 4);
       backToTheFutureImages.add(btflvl4);
       
       backToTheFuture.put("images", backToTheFutureImages);
       movieMap.put("back to the future", backToTheFuture);
       
       JSONObject lordOfTheRings = new JSONObject();
       lordOfTheRings.put("answered", false);
       List<JSONObject> lordOfTheRingsImages = new ArrayList<>();
       
       JSONObject lotrlvl1 = new JSONObject();
       lotrlvl1.put("filename", "img/LordOfTheRings1.png");
       lotrlvl1.put("imageLevel", 1);
       lordOfTheRingsImages.add(lotrlvl1);
       
       JSONObject lotrlvl2 = new JSONObject();
       lotrlvl2.put("filename", "img/LordOfTheRings2.png");
       lotrlvl2.put("imageLevel", 2);
       lordOfTheRingsImages.add(lotrlvl2);
       
       JSONObject lotrlvl3 = new JSONObject();
       lotrlvl3.put("filename", "img/LordOfTheRings3.png");
       lotrlvl3.put("imageLevel", 3);
       lordOfTheRingsImages.add(lotrlvl3);
       
       JSONObject lotrlvl4 = new JSONObject();
       lotrlvl4.put("filename", "img/LordOfTheRings4.png");
       lotrlvl4.put("imageLevel", 4);
       lordOfTheRingsImages.add(lotrlvl4);
       
       lordOfTheRings.put("images", lordOfTheRingsImages);
       movieMap.put("lord of the rings", lordOfTheRings);
       
       JSONObject theLionKing = new JSONObject();
       theLionKing.put("answered", false);
       List<JSONObject> theLionKingImages = new ArrayList<>();
       
       JSONObject tlklvl1 = new JSONObject();
       tlklvl1.put("filename", "img/TheLionKing1.png");
       tlklvl1.put("imageLevel", 1);
       theLionKingImages.add(tlklvl1);
       
       JSONObject tlklvl2 = new JSONObject();
       tlklvl2.put("filename", "img/TheLionKing2.png");
       tlklvl2.put("imageLevel", 2);
       theLionKingImages.add(tlklvl2);
       
       JSONObject tlklvl3 = new JSONObject();
       tlklvl3.put("filename", "img/TheLionKing3.png");
       tlklvl3.put("imageLevel", 3);
       theLionKingImages.add(tlklvl3);
       
       JSONObject tlklvl4 = new JSONObject();
       tlklvl4.put("filename", "img/TheLionKing4.png");
       tlklvl4.put("imageLevel", 4);
       theLionKingImages.add(tlklvl4);
       
       theLionKing.put("images", theLionKingImages);
       movieMap.put("the lion king", theLionKing);
       
       JSONObject jurassicPark = new JSONObject();
       jurassicPark.put("answered", false);
       List<JSONObject> jurassicParkImages = new ArrayList<>();
       
       JSONObject jplvl1 = new JSONObject();
       jplvl1.put("filename", "img/JurassicPark1.png");
       jplvl1.put("imageLevel", 1);
       jurassicParkImages.add(jplvl1);
       
       JSONObject jplvl2 = new JSONObject();
       jplvl2.put("filename", "img/JurassicPark2.png");
       jplvl2.put("imageLevel", 2);
       jurassicParkImages.add(jplvl2);
       
       JSONObject jplvl3 = new JSONObject();
       jplvl3.put("filename", "img/JurassicPark3.png");
       jplvl3.put("imageLevel", 3);
       jurassicParkImages.add(jplvl3);
       
       JSONObject jplvl4 = new JSONObject();
       jplvl4.put("filename", "img/JurassicPark4.png");
       jplvl4.put("imageLevel", 4);
       jurassicParkImages.add(jplvl4);
       
       jurassicPark.put("images", jurassicParkImages);
       movieMap.put("jurassic park", jurassicPark);
   }
}
