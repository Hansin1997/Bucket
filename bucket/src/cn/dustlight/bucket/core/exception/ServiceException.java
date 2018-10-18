package cn.dustlight.bucket.core.exception;

import cn.dustlight.bucket.other.Utils;

/**
 * Service Exception
 */
public class ServiceException extends Exception {

    /**
     * Error Code
     */
    public int code;

    /**
     * Error Message
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
     * To JSON String
     *
     * @return JSON String
     */
    public String toJSON() {
        return Utils.toJSON(this);
    }
}
