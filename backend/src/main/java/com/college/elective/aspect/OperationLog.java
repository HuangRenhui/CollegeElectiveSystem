package com.college.elective.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解，标注在 Controller 方法上即可自动记录操作日志。
 *
 * <pre>{@code
 * @OperationLog(module = "课程管理", operation = "新增课程")
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 业务模块名称 */
    String module() default "";

    /** 操作描述 */
    String operation() default "";

    /** 是否记录请求参数 */
    boolean saveParam() default true;
}
