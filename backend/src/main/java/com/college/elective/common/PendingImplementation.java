package com.college.elective.common;

/**
 * 待实现标记接口。
 *
 * <p>用于标识「仅有骨架、业务逻辑尚未实现」的 Service 实现类。Controller 在调用
 * 此类服务前会先判断标记，直接返回统一的「尚未实现」业务提示，
 * 避免骨架方法抛出的异常被包装成 500 系统错误。</p>
 *
 * <p>当实现类完成全部业务逻辑后，<b>移除本接口的实现声明</b>即可自动接入真实逻辑。</p>
 *
 * @see PendingFeature
 */
public interface PendingImplementation {
}
