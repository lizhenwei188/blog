package com.shiyi.config.mybatisplus;

import org.apache.ibatis.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lizhenwei
 * @date: 2022/5/31 20:06
 * @email: lizhenwei188@foxmail.com
 * @description:
 */
public class MybatisStdOut implements Log {

    private static StringBuilder resultData = new StringBuilder();

    private final Logger logger = LoggerFactory.getLogger(MybatisStdOut.class);

    public MybatisStdOut(String clazz) {
        // Do Nothing
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void error(String s, Throwable e) {
        logger.error(s,e);
    }

    @Override
    public void error(String s) {
        logger.error(s);
    }

    /**
     * MyBatis动作 打印
     * 执行Sql与参数 打印
     */
    @Override
    public void debug(String s) {
        // do nothing
    }

    /**
     * Sql执行结果，打印
     */
    @Override
    public void trace(String s) {
        resultData.append(s).append("\n");
    }

    @Override
    public void warn(String s) {
        logger.warn(s);
    }

    public static void resultClean(StringBuilder s) {
        resultData = s;
    }

    public static void resultSet() {
        MybatisInterceptor.resultData = resultData;
    }

}