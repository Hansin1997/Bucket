package cn.dustlight.bucket.sample;

import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.ServiceCalling;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.services.http.HttpService;
import cn.dustlight.bucket.services.http.handler.MethodMappingHandler;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.Charset;

public class JarSample extends HttpService {

    public JarSample() {
        setHandler(new TestHandler());
    }

    @Override
    public synchronized <T extends Service> CommonFuture<T> initialize(ServiceConfig config) {
        return super.initialize(config);
    }

    private class TestHandler extends MethodMappingHandler {

        public ChannelFuture _index(Context context) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.content().writeCharSequence("Hello Jar", Charset.forName("utf-8"));
            return context.writeAndClose(response);
        }
    }

    @Override
    public <T> CommonFuture<T> call(ServiceCalling calling) {
        return super.call(calling);
    }
}
