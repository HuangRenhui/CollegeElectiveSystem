package com.college.elective.common;

/**
 * 待实现功能的统一占位处理。
 *
 * <p>选课模块与成绩模块的 Service 实现类尚未编写。为使其未实现期间
 * 应用仍可正常启动与运行，相关 Controller 通过本工具在调用时返回
 * 明确的业务提示，而非影响 Spring 容器启动。</p>
 *
 * <p>实现完成后，将 Controller 中的 {@link #unsupported(String)} 调用
 * 替换为对 Service 的实际调用即可。</p>
 *
 * @see ResultCode#FEATURE_NOT_IMPLEMENTED
 */
public final class PendingFeature {

    private PendingFeature() {
    }

    /** 实现指引文档路径，用于在提示信息中引导开发者 */
    private static final String GUIDE = "docs/待实现功能.md";

    /**
     * 抛出「功能尚未实现」业务异常。
     *
     * @param featureName 功能名称，如「学生选课」
     */
    public static BusinessException unsupported(String featureName) {
        return new BusinessException(ResultCode.FEATURE_NOT_IMPLEMENTED,
                featureName + " 尚未实现，请参考 " + GUIDE);
    }

    /**
     * 返回「功能尚未实现」的响应结果。
     *
     * @param featureName 功能名称
     * @param <T>         业务数据类型
     * @return 统一错误响应
     */
    public static <T> Result<T> result(String featureName) {
        return Result.error(ResultCode.FEATURE_NOT_IMPLEMENTED,
                featureName + " 尚未实现，请参考 " + GUIDE);
    }

    /**
     * 获取 Service 实现 Bean，不存在时抛出「功能尚未实现」异常。
     *
     * @param provider    Spring 的 ObjectProvider
     * @param featureName 功能名称
     * @param <T>         Service 类型
     * @return Service 实例
     */
    public static <T> T require(org.springframework.beans.factory.ObjectProvider<T> provider, String featureName) {
        T service = provider.getIfAvailable();
        if (service == null) {
            throw unsupported(featureName);
        }
        return service;
    }

    /**
     * 获取可用的 Service 实现 Bean：Bean 缺失或为骨架实现时返回 {@code null}。
     *
     * <p>供 Controller 在调用前判空，从而返回统一的「尚未实现」业务提示。</p>
     *
     * @param provider Spring 的 ObjectProvider
     * @param <T>      Service 类型
     * @return 可用的 Service 实例；未就绪时返回 {@code null}
     */
    public static <T> T readyOrNull(org.springframework.beans.factory.ObjectProvider<T> provider) {
        T service = provider.getIfAvailable();
        if (service instanceof PendingImplementation) {
            return null;
        }
        return service;
    }
}
