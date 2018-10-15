package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;

/**
 * 服务抽象类
 */
public abstract class Service {

    /**
     * 服务配置
     */
    private ServiceConfig config;

    /**
     * 是否正在运行
     */
    private boolean isRunning;

    /**
     * 初始化
     *
     * @param config 服务配置
     * @throws ServiceException 服务异常
     */
    public synchronized void initialize(ServiceConfig config) throws ServiceException {
        this.config = config;
        if (this.isRunning)
            stop();
        doInit(this.config);
    }

    /**
     * 开启服务
     *
     * @throws ServiceException 服务异常
     */
    public synchronized void start() throws ServiceException {
        if (isRunning)
            return;
        doStart(config);
        this.isRunning = true;
    }

    /**
     * 停止服务
     *
     * @throws ServiceException 服务异常
     */
    public synchronized void stop() throws ServiceException {
//        try{
//            doStop();
//        }catch (ServiceException e){
//            this.isRunning = false;
//            throw e;
//        }
        if (!this.isRunning)
            return;
        doStop();
        this.isRunning = false;
    }

    /**
     * 服务是否正在运行
     *
     * @return
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 执行初始化
     *
     * @param config 服务配置
     * @throws ServiceException 服务异常
     */
    protected abstract void doInit(ServiceConfig config) throws ServiceException;

    /**
     * 执行启动
     *
     * @throws ServiceException 服务异常
     */
    protected abstract void doStart(ServiceConfig config) throws ServiceException;

    /**
     * 执行停止
     *
     * @throws ServiceException 服务异常
     */
    protected abstract void doStop() throws ServiceException;

    /**
     * 重新设置服务配置
     *
     * @param config 服务配置
     * @throws ServiceException 服务异常
     */
    public abstract void resetConfig(ServiceConfig config) throws ServiceException;

    /**
     * 调用服务方法
     *
     * @param calling 调用对象
     * @return 调用结果
     */
    public abstract Object call(ServiceCalling calling);

    /**
     * 获取服务配置
     *
     * @return
     */
    public <T extends ServiceConfig> T getConfig() {
        return (T) config;
    }

    /**
     * 设置服务配置
     *
     * @param config
     * @throws ServiceException
     */
    public void setConfig(ServiceConfig config) throws ServiceException {
        this.config = config;
        resetConfig(config);
    }
}
