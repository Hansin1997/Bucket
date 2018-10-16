package cn.dustlight.bucket.wrappers;

import cn.dustlight.bucket.core.Bucket;
import cn.dustlight.bucket.core.BucketWrapper;
import cn.dustlight.bucket.core.Service;
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
        return super.initialize(config).addListener(new CommonFuture.CommonListener<Bucket>() {
            @Override
            public void onDone(Bucket result) {
                httpService = new HttpService();
                httpService.setHandler(new MappingApiHandler());
                ServiceConfig serviceConfig = new ServiceConfig();
                serviceConfig.host = HttpApiWrapper.this.host;
                serviceConfig.port = HttpApiWrapper.this.port;
                httpService.setConfig(serviceConfig);
                httpService.start();
            }
        });
    }

    @Override
    public CommonFuture<Bucket> destroy() {
        return super.destroy().addListener(new CommonFuture.CommonListener<Bucket>() {
            @Override
            public void onDone(Bucket result) {
                httpService.stop();
            }
        });

    }

    /**
     * API Mapping
     */
    private class MappingApiHandler extends MethodMappingHandler {

        public ChannelFuture _index(Context context) throws IOException, URISyntaxException {
            return context.writeFile(new File(getClass().getResource("HttpApi.html").toURI()));
        }

        public ChannelFuture _test(Context context) {
            return _test_index(context);
        }

        public ChannelFuture _test_index(Context context) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
            response.headers().set("Content-Type","text/html;charset=utf-8");
            response.content().writeBytes(("" + context.params).getBytes());
           return context.writeAndClose(response);
        }

        public ChannelFuture _lunch(Context context) {
            try{
                HttpApiWrapper.this.startService(context.params.get("s"),false)
                .addListener(new CommonFuture.CommonListener<Service>() {
                    @Override
                    public void onDone(Service result) {
                        try{
                            Service service = result;
                            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
                            response.headers().set("Content-Type","application/json;charset=utf-8");
                            response.content().writeBytes(Utils.toJSON(service.getConfig()).getBytes());
                            context.writeAndClose(response);
                        }catch (Exception e){
                            e.printStackTrace();
                            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            response.headers().set("Content-Type","text/plain;charset=utf-8");
                            response.content().writeBytes((e.toString()).getBytes());
                            context.writeAndClose(response);
                        }

                    }
                });

                context.DoNotClose(true);
                return null;
            }catch (Exception e) {
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
                response.headers().set("Content-Type","text/plain;charset=utf-8");
                response.content().writeBytes(e.toString().getBytes());
                return context.writeAndClose(response);
            }
        }
    }
}
