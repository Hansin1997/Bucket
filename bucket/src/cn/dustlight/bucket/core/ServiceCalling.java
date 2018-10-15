package cn.dustlight.bucket.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务调用类
 */
public class ServiceCalling {

    private String method;
    private Map<String,Object> params;

    public ServiceCalling(String method){
        this.method = method;
        params = new HashMap<>();
    }

    public ServiceCalling setParam(String key,Object value){
        params.put(key,value);
        return this;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String getMethod() {
        return method;
    }

    public <T> T getParam(String key,T defaultValue) {
        if(params == null)
            return defaultValue;
        Object obj = params.get(key);
        if(obj == null)
            return defaultValue;
        return (T) obj;
    }

    public <T> T getParam(String key) {
        return getParam(key,null);
    }
}
