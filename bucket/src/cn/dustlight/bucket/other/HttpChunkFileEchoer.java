package cn.dustlight.bucket.other;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;

public class HttpChunkFileEchoer {

    private ChannelHandlerContext ctx;
    private DefaultHttpResponse httpResponse;

    public HttpChunkFileEchoer(ChannelHandlerContext channelHandlerContext) {
        this.ctx = channelHandlerContext;
        this.httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    }

    public HttpChunkFileEchoer header(String key,Object value) {
        httpResponse.headers().set(key,value);
        return this;
    }

    public DefaultHttpResponse getHttpResponse() {
        return httpResponse;
    }

    public ChannelFuture echo(File file) throws IOException {
        RandomAccessFile f = new RandomAccessFile(file, "r");
        ChunkedFile chunkFile = new ChunkedFile(f, 0, file.length(), 8192);
        HttpUtil.setContentLength(httpResponse,chunkFile.length());
        ctx.writeAndFlush(httpResponse);
        ctx.writeAndFlush(chunkFile, ctx.newProgressivePromise());
        return ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                f.close();
                chunkFile.close();
            }
        });
    }

    public static ChannelFuture echo(ChannelHandlerContext ctx,File file) throws IOException {
        return new HttpChunkFileEchoer(ctx).echo(file);
    }
}
