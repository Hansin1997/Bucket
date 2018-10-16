package cn.dustlight.bucket.core.config;

import cn.dustlight.bucket.core.exception.ServiceException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bucket Configure
 */
public class BucketConfig extends Config {

    /**
     * Scanning paths
     */
    public List<String> roots;

    /**
     * Bucket Wrappers
     */
    public Map<String,Map<String,Object>> wrappers;

    public <T> T getWrapper(String name) {
        Object obj;
        if(wrappers == null || (obj = wrappers.get(name)) == null)
            return null;
        return (T) obj;
    }

    public Map<String, Map<String,Object>> getWrappers() {
        return wrappers;
    }

    public Map<String, ServiceConfig> getServiceConfigs() throws ServiceException {
        Map<String, ServiceConfig> result = new HashMap<>();
        if (roots != null)
            for (String root : roots) {
                File parent = new File(root);
                File[] children = parent.listFiles();
                if (children == null)
                    continue;
                for (File child : children) {

                    if (child.isDirectory()) {
                        File configFile = new File(child, "service.json");
                        if (configFile.exists()) {
                            try {
                                ServiceConfig sc = ServiceConfig.load(configFile, ServiceConfig.class);
                                if(sc.root == null)
                                    sc.root = child.getAbsolutePath();
                                if (sc != null) {
                                    if (result.get(sc.name) != null)
                                        throw new ServiceException(-200, "Load service error,cause s_name conflict: " + sc.name);
                                    result.put(sc.name, sc);
                                }

                            } catch (IOException e) {
                                // service.json不存在
                            }
                        }
                    }
                }
            }

        return result;
    }


}
