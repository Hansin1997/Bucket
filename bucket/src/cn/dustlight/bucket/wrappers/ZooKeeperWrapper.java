package cn.dustlight.bucket.wrappers;

import cn.dustlight.bucket.core.*;
import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.Config;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.other.Utils;
import cn.dustlight.bucket.other.ZooKeeperUtils;
import com.google.gson.JsonObject;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

/**
 * Zookeeper Wrapper
 * this wrapper ..
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
    public CommonFuture<Bucket> initialize(BucketConfig config) {
        return super.initialize(config).addListener((result, e) -> {
            if(e != null)
                onException(e);
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
            } catch (IOException exc) {
                onException(exc);
            }
        });

    }

    @Override
    public CommonFuture<RemoteService> startService(String name, boolean reload) {
        RemoteService remoteService = new RemoteService(name);
        if(reload)
            remoteService.setReload(true);
        return remoteService.start();
    }

    @Override
    public RemoteService getService(String name) {
        return new RemoteService(name);
    }

    @Override
    public CommonFuture<RemoteService> stopService(String name) {
        RemoteService remoteService = new RemoteService(name);
        return remoteService.stop();
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
        return new RemoteServiceConfig(name).loadConfigs();
    }

    @Override
    public CommonFuture<Bucket> destroy() {
        return super.destroy().addListener((result, e1) -> {
            if(e1 != null)
                onException(e1);
            try {
                zoo.close();
            } catch (InterruptedException e) {
                onException(e);
            }
        });

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
                        if(lastRpc.contains(l))
                            continue;
                        try {
                            String p = rpcPath + "/" + l;
                            data = zoo.getData(p, false, null);
                            if(data == null)
                                continue;
                            RpcBody rpcBody = Utils.loadFromJSON(new String(data), RpcBody.class);
                            if(rpcBody.instances != null && !rpcBody.instances.contains(instanceName))
                                continue;
                            if(rpcBody == null || rpcBody.type == null)
                                continue;
                            switch (rpcBody.type) {
                                case START_SERVICE:
                                    String n = rpcBody.getParam("name");
                                    Boolean reload = rpcBody.getParam("reload");
                                    reload = (reload != null)?reload:false;

                                    CommonFuture<Service> future = this.bucket.startService(n,reload);

                                    future.addListener((service, e1) -> {
                                        if(e1 != null)
                                            onException(e1);
                                        if(service != null){
                                            try {
                                                RpcResponse response = new RpcResponse();
                                                response.instance = instanceName;
                                                response.data = Utils.toJsonObject(service.getConfig());
                                                ZooKeeperWrapper.this.zoo.setData(p,response.getBytes(),-1);
                                                ZooKeeperWrapper.this.createNode(servicePath + "/" + n +"_"+instanceName,new byte[0],ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL);
                                                ZooKeeperWrapper.this.zoo.setData(mapPath + "/" + n +"_" + instanceName,service.getConfig().toString().getBytes(),-1);
                                            } catch (Exception e){
                                                onException(e);
                                            }
                                        }
                                    });

                                    break;

                            }
                        } catch (ServiceException | KeeperException.NoNodeException e){
                            e.printStackTrace();
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

    /**
     * Remote Procedure Call
     *
     * @param rpcBody RPC Body
     * @return
     */
    protected CommonFuture<RpcResponse> RPC(RpcBody rpcBody) {
        RpcFuture future = new RpcFuture(rpcBody);
        return future.start();
    }

    /**
     * Configure of Remote Virtual Service
     */
    public class RemoteServiceConfig extends ServiceConfig {

        /**
         * this config's owner(if configs size == 1)
         */
        public String instance;

        /**
         * map of instance and config(if size > 1)
         */
        public Map<String, ServiceConfig> configs;

        public RemoteServiceConfig(String serviceName){
            this.name = serviceName;
        }

        /**
         * Load data from remote Bucket
         *
         * @param instances Bucket instance names
         * @return
         */
        public RemoteServiceConfig loadConfigs(List<String> instances) {
            try {
                List<String> ps = zoo.getChildren(mapPath, false);
                if(ps.size() > 0){
                    Map<String, ServiceConfig> map = new HashMap<>();
                    for (String str : ps) {
                        try {
                            if (!str.startsWith(this.name + "_"))
                                continue;
                            String ins = str.substring(this.name.length() + 1);
                            if(instances != null && !instances.contains(ins))
                                continue;
                            byte[] data = zoo.getData(mapPath + "/" + str, null, null);
                            ServiceConfig serviceConfig = Config.loadFromJSON(new String(data), ServiceConfig.class);
                            map.put(str.substring(this.name.length() + 1), serviceConfig);
                        } catch (Exception e) {
                            onException(e);
                        }
                    }
                    if(map.size() > 1)
                        this.configs = map;
                    else if(map.size() == 1){
                        Map.Entry<String, ServiceConfig> kv = map.entrySet().iterator().next();
                        this.instance = kv.getKey();
                        this.root = kv.getValue().root;
                        this.path = kv.getValue().path;
                        this.host = kv.getValue().host;
                        this.port = kv.getValue().port;
                        this.type = kv.getValue().type;
                        this.param = kv.getValue().param;
                        this.methods = kv.getValue().methods;
                        this.autorun = kv.getValue().autorun;
                    }
                }
            } catch (Exception e) {
                ServiceException se = new ServiceException(-500, "ZooKeeper Exception: " + e);
                se.addSuppressed(e);
                throw se;
            }
            return this;
        }

        public RemoteServiceConfig loadConfigs(){
            return loadConfigs(null);
        }

        public boolean exists() {
            return instance != null || configs != null ;
        }

        public Set<String> getInstances(){
            HashSet<String> set = new HashSet<String>();
            if(instance != null)
                set.add(instance);
            else if(configs != null)
                set.addAll(configs.keySet());
            return set;
        }
    }

    /**
     * Remote Virtual Service
     */
    public class RemoteService extends Service {

        private Boolean reload;
        private List<String> instances;

        public RemoteService(String serviceName,List<String> instances) {
            RemoteServiceConfig config = new RemoteServiceConfig(serviceName);
            this.config = config;
            this.instances = instances;
        }

        public RemoteService(String serviceName,String instance) {
            this(serviceName,new ArrayList<String>());
            instances.add(instance);
        }

        public RemoteService(String serviceName){
            RemoteServiceConfig config = new RemoteServiceConfig(serviceName);
            this.config = config;
        }

        @Override
        protected CommonFuture<RemoteService> doInit(ServiceConfig config) throws ServiceException {
            return new CommonFuture<RemoteService>() {
                @Override
                public void run() {
                    done(RemoteService.this);
                }
            };
        }

        @Override
        protected CommonFuture<RemoteService> doStart(ServiceConfig c) throws ServiceException {
            RemoteServiceConfig config = getConfig().loadConfigs(instances);
            if(!config.exists())
                throw new ServiceException(100, "Service doesn't exist.");
            RpcBody body = new RpcBody()
                    .setParams("name", config.name)
                    .setParams("reload",reload)
                    .addInstances(config.getInstances())
                    .setType(RpcBody.RpcType.START_SERVICE);
            return new CommonFuture<RemoteService>() {
                @Override
                public void run() {
                    ZooKeeperWrapper.this.RPC(body).addListener((result, e) -> {
                        if(result.data != null){
                            RemoteService remoteService = new RemoteService(config.name,config.instance);
                            remoteService.getConfig().loadConfigs();
                            done(remoteService,e);
                        }else{
                            done(new ServiceException(-500,"RemoteService doStart Error:" + result.throwable));
                        }
                    });
                }
            };
        }

        @Override
        protected CommonFuture<RemoteService> doStop() throws ServiceException {
            RemoteServiceConfig config = getConfig().loadConfigs(instances);
            if(!config.exists())
                throw new ServiceException(100, "Service doesn't exist.");

            RpcBody body = new RpcBody()
                    .setParams("name", config.name)
                    .addInstances(config.getInstances())
                    .setType(RpcBody.RpcType.STOP_SERVICE);
            return new CommonFuture<RemoteService>() {
                @Override
                public void run() {
                    ZooKeeperWrapper.this.RPC(body).addListener((result, e) -> {
                        done(RemoteService.this,e);
                    });
                }
            };
        }

        @Override
        public void resetConfig(ServiceConfig config) throws ServiceException {

        }

        @Override
        public <T> CommonFuture<T> call(ServiceCalling calling) {
            return null;
        }

        public void setReload(Boolean reload) {
            this.reload = reload;
        }

        @Override
        public RemoteServiceConfig getConfig() {
            return (RemoteServiceConfig) this.config;
        }
    }

    /**
     * RPC Future
     */
    protected class RpcFuture extends CommonFuture<RpcResponse> {

        private RpcBody rpcBody;

        public RpcFuture(RpcBody body) {
            this.rpcBody = body;
        }

        @Override
        public void run() {

            String ph = null;
            try {
                ph = createNode(rpcPath + "/" + instanceName + "_", rpcBody.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
                zoo.getData(ph,watchedEvent -> {
                    try {
                        byte[] data = zoo.getData(watchedEvent.getPath(), false, null);
                        zoo.delete(watchedEvent.getPath(), -1);
                        RpcResponse response = Utils.loadFromJSON(new String(data), RpcResponse.class);
                        done(response,null);
                    } catch (Exception e) {
                        done(null,e);
                    }
                }, null);
            } catch (Exception e) {
                RpcResponse response = new RpcResponse();
                response.throwable = e;
                if(ph != null) {
                    try {
                        zoo.delete(ph,-1);
                    } catch (Exception e1){
                        onException(e1);
                    }
                }
                done(response);
            }
        }
    }

    /**
     * RPC Response
     */
    public static class RpcResponse {

        /**
         * Responder
         */
        public String instance;

        /**
         * result
         */
        public JsonObject data;

        /**
         * exception
         */
        public Throwable throwable;

        @Override
        public String toString() {
            return Utils.toJSON(this);
        }

        public byte[] getBytes() {
            return toString().getBytes();
        }
    }

    /**
     * RPC Body
     */
    public static class RpcBody {

        /**
         * RPC Target
         */
        public List<String> instances;

        /**
         * RPC Type
         */
        public RpcType type;

        /**
         * parameters
         */
        public Map<String, Object> params;


        public RpcBody() {
            type = RpcType.NONE;
            params = new HashMap<>();
            instances = new ArrayList<>();
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

        public RpcBody addInstance(String instance) {
            if(instance != null)
                this.instances.add(instance);
            return this;
        }

        public RpcBody addInstances(Collection<String> instances) {
            if(instances != null && instances.size() > 0)
                this.instances.addAll(instances);
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
