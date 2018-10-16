package cn.dustlight.bucket.other;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * Common Utils
 */
public class Utils {

    /**
     * Transform object to JSON
     *
     * @param object Object
     * @return JSON
     */
    public static String toJSON(Object object) {
        return new Gson().toJson(object);
    }

    public static <T> T loadFromJSON(File file, Class<T> tClass) throws IOException, JsonSyntaxException, JsonIOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        T obj = new Gson().fromJson(reader,tClass);
        reader.close();
        return obj;
    }

    public static <T> T loadFromJSON(String json,Class<T> tClass) throws JsonSyntaxException, JsonIOException {
        return new Gson().fromJson(json,tClass);
    }

    public static void writeJSON(Object obj,OutputStream outputStream) throws IOException {
        byte[] data = toJSON(obj).getBytes();
        outputStream.write(data);
        outputStream.flush();
    }

    public static  void writeJSON(Object obj,File file) throws IOException {
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
        writeJSON(obj,outputStream);
        outputStream.close();
    }

    public static String QueryCharset = "UTF-8";

    public static Map<String, String> QueryDecode(String queryString) throws UnsupportedEncodingException {
        Map<String,String> map = new HashMap<>();
        String key,value;
        if(queryString != null){
            String[] strings = queryString.split("&");
            for(String string : strings) {
                String[] kv = string.split("=",2);
                if(kv.length != 2)
                    continue;
                key = URLDecoder.decode(kv[0],QueryCharset);
                value = URLDecoder.decode(kv[1],QueryCharset);
                map.put(key,value);
            }
        }
        return map;
    }

    public static String QueryEncode(Map<String,String> query) throws UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        Set<Map.Entry<String, String>> set = query.entrySet();
        int i = 0;
        for(Map.Entry<String, String> kv : set) {

            stringBuilder.append(URLEncoder.encode(kv.getKey(),QueryCharset))
                    .append('=')
                    .append(URLEncoder.encode(kv.getValue(),QueryCharset));
            if(i < set.size() - 1)
                stringBuilder.append('&');
            i++;
        }
        return stringBuilder.toString();
    }
}
