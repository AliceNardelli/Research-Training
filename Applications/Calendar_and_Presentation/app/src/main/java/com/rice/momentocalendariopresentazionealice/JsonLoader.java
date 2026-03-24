package com.rice.momentocalendariopresentazionealice;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

public class JsonLoader {
    public Map<String, Object> loadJSONFromAsset(Context context, String fileName) {
        Map<String, Object> jsonMap = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            InputStreamReader reader = new InputStreamReader(is);
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            jsonMap = new Gson().fromJson(reader, type);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMap;
    }
}
