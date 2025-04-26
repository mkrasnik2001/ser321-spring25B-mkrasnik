package org.distrib;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.List;
import java.util.ArrayList;

/**
 * This is a helper class to operate with json messages during communication
 */
public class JsonHelper {

    public static JSONObject createMessage(String header, JSONObject payload){
        JSONObject msg = new JSONObject();
        msg.put("header", header);
        msg.put("payload", payload);
        return msg;
    }

    public static String getHeader(JSONObject message){
        return message.getString("header");
    }

    public static JSONObject getPayload(JSONObject message){
        return message.getJSONObject("payload");
    }

    public static JSONArray convertToJsonArray(List<Integer> ls){
        JSONArray arr = new JSONArray();
        for (int i : ls){
            arr.put(i);
        }
        return arr;
    }

    public static List<Integer> convertFromArray(JSONArray arr){
        List<Integer> ls = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++){
            ls.add(arr.getInt(i));
        }
        return ls;
    }
    
}
