package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.ServiceConfig;

import java.util.Map;

/**
 * Bucket包装器
 */
public class BucketWrapper implements Bucket {

    protected Bucket bucket;

    public Boolean enable;

    public BucketWrapper(Bucket bucket) {
        this.bucket = bucket;
    }

    @Override
    public void initialize(BucketConfig config) {
        this.bucket.initialize(config);
    }

    @Override
    public void destory() {
        this.bucket.destory();
    }

    @Override
    public <T extends Service> T startService(String name, boolean reload) {
        return this.bucket.startService(name, reload);
    }

    @Override
    public <T extends Service> T stopService(String name) {
        return this.bucket.stopService(name);
    }

    @Override
    public <T> T callService(String name, ServiceCalling calling) {
        return this.bucket.callService(name,calling);
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
