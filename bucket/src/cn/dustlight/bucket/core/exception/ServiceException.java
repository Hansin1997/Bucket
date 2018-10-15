package cn.dustlight.bucket.core.exception;

import cn.dustlight.bucket.other.Utils;

/**
 * 服务异常类
 */
public class ServiceException extends RuntimeException {

    /**
     * 错误码
     */
    public int code;

    /**
     * 错误消息
     */
    public String msg;

    public ServiceException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public static ServiceException UnknowError() {
        return new ServiceException(0, "Unknow Error");
    }

    /**
     * 转换至JSON字符串
     *
     * @return JSON字符串
     */
    public String toJSON() {
        return Utils.toJSON(this);
    }
}
