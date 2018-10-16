package cn.dustlight.bucket.services.http.handler;

import cn.dustlight.bucket.services.http.HttpService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Http Handler Interface
 */
public interface HttpHandler {
    void init(HttpService httpService);

    void dispose(HttpRequest q, ChannelHandlerContext ctx);
}