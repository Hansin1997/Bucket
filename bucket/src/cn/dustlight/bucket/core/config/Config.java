package cn.dustlight.bucket.core.config;

import cn.dustlight.bucket.other.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * JSON配置类
 */
public class Config {

    /**
     * 保存
     *
     * @param path
     * @throws IOException
     */
    public void save(String path) throws IOException {
        Utils.writeJSON(this, new File(path));
    }


    /**
     * 加载
     *
     * @param fileName 文件名
     * @param tClass   实例类名
     * @param <T>      子类
     * @return
     * @throws IOException
     */
    public static <T extends Config> T load(String fileName, Class<T> tClass) throws IOException {
        return (T) Utils.loadFromJSON(new File(fileName), tClass);
    }

    /**
     * 加载
     *
     * @param file 文件
     * @param tClass   实例类名
     * @param <T>      子类
     * @return
     * @throws IOException
     */
    public static <T extends Config> T load(File file, Class<T> tClass) throws IOException {
        return (T) Utils.loadFromJSON(file, tClass);
    }

    /**
     * 加载
     *
     * @param json json
     * @param tClass   实例类名
     * @param <T>      子类
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
