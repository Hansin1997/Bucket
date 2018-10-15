package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.ServiceConfig;

import java.util.Map;

public interface Bucket {

    void initialize(BucketConfig config);

    void destory();

    <T extends Service> T startService(String name, boolean reload);

    <T extends Service> T stopService(String name);

    <T> T callService(String name,ServiceCalling calling);

    <T extends ServiceConfig> Map<String, T> getServiceConfigs();

    <T extends ServiceConfig> T getServiceConfig(String name);

    <T extends Service> Map<String,T> getServices();

    <T extends Service> T getService(String name);

    BucketConfig getConfig();
}