package cn.dustlight.bucket.services;

import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.CommonFuture;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;

/**
 * NettyService
 * <p>
 * You can easily build a highly concurrent service with Netty.
 */
public abstract class NettyService extends Service {

    /**
     * Netty Boss Group
     */
    protected EventLoopGroup boss;

    /**
     * Netty Worker Group
     */
    protected EventLoopGroup workers;

    /**
     * Netty Bootstrap
     */
    protected ServerBootstrap bootstrap;

    /**
     * ChannelFuture object,created by bootstrap
     */
    protected ChannelFuture channelFuture;

    /**
     * build a ServerBootstrap to lunch a netty server
     *
     * @param config service configure
     * @return Bootstrap
     */
    protected abstract ServerBootstrap buildBootstrap(ServiceConfig config);

    /**
     * init boss and workers with config
     *
     * @param config service configure
     */
    protected void initEventLoopGroups(ServiceConfig config) {
        try {
            if (this.boss != null && !this.boss.isShutdown())
                this.boss.shutdownGracefully();
            if (this.workers != null && !this.workers.isShutdown())
                this.workers.shutdownGracefully();
        } catch (Exception e) {

        }
        try {
            this.boss = new NioEventLoopGroup(config.getParam("boss_num"));
            this.workers = new NioEventLoopGroup(config.getParam("workers_num"));
        } catch (Exception e) {
            this.boss = new NioEventLoopGroup();
            this.workers = new NioEventLoopGroup();
        }
    }

    @Override
    protected <T extends Service> CommonFuture<T> doInit(ServiceConfig config) {
        return new CommonFuture<T>() {
            @Override
            public void run() {
                try {
                    initEventLoopGroups(config);
                    bootstrap = buildBootstrap(config);
                    done((T) NettyService.this);
                } catch (Exception e) {
                    done((T) NettyService.this, e);
                }
            }
        };
    }

    @Override
    protected <T extends Service> CommonFuture<T> doStart(ServiceConfig config) {
        return new CommonFuture<T>() {
            @Override
            public void run() {
                try {
                    channelFuture = bootstrap.bind(config.host, config.port).sync();
                    InetSocketAddress add = (InetSocketAddress) channelFuture.channel().localAddress();
                    getConfig().port = add.getPort();
                    done((T) NettyService.this);
                } catch (Exception e) {
                    done((T) NettyService.this, e);
                }
            }
        };
    }

    @Override
    protected <T extends Service> CommonFuture<T> doStop() {
        return new CommonFuture<T>() {
            @Override
            public void run() {
                ServiceException exception = null;
                try {
                    channelFuture.channel().close();
                    initEventLoopGroups(getConfig());
                    bootstrap = buildBootstrap(getConfig());
                } catch (Exception e) {
                    exception = new ServiceException(-101, "Stop Service Error.");
                    exception.addSuppressed(e);
                }
                done((T) NettyService.this, exception);
            }
        };
    }

    @Override
    public void resetConfig(ServiceConfig config) throws ServiceException {
        if (this.isRunning())
            this.stop();
        this.initialize(config);
        this.start();
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public ServerBootstrap getBootstrap() {
        return bootstrap;
    }

    public EventLoopGroup getBoss() {
        return boss;
    }

    public EventLoopGroup getWorkers() {
        return workers;
    }

}
