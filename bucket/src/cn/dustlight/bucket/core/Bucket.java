package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.other.CommonFuture;

import java.util.Map;

/**
 * The interface of Bucket
 */
public interface Bucket {

    /**
     * Init the Bucket
     *
     * @param config
     * @param <T>
     * @return
     */
    <T extends Bucket> CommonFuture<T> initialize(BucketConfig config);

    /**
     * Destroy the Bucket
     *
     * @param <T>
     * @return
     */
    <T extends Bucket> CommonFuture<T> destroy();

    /**
     * Lunch service
     *
     * @param name
     * @param reload
     * @param <T>
     * @return
     */
    <T extends Service> CommonFuture<T> startService(String name, boolean reload);

    /**
     * Stop service
     *
     * @param name 服务名
     * @param <T>
     * @return
     */
    <T extends Service> CommonFuture<T> stopService(String name);

    /**
     * Call the method in service
     *
     * @param name
     * @param calling
     * @param <T>
     * @return
     */
    <T extends Object> CommonFuture<T> callService(String name, ServiceCalling calling);

    /**
     * Get all service's configures
     *
     * @param <T>
     * @return
     */
    <T extends ServiceConfig> Map<String, T> getServiceConfigs();

    /**
     * Get the configure of service
     *
     * @param name
     * @param <T>
     * @return
     */
    <T extends ServiceConfig> T getServiceConfig(String name);

    /**
     * Get all service
     *
     * @param <T>
     * @return
     */
    <T extends Service> Map<String, T> getServices();

    /**
     * Get service
     *
     * @param name
     * @param <T>
     * @return
     */
    <T extends Service> T getService(String name);

    /**
     * Get the configure of Bucket
     *
     * @return
     */
    BucketConfig getConfig();
}