package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.ServiceUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 容器
 */
public class BucketBase implements Bucket {

    protected BucketConfig config;
    protected Map<String, ServiceConfig> servicesMap;
    protected Map<String, Service> workingServices;
    protected Map<String, Service> stoppedServices;

    public BucketBase() {
        servicesMap = new HashMap<>();
        workingServices = new HashMap<>();
        stoppedServices = new HashMap<>();
    }

    @Override
    public synchronized void initialize(BucketConfig config) {
        this.config = config;
        Collection<Service> services = new HashSet<>();
        services.addAll(workingServices.values());
        for (Service service : services) {
            if (service == null)
                continue;
            stopService(service.getConfig().name);
        }
        servicesMap.clear();
        servicesMap.putAll(config.getServiceConfigs());
    }

    @Override
    public void destory() {
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
    }

    @Override
    public synchronized <T extends Service> T stopService(String name) {
        Service service = workingServices.remove(name);
        service.stop();
        stoppedServices.put(name, service);
        return (T) service;
    }

    @Override
    public synchronized  <T> T callService(String name, ServiceCalling calling) {
        Service service = workingServices.get(name);
        if(service == null)
            throw new ServiceException(101, "Service is not running.");
        return (T) service.call(calling);
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

    @Override
    public synchronized <T extends Service> T startService(String name, boolean reload) {
        Service service;
        if((service = workingServices.get(name)) != null) {
            if(reload) {
                stopService(name);
            }else{
                return (T) service;
            }
        }
        service = null;
        if (!reload) {
            service = stoppedServices.get(name);
            if (service == null)
                return startService(name, true);
        } else {
            ServiceConfig config = servicesMap.get(name);
            if (config == null)
                throw new ServiceException(100, "Service doesn't exist.");
            service = ServiceUtils.createService(config);
        }
        if (service != null) {
            service.start();
            workingServices.put(name, service);
            return (T) service;
        }else
            return null;
    }

}
