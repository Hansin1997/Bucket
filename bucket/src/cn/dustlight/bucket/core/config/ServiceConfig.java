package cn.dustlight.bucket.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Service Configure
 */
public class ServiceConfig extends Config {

    /**
     * service name
     */
    public String name;

    /**
     * service type
     */
    public ServiceType type;

    /**
     * is auto run
     */
    public Boolean autorun;

    /**
     * the root path of service
     */
    public String root;

    /**
     * service target path
     */
    public String path;

    /**
     * bind host
     */
    public String host;

    /**
     * bind port
     */
    public Integer port;

    /**
     * extend params
     */
    public Map<String, Object> param;

    /**
     * methods
     */
    public Map<String, ServiceMethodBody> methods;

    /**
     * Add param
     *
     * @param key
     * @param value
     * @return
     */
    public ServiceConfig setParam(String key, Object value) {
        if (param == null)
            param = new HashMap<>();
        param.put(key, value);
        return this;
    }

    /**
     * Get param
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> T getParam(String key) {
        Object obj;
        if (param == null || (obj = param.get(key)) == null)
            return null;
        return (T) obj;
    }

    /**
     * Add method
     *
     * @param name       methodName
     * @param methodBody methodBody
     * @return
     */
    public ServiceConfig setMethod(String name, ServiceMethodBody methodBody) {
        if (methods == null)
            methods = new HashMap<>();
        methods.put(name, methodBody);
        return this;
    }

    /**
     * Method Body
     */
    public static class ServiceMethodBody {

        /**
         * request type
         */
        public MethodType method;

        /**
         * request path
         */
        public String path;

        /**
         * extend params
         */
        public Map<String, ServiceMethodParameterBody> param;

        public ServiceMethodBody() {
            this(MethodType.POST, "");
        }

        public ServiceMethodBody(MethodType method, String path) {
            this.method = method;
            this.path = path;
        }

        /**
         * Add param
         *
         * @param key
         * @param value
         * @return
         */
        public ServiceMethodBody setParam(String key, ServiceMethodParameterBody value) {
            if (param == null)
                param = new HashMap<>();
            param.put(key, value);
            return this;
        }

        /**
         * Method Parameter Body
         */
        public static class ServiceMethodParameterBody {

            /**
             * parameter type
             */
            public String type;

            /**
             * parameter description
             */
            public String description;

            /**
             * is array
             */
            public Boolean array;

            public ServiceMethodParameterBody() {
                this("Object", "Unknow", false);
            }

            public ServiceMethodParameterBody(String type, String description, Boolean array) {
                this.type = type;
                this.description = description;
                this.array = array;
            }

        }

        /**
         * Method Types
         */
        public enum MethodType {
            GET,
            HEAD,
            POST,
            OPTIONS,
            PUT,
            DELETE,
            TRACE,
            CONNECT
        }

    }

    /**
     * Service Types
     */
    public enum ServiceType {
        /**
         * SimpleHttpService
         */
        HTTP,
        /**
         * Jar
         */
        JAVA_JAR,
        /**
         * Java Class File
         */
        JAVA_CLASS,
        /**
         * Java Source File
         */
        JAVA_FILE,
        /**
         * PHP Script
         */
        PHP,
        /**
         * Python Script
         */
        PYTHON_FILE,
        /**
         * System Executable File
         */
        EXECUTABLE
    }
}

