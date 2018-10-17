package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.CommonFuture;


/**
 * The abstract Service
 */
public abstract class Service {

    /**
     * configure
     */
    protected ServiceConfig config;

    /**
     * Is service running
     */
    private boolean isRunning;

    /**
     * Initialize service
     *
     * @param config configure of service
     * @throws ServiceException
     */
    public synchronized <T extends Service> CommonFuture<T> initialize(ServiceConfig config) throws ServiceException {
        this.config = config;
        if (this.isRunning)
            return (CommonFuture<T>) stop().start();
        else
            return (CommonFuture<T>) doInit(this.config).start();

    }

    /**
     * Lunch the service
     *
     * @throws ServiceException
     */
    public synchronized <T extends Service> CommonFuture<T> start() throws ServiceException {
        if (isRunning)
            return null;
        CommonFuture<T> result = (CommonFuture<T>) doStart(config).start();
        this.isRunning = true;
        return result;
    }

    /**
     * Stop the service
     *
     * @throws ServiceException
     */
    public synchronized <T extends Service> CommonFuture<T> stop() throws ServiceException {
        if (!this.isRunning)
            return null;
        CommonFuture<T> result = (CommonFuture<T>) doStop().start();
        this.isRunning = false;
        return result;
    }

    /**
     * Is service running
     *
     * @return
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Do initialize
     *
     * @param config configure of service
     * @throws ServiceException
     */
    protected abstract <T extends Service> CommonFuture<T> doInit(ServiceConfig config) throws ServiceException;

    /**
     * Do lunch
     *
     * @throws ServiceException
     */
    protected abstract <T extends Service> CommonFuture<T> doStart(ServiceConfig config) throws ServiceException;

    /**
     * Do stop
     *
     * @throws ServiceException
     */
    protected abstract <T extends Service> CommonFuture<T> doStop() throws ServiceException;

    /**
     * Reset the configure and service
     *
     * @param config configure of service
     * @throws ServiceException
     */
    public abstract void resetConfig(ServiceConfig config) throws ServiceException;

    /**
     * Call the method in this service
     *
     * @param calling Calling Object
     * @return
     */
    public abstract <T> CommonFuture<T> call(ServiceCalling calling);

    /**
     * Get service configure
     *
     * @return
     */
    public <T extends ServiceConfig> T getConfig() {
        return (T) config;
    }

    /**
     * Set service configure.(Possible reset service)
     *
     * @param config
     * @throws ServiceException
     */
    public void setConfig(ServiceConfig config) throws ServiceException {
        this.config = config;
        resetConfig(config);
    }
}
