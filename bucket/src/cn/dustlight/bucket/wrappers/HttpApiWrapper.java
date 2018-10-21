package cn.dustlight.bucket.wrappers;

import cn.dustlight.bucket.core.Bucket;
import cn.dustlight.bucket.core.BucketWrapper;
import cn.dustlight.bucket.core.ServiceCalling;
import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.other.Utils;
import cn.dustlight.bucket.services.http.HttpService;
import cn.dustlight.bucket.services.http.handler.MethodMappingHandler;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.*;

import java.io.*;
import java.net.URISyntaxException;

/**
 * Http API Wrapper
 * this wrapper offer url to operate the Bucket interface
 */
public class HttpApiWrapper extends BucketWrapper {


    /**
     * bind host
     */
    public String host;

    /**
     * bind port
     */
    public Integer port;

    /**
     * API Http Service
     */
    private HttpService httpService;

    public HttpApiWrapper(Bucket bucket) {
        super(bucket);

    }

    @Override
    public CommonFuture<Bucket> initialize(BucketConfig config) {
        return super.initialize(config).addListener((result, e) -> {
            httpService = new HttpService();
            httpService.setHandler(new MappingApiHandler());
            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.host = HttpApiWrapper.this.host;
            serviceConfig.port = HttpApiWrapper.this.port;
            httpService.initialize(serviceConfig);
            httpService.start();
        });
    }

    @Override
    public CommonFuture<Bucket> destroy() {
        return super.destroy().addListener((result, e) -> httpService.stop());

    }

    /**
     * API Mapping
     */
    public class MappingApiHandler extends MethodMappingHandler {

        public ChannelFuture _index(Context context) throws IOException, URISyntaxException {
            return context.writeFile(new File(getClass().getResource("HttpApi.html").toURI()));
        }

        public ChannelFuture _test(Context context) {
            return _test_index(context);
        }

        public ChannelFuture _test_index(Context context) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set("Content-Type", "text/html;charset=utf-8");
            response.content().writeBytes(("" + context.params).getBytes());
            return context.writeAndClose(response).addListener(future -> response.release());
        }

        public ChannelFuture _lunch(Context context) {
            HttpApiWrapper.this
                    .startService(context.params.get("s"), false)
                    .addListener((service, e) -> {
                        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                        try {
                            if (e != null)
                                throw e;
                            if (service == null || service.getConfig() == null) {
                                throw new NullPointerException("service or config is null");
                            }

                            response.headers().set("Content-Type", "application/json;charset=utf-8");
                            response.content().writeBytes(Utils.toJSON(service.getConfig()).getBytes());
                            context.writeAndClose(response).addListener(future -> response.release());
                        } catch (Exception e2) {
                            Exception(context,e2);
                            response.release();
                        }

                    });
            context.DoNotClose(true);
            return null;
        }

        public ChannelFuture _call(Context context) {
            ServiceCalling calling = new ServiceCalling(context.params.get("m"));
            calling.getParams().putAll(context.params);
            HttpApiWrapper.this
                    .callService(context.params.get("s"), calling)
                    .addListener((object, e) -> {
                        try {
                            if (e != null)
                                throw e;
                            if (object == null) {
                                throw new NullPointerException("result is null");
                            }
                            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                            response.headers().set("Content-Type", "application/json;charset=utf-8");
                            response.content().writeBytes(Utils.toJSON(object).getBytes());
                            context.writeAndClose(response);
                        } catch (Exception e2) {
                            Exception(context,e2);
                        }

                    });
            context.DoNotClose(true);
            return null;
        }

        public ChannelFuture _stop(Context context) {

            HttpApiWrapper.this
                    .stopService(context.params.get("s"))
                    .addListener((object, e) -> {
                        try {
                            if (e != null)
                                throw e;
                            if (object == null) {
                                throw new NullPointerException("result is null");
                            }
                            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                            response.headers().set("Content-Type", "application/json;charset=utf-8");
                            response.content().writeBytes(Utils.toJSON(object.getConfig()).getBytes());
                            context.writeAndClose(response);
                        } catch (Exception e2) {
                            Exception(context,e2);
                        }

                    });
            context.DoNotClose(true);
            return null;
        }
    }
}
