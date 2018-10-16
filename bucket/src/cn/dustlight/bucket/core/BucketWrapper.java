package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.other.CommonFuture;

import java.util.Map;

/**
 * Bucket Wrapper
 */
public class BucketWrapper implements Bucket {

    /**
     * Packaged Bucket
     */
    protected Bucket bucket;

    /**
     * Is enable this wrapper
     */
    public Boolean enable;

    public BucketWrapper(Bucket bucket) {
        this.bucket = bucket;
    }

    @Override
    public <T extends Bucket> CommonFuture<T> initialize(BucketConfig config) {
        return this.bucket.initialize(config);
    }

    @Override
    public <T extends Bucket> CommonFuture<T> destroy() {
        return bucket.destroy();
    }

    @Override
    public <T extends Service> CommonFuture<T> startService(String name, boolean reload) {
        return this.bucket.startService(name, reload);
    }

    @Override
    public <T extends Service> CommonFuture<T> stopService(String name) {
        return this.bucket.stopService(name);
    }

    @Override
    public <T> CommonFuture<T> callService(String name, ServiceCalling calling) {
        return this.bucket.callService(name, calling);
    }

    @Override
    public Map<String, ServiceConfig> getServiceConfigs() {
        return this.bucket.getServiceConfigs();
    }

    @Override
    public ServiceConfig getServiceConfig(String name) {
        return this.bucket.getServiceConfig(name);
    }

    @Override
    public <T extends Service> Map<String, T> getServices() {
        return this.bucket.getServices();
    }

    @Override
    public <T extends Service> T getService(String name) {
        return this.bucket.getService(name);
    }

    @Override
    public BucketConfig getConfig() {
        return this.bucket.getConfig();
    }

    public void setBucket(Bucket bucket) {
        this.bucket = bucket;
    }
}
