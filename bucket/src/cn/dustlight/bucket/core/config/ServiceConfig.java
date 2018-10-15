package cn.dustlight.bucket.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务配置类
 */
public class ServiceConfig extends Config {

    /**
     * 服务名称
     */
    public String name;

    /**
     * 服务类型
     */
    public ServiceType type;

    /**
     * 是否自启动
     */
    public Boolean autorun;

    /**
     * 服务所在目录（由容器设置）
     */
    public String root;

    /**
     * 服务路径
     */
    public String path;

    /**
     * 服务绑定地址
     */
    public String host;

    /**
     * 服务绑定端口
     */
    public Integer port;

    /**
     * 额外参数
     */
    public Map<String, Object> param;

    /**
     * 服务具有的方法
     */
    public Map<String, ServiceMethodBody> methods;

    /**
     * 设置参数
     *
     * @param key   参数名
     * @param value 参数值
     * @return 配置对象
     */
    public ServiceConfig setParam(String key, Object value) {
        if (param == null)
            param = new HashMap<>();
        param.put(key, value);
        return this;
    }

    /**
     * 获取参数
     *
     * @param key 参数名
     * @param <T> 参数类型
     * @return 参数
     */
    public <T> T getParam(String key) {
        Object obj;
        if (param == null || (obj = param.get(key)) == null)
            return null;
        return (T) obj;
    }

    public ServiceConfig setMethod(String name, ServiceMethodBody methodBody) {
        if (methods == null)
            methods = new HashMap<>();
        methods.put(name, methodBody);
        return this;
    }

    /**
     * 服务方法体
     */
    public static class ServiceMethodBody {

        /**
         * 请求方法类型
         */
        public MethodType method;

        /**
         * 请求路径
         */
        public String path;

        /**
         * 携带参数
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
         * 设置参数
         *
         * @param key   参数名
         * @param value 参数值
         * @return 方法体
         */
        public ServiceMethodBody setParam(String key, ServiceMethodParameterBody value) {
            if (param == null)
                param = new HashMap<>();
            param.put(key, value);
            return this;
        }

        /**
         * 方法参数结构体
         */
        public static class ServiceMethodParameterBody {

            /**
             * 参数类型
             */
            public String type;

            /**
             * 参数描述
             */
            public String description;

            /**
             * 是否数组
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
         * 方法类型枚举
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
     * 服务类型
     */
    public enum ServiceType {
        /**
         * 简易Http服务
         */
        HTTP,
        /**
         * Jar包
         */
        JAVA_JAR,
        /**
         * Java编译文件
         */
        JAVA_CLASS,
        /**
         * Java源文件
         */
        JAVA_FILE,
        /**
         * PHP文件
         */
        PHP,
        /**
         * Python文件
         */
        PYTHON_FILE,
        /**
         * 系统可执行文件
         */
        EXECUTABLE
    }
}

