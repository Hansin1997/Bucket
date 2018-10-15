package cn.dustlight.bucket.services.http;

import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.ServiceCalling;
import cn.dustlight.bucket.core.exception.ServiceException;
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

/**
 * Http服务
 */
public class HttpService extends Service {

    private EventLoopGroup boss;
    private EventLoopGroup workers;
    private ServerBootstrap bootstrap;
    private ChannelFuture channelFuture;
    private HttpHandler handler;
    protected final static byte[] ERROR_MSG_HANDLER_NOT_SET = "Handler not set!".getBytes();

    @Override
    protected void doInit(ServiceConfig config) throws ServiceException {
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
        this.boss = new NioEventLoopGroup(1);
        this.workers = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        bootstrap.group(boss, workers)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 2048)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                        socketChannel.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                        socketChannel.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                        socketChannel.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                        socketChannel.pipeline().addLast("http-service", new HttpServiceHandler(HttpService.this.handler));
                    }
                });

        handler.init(this);
    }

    @Override
    public void resetConfig(ServiceConfig config) throws ServiceException {
        if (this.isRunning())
            this.stop();
        this.initialize(config);
        this.start();
    }

    @Override
    public Object call(ServiceCalling calling) {
        return null;
    }

    @Override
    protected void doStart(ServiceConfig config) throws ServiceException {
        try {
            channelFuture = bootstrap.bind(config.host, config.port).sync();
        } catch (Exception e) {
            throw new ServiceException(-100, e.toString());
        }
    }

    @Override
    protected void doStop() throws ServiceException {
        ServiceException exception = null;
        try {
            channelFuture.channel().close();
        } catch (Exception e) {
            e.printStackTrace();
            exception = new ServiceException(-101, e.toString());
        }
        boss.shutdownGracefully();
        workers.shutdownGracefully();
        if (exception != null)
            throw exception;
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
     * Http业务Netty接口
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