package cn.dustlight.bucket.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Service method calling
 */
public class ServiceCalling {

    /**
     * calling method
     */
    private String method;

    /**
     * parameters
     */
    private Map<String, Object> params;

    public ServiceCalling(String method) {
        this.method = method;
        params = new HashMap<>();
    }

    /**
     * Add parameter
     *
     * @param key
     * @param value
     * @return
     */
    public ServiceCalling setParam(String key, Object value) {
        params.put(key, value);
        return this;
    }

    /**
     * Get parameters
     *
     * @return
     */
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * Get method name
     *
     * @return
     */
    public String getMethod() {
        return method;
    }

    /**
     * Get parameter
     *
     * @param key
     * @param defaultValue
     * @param <T>
     * @return
     */
    public <T> T getParam(String key, T defaultValue) {
        if (params == null)
            return defaultValue;
        Object obj = params.get(key);
        if (obj == null)
            return defaultValue;
        return (T) obj;
    }

    /**
     * Get parameters
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> T getParam(String key) {
        return getParam(key, null);
    }
}
