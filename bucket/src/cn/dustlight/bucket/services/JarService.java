package cn.dustlight.bucket.services;

import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.ServiceCalling;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.CommonFuture;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Jar Proxy
 */
public class JarService extends Service {

    private URLClassLoader loader;
    private URL url;
    private String clazz;
    private Service service;

    @Override
    protected <T extends Service> CommonFuture<T> doInit(ServiceConfig config) throws ServiceException {
        if(config.type != ServiceConfig.ServiceType.JAVA_JAR)
            throw new ServiceException(-1,"Service Type Error: " + config.name + " - " + config.type);
        clazz = config.getParam("class");
        if(clazz == null)
            throw new ServiceException(-2,"JarService: Class Name Not Found! " + config.name);
        try {
            url  = new File(config.root + File.separator + config.path).toURI().toURL();
        } catch (MalformedURLException e) {
            ServiceException t = new ServiceException(-3, "Jar url error: " + config.root + File.pathSeparator + config.path);
            t.addSuppressed(e);
            throw t;
        }
        loader = new URLClassLoader(new URL[]{ url });
        try {
            Class<?> c = loader.loadClass(clazz);
            service = (Service) c.newInstance();
            return service.initialize(config);
        } catch (Exception e) {
            ServiceException se = new ServiceException(-4,"JarService start fail: " + e);
            se.addSuppressed(e);
            throw se;
        }
    }

    @Override
    protected <T extends Service> CommonFuture<T> doStart(ServiceConfig config) throws ServiceException {
        if(loader == null || service == null)
            return (CommonFuture<T>) doInit(config).addListener((result, e) -> {
                if(result != null)
                    result.start();
                if(e != null)
                    e.printStackTrace();
            });
        else
            return service.start();
    }

    @Override
    protected <T extends Service> CommonFuture<T> doStop() throws ServiceException {
        if(service == null)
            throw new ServiceException(-6,"JarService: inner service is null");
        try {
            loader.close();
            loader = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return service.stop();
    }

    @Override
    public void resetConfig(ServiceConfig config) throws ServiceException {
        if(service == null)
            throw new ServiceException(-6,"JarService: inner service is null");
        service.resetConfig(config);
    }

    @Override
    public <T> CommonFuture<T> call(ServiceCalling calling) {
        if(service == null)
            throw new ServiceException(-6,"JarService: inner service is null");
        return service.call(calling);
    }
}
