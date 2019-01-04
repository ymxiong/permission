package cc.eamon.open.permission.mvc;

import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface PermissionChecker {

    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

    void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception;

    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception;

    boolean check(HttpServletRequest request, HttpServletResponse response, String methodName, String roleLimit) throws Exception;

    Object handleException(HttpServletResponse response, Exception ex);
}
