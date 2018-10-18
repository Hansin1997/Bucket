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
    public synchronized <T extends Service> CommonFuture<T> initialize(ServiceConfig config) {
        this.config = config;
        if (this.isRunning)
            return new CommonFuture<T>() {
                @Override
                public void run() {
                    stop().addListener((result, e) -> {
                        if (e != null)
                            done((T) result, e);
                        else
                            start().addListener((result1, e1) -> {
                                done(result1, e1);
                            });
                    });
                }
            };
        else
            return (CommonFuture<T>) doInit(this.config).start();

    }

    /**
     * Lunch the service
     *
     * @throws ServiceException
     */
    public synchronized <T extends Service> CommonFuture<T> start() {
        if (this.isRunning)
            return new CommonFuture<T>() {
                @Override
                public void run() {
                    done((T) Service.this, new ServiceException(601,"Service is running"));
                }
            }.start();
        CommonFuture<T> result = (CommonFuture<T>) doStart(config).start().addListener((result1, e) -> this.isRunning = true);
        return result;
    }

    /**
     * Stop the service
     *
     * @throws ServiceException
     */
    public synchronized <T extends Service> CommonFuture<T> stop() {
        if (!this.isRunning)
            return new CommonFuture<T>() {
                @Override
                public void run() {
                    done((T) Service.this, new ServiceException(600,"Service is not running"));
                }
            }.start();
        CommonFuture<T> result = (CommonFuture<T>) doStop().start().addListener((result1, e) -> this.isRunning = false);
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
    protected abstract <T extends Service> CommonFuture<T> doInit(ServiceConfig config);

    /**
     * Do lunch
     *
     * @throws ServiceException
     */
    protected abstract <T extends Service> CommonFuture<T> doStart(ServiceConfig config);

    /**
     * Do stop
     *
     * @throws ServiceException
     */
    protected abstract <T extends Service> CommonFuture<T> doStop();

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
