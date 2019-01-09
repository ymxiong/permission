package cc.eamon.open.permission.mvc;

import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface PermissionChecker {

    /**
     * 在权限处理前的处理
     * @param request Http请求
     * @param response Http响应
     * @param handler 控制器
     * @return 是否拦截
     * @throws Exception 抛出错误
     */
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

    /**
     * 在权限处理后的处理
     * @param request Http请求
     * @param response Http响应
     * @param handler 控制器
     * @param modelAndView Model
     * @throws Exception 抛出错误
     */
    void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception;

    /**
     * 完成后的处理
     * @param request Http请求
     * @param response Http响应
     * @param handler 控制器
     * @param ex 错误
     * @throws Exception 抛出错误
     */
    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception;

    /**
     * 错误控制
     * @param response Http响应
     * @param ex 错误
     * @return 错误管理
     */
    Object handleException(HttpServletResponse response, Exception ex);

    /**
     * 检查权限信息
     * @param request Http请求
     * @param response Http响应
     * @return 是否拦截
     * @throws Exception
     */
    boolean check(HttpServletRequest request, HttpServletResponse response, Object... args) throws Exception;


}
