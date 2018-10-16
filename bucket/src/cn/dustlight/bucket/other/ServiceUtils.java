package cn.dustlight.bucket.other;

import cn.dustlight.bucket.core.config.ServiceConfig;
import cn.dustlight.bucket.core.Service;
import cn.dustlight.bucket.services.http.SimpleHttpService;

public class ServiceUtils {

    public static Service createService(ServiceConfig config) {
        Service service = null;
        switch (config.type) {
            case HTTP:
                service = new SimpleHttpService();
                service.initialize(config);
                break;
        }
        return service;
    }
}
