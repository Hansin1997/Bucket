package cn.dustlight.bucket.services.http;

import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.ServiceCalling;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.services.http.handler.HttpHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.net.InetSocketAddress;

/**
 * Http Service base on netty
 * <p>
 * you can set your own handler to dispose your business
 */
public class HttpService extends Service {

    /**
     * Netty Boss Group
     */
    private EventLoopGroup boss;

    /**
     * Netty Worker Group
     */
    private EventLoopGroup workers;

    private ServerBootstrap bootstrap;
    private ChannelFuture channelFuture;

    private ChannelHandler channelHandler;
    private LoggingHandler loggingHandler;

    /**
     * Http Handler
     */
    private HttpHandler handler;

    protected final static byte[] ERROR_MSG_HANDLER_NOT_SET = "Handler not set!".getBytes();

    protected ServerBootstrap createBootstrap() {
        this.boss = new NioEventLoopGroup(1);
        this.workers = new NioEventLoopGroup();
        ServerBootstrap bs = new ServerBootstrap();
        bs.group(boss, workers)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 2048)
                .handler(loggingHandler)
                .childHandler(channelHandler);
        return bs;
    }

    @Override
    protected CommonFuture<HttpService> doInit(ServiceConfig config) {
        if (this.handler == null)
            this.handler = new HttpHandler() {
                @Override
                public void init(HttpService httpService) {

                }

                @Override
                public void dispose(HttpRequest q, ChannelHandlerContext ctx) {
                    DefaultFullHttpResponse r = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    r.content().writeBytes(ERROR_MSG_HANDLER_NOT_SET);
                    ctx.writeAndFlush(r).addListener(ChannelFutureListener.CLOSE);
                }
            };

        loggingHandler = new LoggingHandler(LogLevel.INFO);
        channelHandler = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                socketChannel.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                socketChannel.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                socketChannel.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                socketChannel.pipeline().addLast("http-service", new HttpServiceHandler(HttpService.this.handler));
            }
        };

        bootstrap = createBootstrap();

        handler.init(this);
        return new CommonFuture<HttpService>() {
            @Override
            public void run() {
                done(HttpService.this);
            }
        }.start();
    }

    @Override
    public void resetConfig(ServiceConfig config) {
        if (this.isRunning())
            this.stop();
        this.initialize(config);
        this.start();
    }

    @Override
    public <T> CommonFuture<T> call(ServiceCalling calling) {
        return new CommonFuture<T>() {
            @Override
            public void run() {
                done(null, null);
            }
        }.start();
    }

    @Override
    protected CommonFuture<HttpService> doStart(ServiceConfig config) {
        return new CommonFuture<HttpService>() {
            @Override
            public void run() {
                try {
                    channelFuture = bootstrap.bind(config.host, config.port).sync();
                    InetSocketAddress add = (InetSocketAddress) channelFuture.channel().localAddress();
                    getConfig().port = add.getPort();
                    done(HttpService.this, null);
                } catch (Exception e) {
                    done(HttpService.this, e);
                }
            }
        };
    }

    @Override
    protected CommonFuture<HttpService> doStop() {
        return new CommonFuture<HttpService>() {
            @Override
            public void run() {
                ServiceException exception = null;
                try {
                    channelFuture.channel().close();
                    boss.shutdownGracefully();
                    workers.shutdownGracefully();
                    bootstrap = createBootstrap();
                } catch (Exception e) {
                    e.printStackTrace();
                    exception = new ServiceException(-101, "Stop Service Error.");
                    exception.addSuppressed(e);
                }
                done(HttpService.this, exception);
            }
        };
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

    public void setHandler(HttpHandler handler) {
        this.handler = handler;
    }

    /**
     * A Netty Http Handler
     */
    public static class HttpServiceHandler extends ChannelInboundHandlerAdapter {

        private HttpHandler handler;

        public HttpServiceHandler(HttpHandler handler) {
            this.handler = handler;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            HttpRequest q = (HttpRequest) msg;
            handler.dispose(q, ctx);
            ctx.flush();
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}
