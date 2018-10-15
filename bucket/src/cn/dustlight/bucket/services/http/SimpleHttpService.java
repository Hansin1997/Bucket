package cn.dustlight.bucket.services.http;

import cn.dustlight.bucket.services.http.handler.SimpleHttpHandler;

/**
 * 简单Http服务
 */
public class SimpleHttpService extends HttpService {
    public SimpleHttpService() {
        setHandler(new SimpleHttpHandler());
    }
}
