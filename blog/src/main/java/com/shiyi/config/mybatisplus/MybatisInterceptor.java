package com.shiyi.config.mybatisplus;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author lizhenwei
 * @date: 2022/5/13 09:52
 * @email: lizhenwei188@foxmail.com
 * @description: 优化Mybatis中Sql打印
 */

@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})}
)
//@Profile({"dev", "test", "prod"})
public class MybatisInterceptor implements Interceptor{

    private static final ThreadLocal<SimpleDateFormat> dateTimeFormat =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public static StringBuilder resultData;

//    private final Logger logger = LoggerFactory.getLogger(MybatisInterceptor.class);


    @Override
    public Object intercept(Invocation invocation) {

        Object result = null;
        List<String> parameters = new ArrayList<>();
        //捕获掉异常，不影响业务
        try {
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            Object parameter = null;
            if (invocation.getArgs().length > 1) {
                parameter = invocation.getArgs()[1];
            }
            String sqlId = mappedStatement.getId();
            BoundSql boundSql = mappedStatement.getBoundSql(parameter);
            Configuration configuration = mappedStatement.getConfiguration();

            long startTime = System.currentTimeMillis();
            try {
                result = invocation.proceed();
            } finally {
                long endTime = System.currentTimeMillis();
                long sqlCostTime = endTime - startTime;
                String sql = this.getSql(configuration, boundSql, parameters);
                this.formatSqlLog(sqlId, sql, sqlCostTime, result, parameters);
            }
            return result;
        } catch (Exception e) {
            return result;
        }
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        // nothing
    }

    /**
     * 获取完整的sql语句
     */
    private String getSql(Configuration configuration, BoundSql boundSql, List<String> parameters) {
        // 输入sql字符串空判断
        String sql = boundSql.getSql();
        if (StringUtils.isEmpty(sql)) {
            return "";
        }
        return formatSql(sql, configuration, boundSql, parameters);
    }

    /**
     * 将占位符替换成参数值
     */
    private String formatSql(String sql, Configuration configuration, BoundSql boundSql, List<String> parameters) {

        //美化sql
        sql = beautifySql(sql);

        //填充占位符, 目前基本不用mybatis存储过程调用,故此处不做考虑
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();

        if (parameterMappings != null) {
            extracted(configuration, boundSql, parameters, parameterObject, parameterMappings, typeHandlerRegistry);
        }

        for (String value : parameters) {
            sql = sql.replaceFirst("\\?", value);
        }
        return sql;
    }

    private void extracted(Configuration configuration, BoundSql boundSql, List<String> parameters, Object parameterObject, List<ParameterMapping> parameterMappings, TypeHandlerRegistry typeHandlerRegistry) {

        MetaObject metaObject = parameterObject == null ? null : configuration.newMetaObject(parameterObject);

        for (ParameterMapping parameterMapping : parameterMappings) {
            if (parameterMapping.getMode() != ParameterMode.OUT) {
                //  参数值
                Object value;
                String propertyName = parameterMapping.getProperty();
                //  获取参数名称
                if (boundSql.hasAdditionalParameter(propertyName)) {
                    // 获取参数值
                    value = boundSql.getAdditionalParameter(propertyName);
                } else if (parameterObject == null) {
                    value = null;
                } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    // 如果是单个值则直接赋值
                    value = parameterObject;
                } else {
                    value = metaObject == null ? null : metaObject.getValue(propertyName);
                }

                if (value instanceof Number) {
                    parameters.add(String.valueOf(value));
                } else {
                    StringBuilder builder = new StringBuilder();
                    builder.append("'");
                    if (value instanceof Date) {
                        builder.append(dateTimeFormat.get().format((Date) value));
                    } else if (value instanceof String) {
                        builder.append(value);
                    }
                    builder.append("'");
                    parameters.add(builder.toString());
                }
            }
        }
    }


    /**
     * 格式化sql日志
     */
    private void formatSqlLog(String sqlId, String sql, long costTime, Object obj, List<String> parameters) {
        String[] split = sqlId.split("\\.");
        if (split.length >= 2) {
            sqlId = split[split.length - 2] + "." + split[split.length - 1];
        }
        StringBuilder result = new StringBuilder();
        List list;
        if (obj instanceof List) {
            list = (List) obj;
            int count = list.size();
            result.append("\uD83D\uDC2C ===>     \uD83C\uDFAF Total ===> ").append(count);
        } else if (obj instanceof Integer) {
            result.append("\uD83D\uDC2C ===>     \uD83C\uDFAF Total ===> ").append(obj);
        }

        result.append("      ⏳ SpendTime ===> ").append(costTime).append(" ms");
        result.append("     \uD83D\uDCE8 Params ===> ").append(parameters.toString());

        try {
            MybatisStdOut.resultSet();
            String dateTime = dateTimeFormat.get().format(new Date());
            System.out.println( "\n" + "\uD83D\uDC2C ===> " + "    ⏰ " + dateTime +
                    "  <===🪢️🪢️🪢️🪢️🪢️===>  " + sqlId + " \uD83D\uDD16" + "\n" +
                    sql + "\n" + result + "\n" + resultData);
            MybatisStdOut.resultClean(new StringBuilder());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dateTimeFormat.remove();
        }
    }

    public static String beautifySql(String sql) {
        sql = sql.replaceAll("[\n ]+", " ");
        sql = "\033[34;1;4m" + sql + "\033[0m";
        return sql;
    }
}



