package cc.eamon.open.permission.annotation;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

/**
 * Created by Eamon on 2018/9/29.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
//最高优先级
@Order(Ordered.HIGHEST_PRECEDENCE)
public @interface PermissionLimit {

    /**
     * 权限细节
     * @return 权限细节
     */
    String value() default "";

    String[] limits() default {};

}
