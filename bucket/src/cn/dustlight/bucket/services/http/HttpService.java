package cn.dustlight.bucket.services.http;

import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.ServiceCalling;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.services.NettyService;
import cn.dustlight.bucket.services.http.handler.HttpHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Http Service base on netty
 * <p>
 * you can set your own handler to dispose your business
 */
public class HttpService extends NettyService {

    /**
     * Http Channel Handler
     */
    private ChannelHandler channelHandler;

    /**
     * Logging Handler
     */
    private LoggingHandler loggingHandler;

    /**
     * Http Handler
     */
    private HttpHandler handler;

    /**
     * Error message when handler not set
     */
    protected final static byte[] ERROR_MSG_HANDLER_NOT_SET = "Handler Not Set!".getBytes();
    public static int HTTP_MAX_CONTENT_LENGTH = 65536;
    public static int HTTP_SOCKET_BACKLOG = 2048;

    /**
     * Set Http Handler
     *
     * @param handler handler
     */
    public void setHandler(HttpHandler handler) {
        this.handler = handler;
    }

    @Override
    protected ServerBootstrap buildBootstrap(ServiceConfig config) {
        ServerBootstrap bs = new ServerBootstrap();
        bs.group(boss, workers)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, HTTP_SOCKET_BACKLOG)
                .handler(loggingHandler)
                .childHandler(channelHandler);
        return bs;
    }

    @Override
    protected <T extends Service> CommonFuture<T> doInit(ServiceConfig config) {
        if (this.handler == null)
            this.handler = new HttpHandler() {
                @Override
                public void init(HttpService httpService) {

                }

                @Override
                public void dispose(HttpRequest q, ChannelHandlerContext ctx) {
                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    response.content().writeBytes(ERROR_MSG_HANDLER_NOT_SET);
                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                }
            };
        setHandler(this.handler);
        this.handler.init(this);
        loggingHandler = new LoggingHandler(LogLevel.INFO);
        channelHandler = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                socketChannel.pipeline().addLast("http-aggregator", new HttpObjectAggregator(HTTP_MAX_CONTENT_LENGTH));
                socketChannel.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                socketChannel.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                socketChannel.pipeline().addLast("http-service", new HttpServiceHandler());
            }
        };

        return super.doInit(config);
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

    /**
     * Netty Http Handler
     */
    protected class HttpServiceHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            HttpRequest q = (HttpRequest) msg;
            HttpService.this.handler.dispose(q, ctx);
            ctx.flush();
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }

}
