package cn.dustlight.bucket.wrappers;

import cn.dustlight.bucket.core.Bucket;
import cn.dustlight.bucket.core.BucketBuilder;
import cn.dustlight.bucket.core.BucketWrapper;
import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.Config;
import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.other.Utils;
import cn.dustlight.bucket.services.http.HttpService;
import cn.dustlight.bucket.services.http.handler.MethodMappingHandler;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.*;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Scanner;

public class HttpApiWrapper extends BucketWrapper {


    public static void main(String[] args) throws IOException {

        BucketConfig config = Config.load("bucket.json", BucketConfig.class);

        //BucketBase bucket = new BucketBase();


        //bucket.initialize(config);
        Bucket bucket = BucketBuilder.create()
                .addWrapper("zookeeper",ZooKeeperWrapper.class)
                .addWrapper("http",HttpApiWrapper.class)
                .initialize(config);

        Scanner scanner = new Scanner(System.in);


        while (scanner.hasNext()){
            String cmd = scanner.next();
            if(cmd.equals("quit"))
                break;
        }
        scanner.close();
        //bucket.startService("myweb", true);

    }

    public String host;

    public Integer port;

    private HttpService httpService;

    public HttpApiWrapper(Bucket bucket) {
        super(bucket);

    }

    @Override
    public void initialize(BucketConfig config) {
        super.initialize(config);

        httpService = new HttpService();
        httpService.setHandler(new MappingApiHandler());
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.host = this.host;
        serviceConfig.port = this.port;
        httpService.setConfig(serviceConfig);
        httpService.start();
    }

    @Override
    public void destory() {
        super.destory();
        httpService.stop();
    }

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
                Service service = HttpApiWrapper.this.startService(context.params.get("s"),false);
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
                response.headers().set("Content-Type","text/html;charset=utf-8");
                response.content().writeBytes(("!!!").getBytes());
                return context.writeAndClose(response);
            }catch (Exception e) {
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
                response.headers().set("Content-Type","text/plain;charset=utf-8");
                response.content().writeBytes(e.toString().getBytes());
                return context.writeAndClose(response);
            }


        }
    }
}
