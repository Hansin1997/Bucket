package cn.dustlight.bucket.core.config;

import cn.dustlight.bucket.other.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * JSON Config
 */
public class Config {

    /**
     * Save this object to file with JSON format.
     *
     * @param path
     * @throws IOException
     */
    public void save(String path) throws IOException {
        Utils.writeJSON(this, new File(path));
    }


    /**
     * Load a JSON format file and cast to Object
     *
     * @param fileName jsonFileName
     * @param tClass   targetClass
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T extends Config> T load(String fileName, Class<T> tClass) throws IOException {
        return (T) Utils.loadFromJSON(new File(fileName), tClass);
    }

    /**
     * Load a JSON format file and cast to Object
     *
     * @param file   file
     * @param tClass targetClass
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T extends Config> T load(File file, Class<T> tClass) throws IOException {
        return (T) Utils.loadFromJSON(file, tClass);
    }

    /**
     * Load a JSON format char string and cast to Object
     *
     * @param json   json
     * @param tClass targetClass
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T extends Config> T loadFromJSON(String json, Class<T> tClass) throws IOException {
        return (T) Utils.loadFromJSON(json, tClass);
    }

    @Override
    public String toString() {
        return Utils.toJSON(this);
    }
}
