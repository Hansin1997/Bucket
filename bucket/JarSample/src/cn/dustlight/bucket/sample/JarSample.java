package cn.dustlight.bucket.sample;

import cn.dustlight.bucket.services.http.HttpService;
import cn.dustlight.bucket.services.http.handler.MethodMappingHandler;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.Charset;

public class JarSample extends HttpService {

    public JarSample(){
        setHandler(new TestHandler());
    }

    private class TestHandler extends MethodMappingHandler {

        public ChannelFuture _index(Context context) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.content().writeCharSequence("Hello Jar", Charset.forName("utf-8"));
            return context.writeAndClose(response);
        }
    }

}
