package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.other.ServiceUtils;
import org.python.util.PythonInterpreter;

import java.util.*;

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

        /** init python script engine **/
        Properties props = new Properties();
        props.put("python.console.encoding", "UTF-8");
        props.put("python.security.respectJavaAccessibility", "false");
        props.put("python.import.site", "false");
        Properties preprops = System.getProperties();
        PythonInterpreter.initialize(props, preprops, new String[]{});
    }

    @Override
    public synchronized CommonFuture<BucketBase> initialize(BucketConfig config) {
        this.config = config;
        Collection<Service> services = new HashSet<>();
        services.addAll(workingServices.values());
        return new CommonFuture<BucketBase>() {
            @Override
            public void run() {
                try {
                    for (Service service : services) {
                        if (service == null)
                            continue;
                        stopService(service.getConfig().name);
                    }
                    servicesMap.clear();
                    servicesMap.putAll(config.getServiceConfigs());
                    done(BucketBase.this);
                } catch (Exception e) {
                    done(BucketBase.this, e);
                }

            }
        }.start();
    }

    @Override
    public CommonFuture<BucketBase> destroy() {
        return new CommonFuture<BucketBase>() {
            @Override
            public void run() {
                Collection<Service> services = workingServices.values();
                for (Service service : services) {
                    try {
                        service.stop();
                    } catch (Exception e) {
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
    public synchronized <T> CommonFuture<T> callService(String name, ServiceCalling calling) {
        Service service = workingServices.get(name);
        if (service == null)
            return new CommonFuture<T>() {
                @Override
                public void run() {
                    done(new ServiceException(101, "Service is not running."));
                }
            }.start();
        else
            return (CommonFuture<T>) service.call(calling).start();
    }

    @Override
    public synchronized <T extends Service> CommonFuture<T> stopService(String name) {
        Service service = workingServices.get(name);
        if (service == null)
            return new CommonFuture<T>() {
                @Override
                public void run() {
                    done(new ServiceException(101, "Service is not running."));
                }
            }.start();
        else
            return new CommonFuture<T>() {
                @Override
                public void run() {
                    try {
                        service.stop().addListener((result, e) -> {
                            stoppedServices.put(name, service);
                            done((T) service, e);
                        });
                    } catch (Exception e) {
                        done(null, e);
                    }
                }
            }.start();
    }

    @Override
    public synchronized <T extends Service> CommonFuture<T> startService(String name, boolean reload) {
        Service service;
        if ((service = workingServices.get(name)) != null) {
            if (reload) {
                stopService(name);
            } else {
                return service.start();
            }
        }
        service = null;
        if (!reload) {
            service = stoppedServices.get(name);
            if (service == null) {
                return startService(name, true);
            } else {
                CommonFuture<T> result = service.start();
                result.addListener((rs, e) -> {
                    stoppedServices.remove(name);
                    workingServices.put(name, rs);
                });
                return result;
            }
        } else {
            ServiceConfig config = servicesMap.get(name);
            if (config == null)
                return new CommonFuture<T>() {
                    @Override
                    public void run() {
                        done(new ServiceException(100, "Service doesn't exist."));
                    }
                }.start();
            service = ServiceUtils.createService(config);
            if (service != null) {
                CommonFuture<T> result = service.start();
                workingServices.put(name, service);
                return result;
            } else {
                return new CommonFuture<T>() {
                    @Override
                    public void run() {
                        done(new ServiceException(100, "Service is null."));
                    }
                }.start();
            }
        }
    }


    @Override
    public <T extends ServiceConfig> Map<String, T> getServiceConfigs() {
        return (Map<String, T>) servicesMap;
    }

    @Override
    public <T extends ServiceConfig> T getServiceConfig(String name) {
        if (servicesMap == null)
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
