package cc.eamon.open.permission.mvc;

import java.io.IOException;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.eamon.open.permission.annotation.Permission;
import cc.eamon.open.permission.annotation.PermissionLimit;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;


/**
 * @author Eamon
 * ʕ•ﻌ•ʔ 留给小纯洁的ajax权限header
 */
public class PermissionInterceptor implements HandlerInterceptor {

    private PermissionChecker checker;

    public PermissionChecker getChecker() {
        return checker;
    }

    public void setChecker(PermissionChecker checker) {
        this.checker = checker;
    }

    public PermissionInterceptor(PermissionChecker checker) {
        this.checker = checker;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (checker.preHandle(request,response,handler)){
            return permissionControl(request, response, handler);
        }else{
            return false;
        }
    }

    /**
     * 角色权限控制访问
     */
    private boolean permissionControl(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
            // Object target = hm.getBean();
            Class<?> clazz = hm.getBeanType();
            Method m = hm.getMethod();
            try {
                if (clazz != null && m != null) {
                    boolean isClzAnnotation = clazz.isAnnotationPresent(Permission.class);
                    boolean isMethodAnnotation = m.isAnnotationPresent(PermissionLimit.class);

                    Permission pc;
                    PermissionLimit rc;
                    String methodPermissionName;

                    if (isClzAnnotation){
                        pc = clazz.getAnnotation(Permission.class);

                    }else {
                        return true;
                    }


                    if (isMethodAnnotation) {
                        rc = m.getAnnotation(PermissionLimit.class);
                        if (rc.name().equals("")){
                            methodPermissionName = pc.value().toLowerCase() + "_" + m.getName();
                        }else {
                            methodPermissionName = pc.value().toLowerCase() + "_" + rc.name();
                        }
                    }else {
                        return true;
                    }

                    if (!checker.check(request, response, methodPermissionName, rc.value())){
                        throw new Exception();
                    }
                }
            } catch (Exception e) {
                response.setCharacterEncoding("utf-8");
                response.setContentType("application/json; charset=utf-8");
                response.setStatus(200);
                try {
                    Object o = checker.handleException(response, e);
                    if (o != null){
                        response.getWriter().write(o.toString());
                    }
                    return false;
                } catch (IOException e1) {
                    e1.printStackTrace();
                    return false;
                }
            }
        }
        return true;

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        checker.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        checker.afterCompletion(request, response, handler, ex);
    }



}
