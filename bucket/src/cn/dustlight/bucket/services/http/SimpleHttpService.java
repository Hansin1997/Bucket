package cn.dustlight.bucket.services.http;

import cn.dustlight.bucket.services.http.handler.SimpleHttpHandler;

/**
 * Simple Http Service
 * with SimpleHttpHandler
 */
public class SimpleHttpService extends HttpService {
    public SimpleHttpService() {
        setHandler(new SimpleHttpHandler());
    }
}
