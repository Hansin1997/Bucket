package cn.dustlight.bucket.wrappers;

import cn.dustlight.bucket.core.Bucket;
import cn.dustlight.bucket.core.BucketWrapper;
import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.ServiceCalling;
import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.Config;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.Utils;
import cn.dustlight.bucket.other.ZooKeeperUtils;
import com.google.gson.JsonObject;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zookeeper包装类
 */
public class ZooKeeperWrapper extends BucketWrapper implements Watcher {

    public String host;
    public Integer port;
    public Integer session;

    private ZooKeeper zoo;
    private static final String ROOT = "/Bucket", INSTANCE = "/INSTANCE", MAP = "/MAP", SERVICES = "/SERVICES", RPC = "/RPC";
    private String rootPath, instancePath, mapPath, servicePath, rpcPath;
    private String instanceName;

    private List<String> lastRpc;

    public ZooKeeperWrapper(Bucket bucket) {
        super(bucket);

    }

    @Override
    public void initialize(BucketConfig config) {
        super.initialize(config);
        lastRpc = new ArrayList<>();
        try {
            zoo = new ZooKeeper(ZooKeeperWrapper.this.host + ":" + ZooKeeperWrapper.this.port, ZooKeeperWrapper.this.session, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    try {
                        init();
                    } catch (Exception e) {
                        onException(e);
                    }
                }
            });
        } catch (IOException e) {
            onException(e);
        }
    }

    @Override
    public RemoteService startService(String name, boolean reload) {
        RemoteService remoteService = new RemoteService(name);
        if(reload)
            remoteService.setReload(reload);
        remoteService.start();
        return remoteService;
    }

    @Override
    public RemoteService getService(String name) {
        return new RemoteService(name);
    }

    @Override
    public RemoteService stopService(String name) {
        RemoteService remoteService = new RemoteService(name);
        remoteService.stop();
        return remoteService;
    }

    @Override
    public Map<String, ServiceConfig> getServiceConfigs() {
        try {
            List<String> ps = zoo.getChildren(mapPath, false);
            Map<String, ServiceConfig> map = new HashMap<>();
            for (String str : ps) {
                try {
                    byte[] data = zoo.getData(mapPath + "/" + str, null, null);
                    ServiceConfig serviceConfig = Config.loadFromJSON(new String(data), ServiceConfig.class);
                    map.put(str, serviceConfig);
                } catch (Exception e) {
                    onException(e);
                }
            }
            return map;
        } catch (Exception e) {
            throw new ServiceException(-500, "ZooKeeper Exception: " + e);
        }
    }

    @Override
    public RemoteServiceConfig getServiceConfig(String name) {
        RemoteServiceConfig config = new RemoteServiceConfig(name);
        config.getConfigs();
        return config;
    }

    @Override
    public void destory() {
        super.destory();
        try {
            zoo.close();
        } catch (InterruptedException e) {
            onException(e);
        }
    }

    protected void onException(Exception e) {
        e.printStackTrace();
    }

    protected String createNode(String path, byte[] data, List<ACL> acls, CreateMode createMode) throws KeeperException, InterruptedException {
        return ZooKeeperUtils.creat(zoo, data, path, acls, createMode);
    }

    protected void init() throws Exception {

        rootPath = createNode(ROOT, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        instancePath = rootPath + INSTANCE;
        servicePath = rootPath + SERVICES;
        mapPath = rootPath + MAP;
        rpcPath = rootPath + RPC;

        instancePath = createNode(instancePath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        servicePath = createNode(servicePath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        rpcPath = createNode(rpcPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        mapPath = createNode(mapPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        String json = Utils.toJSON(ZooKeeperWrapper.this.getConfig());
        instanceName = InetAddress.getLocalHost().getHostName() + ":" + zoo.getSessionId();
        String bucketRoot = instancePath + "/" + instanceName;
        createNode(bucketRoot, json.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

        zoo.getChildren(rpcPath, this);

        Map<String, ServiceConfig> map = ZooKeeperWrapper.this.bucket.getServiceConfigs();
        for (ServiceConfig serviceConfig : map.values()) {
            if (serviceConfig == null)
                continue;
            String js = Utils.toJSON(serviceConfig);
            createNode(mapPath + "/" + serviceConfig.name + "_" + instanceName, js.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        synchronized (lastRpc) {
            try {
                if (watchedEvent.getPath().startsWith(rpcPath)) {
                    List<String> list = zoo.getChildren(rpcPath, this);
                    byte[] data;
                    for (String l : list) {
                        try {
                            String p = rpcPath + "/" + l;
                            data = zoo.getData(p, false, null);
                            RpcBody rpcBody = Utils.loadFromJSON(new String(data), RpcBody.class);
                            switch (rpcBody.type) {
                                case START_SERVICE:
                                    String n = rpcBody.getParam("name");
                                    Boolean reload = rpcBody.getParam("reload");
                                    reload = (reload != null)?reload:false;
                                    Service service = this.bucket.startService(n,reload);
                                    if(service != null){
                                        zoo.setData(p,Utils.toJSON(service.getConfig()).getBytes(),-1);
                                    }
                                    break;

                            }
                        } catch (ServiceException e){

                        } catch (Exception e) {
                            onException(e);
                        }


                    }
                    lastRpc.clear();
                    lastRpc.addAll(list);
                }
            } catch (Exception e) {
                onException(e);
            }
        }
    }

    protected void RPC(RpcBody rpcBody, RpcCallback callback) {
        RpcResponse response = new RpcResponse();
        try {
            String ph = createNode(rpcPath + "/" + instanceName + "_", rpcBody.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            zoo.getData(ph, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {

                    try {
                        byte[] data = zoo.getData(ph, false, null);
                        zoo.delete(ph, -1);
                        response.data = Utils.loadFromJSON(new String(data), JsonObject.class);
                    } catch (Exception e) {
                        response.throwable = e;
                    }
                    callback.onDone(response);
                }
            }, null);
        } catch (Exception e) {
            response.throwable = e;
            callback.onDone(response);
        }

    }


    public class RemoteServiceConfig extends ServiceConfig {

        public String s_name;
        public Map<String, ServiceConfig> configs;

        public RemoteServiceConfig(String s_name) {
            this.s_name = s_name;
        }

        public Map<String, ServiceConfig> getConfigs() {
            try {
                List<String> ps = zoo.getChildren(mapPath, false);
                Map<String, ServiceConfig> map = new HashMap<>();
                for (String str : ps) {
                    try {
                        if (!str.startsWith(this.s_name + "_"))
                            continue;
                        byte[] data = zoo.getData(mapPath + "/" + str, null, null);
                        ServiceConfig serviceConfig = Config.loadFromJSON(new String(data), ServiceConfig.class);
                        map.put(str, serviceConfig);
                    } catch (Exception e) {
                        onException(e);
                    }
                }
                this.configs = map;
                return map;
            } catch (Exception e) {
                throw new ServiceException(-500, "ZooKeeper Exception: " + e);
            }
        }

    }


    /**
     * 远程服务
     */
    public class RemoteService extends Service implements Watcher {

        public String s_name;
        public RemoteServiceConfig config;

        private Boolean reload;

        public RemoteService(String s_name) {
            this.s_name = s_name;
            config = new RemoteServiceConfig(s_name);
        }

        @Override
        protected void doInit(ServiceConfig config) throws ServiceException {

        }

        @Override
        protected void doStart(ServiceConfig config) throws ServiceException {

            RpcBody body = new RpcBody()
                    .setParams("name", s_name)
                    .setParams("reload",reload)
                    .setType(RpcBody.RpcType.START_SERVICE);

            ZooKeeperWrapper.this.RPC(body, new RpcCallback() {
                @Override
                public void onDone(RpcResponse rpcResponse) {
                    System.out.println(rpcResponse);
                }
            });
        }

        @Override
        protected void doStop() throws ServiceException {

        }

        @Override
        public void resetConfig(ServiceConfig config) throws ServiceException {

        }

        @Override
        public Object call(ServiceCalling calling) {
            return null;
        }

        @Override
        public void process(WatchedEvent watchedEvent) {

        }

        public void setReload(Boolean reload) {
            this.reload = reload;
        }

        @Override
        public RemoteServiceConfig getConfig() {
            config.getConfigs();
            return this.config;
        }
    }

    public static class RpcResponse {
        public JsonObject data;
        public Throwable throwable;

        @Override
        public String toString() {
            return Utils.toJSON(this);
        }
    }

    public interface RpcCallback {
        void onDone(RpcResponse rpcResponse);
    }

    public static class RpcBody {

        public RpcType type;
        public Map<String, Object> params;

        private ZooKeeperWrapper zooKeeperWrapper;

        public RpcBody() {
            type = RpcType.NONE;
            params = new HashMap<>();
        }

        public void setZooKeeperWrapper(ZooKeeperWrapper zooKeeperWrapper) {
            this.zooKeeperWrapper = zooKeeperWrapper;
        }

        public <T> T getParam(String key) {
            Object obj;
            if (params == null || (obj = params.get(key)) == null)
                return null;
            return (T) obj;
        }

        public RpcBody setParams(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public RpcBody setParams(Map<String, Object> params) {
            params.putAll(params);
            return this;
        }

        public RpcBody setType(RpcType type) {
            this.type = type;
            return this;
        }

        public enum RpcType {
            NONE,
            START_SERVICE,
            STOP_SERVICE,
            CALL
        }

        public byte[] getBytes() {
            return Utils.toJSON(this).getBytes();
        }

    }
}
