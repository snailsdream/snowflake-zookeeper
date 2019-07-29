package com.globalid.snowflake.exception;

/**
 * @Date: 2019/1/10 12:45
 * @Description: 自定义异常
 */
public class SnowFlakeException extends RuntimeException {
    public SnowFlakeException () {
        super();
    }
    public SnowFlakeException(String msg) {
        super(msg);
    }
}
