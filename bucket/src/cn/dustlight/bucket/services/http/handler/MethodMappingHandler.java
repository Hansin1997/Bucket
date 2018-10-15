package cn.dustlight.bucket.services.http.handler;

import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.exception.ServiceException;
import cn.dustlight.bucket.other.HttpChunkFileEchoer;
import cn.dustlight.bucket.other.Utils;
import cn.dustlight.bucket.services.http.HttpService;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodMappingHandler implements HttpHandler {

    private Class<?> aClass;

    @Override
    public void init(HttpService httpService) {
        ServiceConfig config = httpService.getConfig();
        aClass = getClass();
    }

    @Override
    public void dispose(HttpRequest q, ChannelHandlerContext ctx) {
        Context context = new Context();
        context.request = q;
        context.ctx = ctx;
        try {

            URI uri = new URI(q.uri());
            context.params = Utils.QueryDecode(uri.getQuery());
            context.uri = uri;
            if(q.method().equals(HttpMethod.POST)) {
                HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(q);
                decoder.offer((FullHttpRequest)q);
                List<InterfaceHttpData> list = decoder.getBodyHttpDatas();
                if (context.params == null)
                    context.params = new HashMap<>();
                for(InterfaceHttpData data : list) {
                    context.params.put(data.getName(),((Attribute) data).getValue());
                }
            }
            String path = uri.getPath().toLowerCase().replace('/', '_');

            if (path.charAt(path.length() - 1) == '_')
                path += "index";

            Method method = aClass.getMethod(path, Context.class);
            method.setAccessible(true);
            Object result = method.invoke(this, context);

            if (result instanceof ChannelFuture && !HttpUtil.isKeepAlive(q))
                ((ChannelFuture) result).addListener(ChannelFutureListener.CLOSE);

        } catch (NoSuchMethodException e) {
            MethodNotFound(context,e);
        } catch (Exception e) {
            e.printStackTrace();
            Exception(context,e);
        }
    }

    protected ChannelFuture MethodNotFound(Context context,Exception e) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        response.headers().set("Content-Type","text/plain");
        response.content().writeCharSequence("Path Not Found.\n" + e, Charset.forName("UTF-8"));
        return context.ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    protected ChannelFuture Exception(Context context,Exception e) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
        response.headers().set("Content-Type","text/plain");
        response.content().writeBytes(e.getMessage().getBytes());
        return context.ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    protected static class Context {

        public HttpRequest request;
        public ChannelHandlerContext ctx;
        public URI uri;
        public Map<String, String> params;

        public ChannelFuture writeAndClose(Object object) {
            return ctx.writeAndFlush(object).addListener(ChannelFutureListener.CLOSE);
        }

        public ChannelFuture write(Object object) {
            return ctx.write(object);
        }

        public ChannelFuture writeAndFlush(Object object) {
            return ctx.writeAndFlush(object);
        }

        public ChannelFuture writeFile(File file) throws IOException {
            return HttpChunkFileEchoer.echo(ctx,file);
        }
    }

}
