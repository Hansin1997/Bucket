package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.other.ServiceUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A base implementation of Bucket interface
 */
public class BucketBase implements Bucket {

    /**
     * The configure of bucket
     */
    protected BucketConfig config;

    /**
     * The Scanned service
     */
    protected Map<String, ServiceConfig> servicesMap;

    /**
     * The working services
     */
    protected Map<String, Service> workingServices;

    /**
     * The stopped services
     */
    protected Map<String, Service> stoppedServices;

    public BucketBase() {
        servicesMap = new HashMap<>();
        workingServices = new HashMap<>();
        stoppedServices = new HashMap<>();
    }

    @Override
    public synchronized CommonFuture<BucketBase> initialize(BucketConfig config) {
        this.config = config;
        Collection<Service> services = new HashSet<>();
        services.addAll(workingServices.values());
        return new CommonFuture<BucketBase>() {
            @Override
            public void run() {
                for (Service service : services) {
                    if (service == null)
                        continue;
                    stopService(service.getConfig().name);
                }
                servicesMap.clear();
                servicesMap.putAll(config.getServiceConfigs());
                done(BucketBase.this);
            }
        }.start();
    }

    @Override
    public CommonFuture<BucketBase> destroy() {
        return new CommonFuture<BucketBase>(){
            @Override
            public void run() {
                Collection<Service> services = workingServices.values();
                for (Service service : services) {
                    try {
                        service.stop();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                servicesMap.clear();
                workingServices.clear();
                stoppedServices.clear();
                done(BucketBase.this);
            }
        }.start();
    }

    @Override
    public synchronized  <T> CommonFuture<T> callService(String name, ServiceCalling calling) {
        Service service = workingServices.get(name);
        if(service == null)
            throw new ServiceException(101, "Service is not running.");
        return (CommonFuture<T>) service.call(calling).start();
    }

    @Override
    public synchronized <T extends Service> CommonFuture<T> stopService(String name) {
        return new CommonFuture<T>(){
            @Override
            public void run() {
                Service service = workingServices.remove(name);
                service.stop().addListener(new CommonListener() {
                    @Override
                    public void onDone(Object result) {
                        stoppedServices.put(name, service);
                        done((T) service);
                    }
                });
            }
        }.start();
    }

    @Override
    public synchronized <T extends Service> CommonFuture<T> startService(String name, boolean reload) {
        Service service;
        if((service = workingServices.get(name)) != null) {
            if(reload) {
                stopService(name);
            }else{
                Service finalService = service;
                return new CommonFuture<T>(){
                    @Override
                    public void run() {
                        done((T) finalService);
                    }
                }.start();
            }
        }
        service = null;
        if (!reload) {
            service = stoppedServices.get(name);
            if (service == null) {
                return startService(name, true);
            }else {
                CommonFuture<T> result = service.start();
                result.addListener(new CommonFuture.CommonListener<T>() {
                    @Override
                    public void onDone(T result) {
                        stoppedServices.remove(name);
                        workingServices.put(name,result);
                    }
                });
                return result;
            }
        } else {
            ServiceConfig config = servicesMap.get(name);
            if (config == null)
                throw new ServiceException(100, "Service doesn't exist.");
            service = ServiceUtils.createService(config);
            if (service != null) {
                CommonFuture<T> result =  service.start();
                workingServices.put(name, service);
                return result;
            }else{
                return null;
            }

        }

    }


    @Override
    public <T extends ServiceConfig> Map<String, T> getServiceConfigs() {
        return (Map<String, T>) servicesMap;
    }

    @Override
    public <T extends ServiceConfig> T getServiceConfig(String name) {
        if(servicesMap == null)
            return null;
        return (T) servicesMap.get(name);
    }

    @Override
    public <T extends Service> Map<String, T> getServices() {
        return (Map<String, T>) workingServices;
    }

    @Override
    public <T extends Service> T getService(String name) {
        if (workingServices == null)
            return null;
        return (T) workingServices.get(name);
    }

    @Override
    public BucketConfig getConfig() {
        return config;
    }

}
