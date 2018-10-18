package cn.dustlight.bucket.services.http;

import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.ServiceCalling;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.services.NettyService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class WebSocketService extends NettyService {

    public static void main(String[] args) {
        ServiceConfig config = new ServiceConfig();
        config.host = "127.0.0.1";
        config.port = 8887;
        WebSocketService service = new WebSocketService();
        service.initialize(config);
        service.start().addListener((result, e) -> System.out.println(e));
    }

    private LoggingHandler loggingHandler;

    private ChannelHandler channelHandler;

    public static int HTTP_MAX_CONTENT_LENGTH = 65536;
    public static int HTTP_SOCKET_BACKLOG = 2048;

    @Override
    protected ServerBootstrap buildBootstrap(ServiceConfig config) {
        ServerBootstrap bs = new ServerBootstrap();
        bs.group(boss,workers)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, HTTP_SOCKET_BACKLOG)
                .handler(loggingHandler)
                .childHandler(channelHandler);
        return bs;
    }

    @Override
    protected <T extends Service> CommonFuture<T> doInit(ServiceConfig config) {
        loggingHandler = new LoggingHandler(LogLevel.INFO);
        channelHandler = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                pipeline.addLast("HttpServerCodec", new HttpServerCodec());
                pipeline.addLast("ChunkedWriteHandler", new ChunkedWriteHandler());
                pipeline.addLast("HttpObjectAggregator", new HttpObjectAggregator(HTTP_MAX_CONTENT_LENGTH));
                pipeline.addLast("SebSocketServerProtocolHandler", new WebSocketServerProtocolHandler("*"));
                pipeline.addLast("WebSocketHandler", new WebSocketHandler());
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

    public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
            Channel channel = ctx.channel();
            System.out.println(channel.remoteAddress() + ": " + msg.text());
            ctx.channel().writeAndFlush(new TextWebSocketFrame("来自服务端: " + System.currentTimeMillis()));
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            System.out.println("ChannelId" + ctx.channel().id().asLongText());
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            System.out.println("用户下线: " + ctx.channel().id().asLongText());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.channel().close();
        }
    }
}
