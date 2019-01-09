package cc.eamon.open.permission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Eamon on 2018/9/29.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {

    /**
     * 权限名称
     * @return 权限名称
     */
    String value() default "";

    /**
     * 权限名称
     * @return 权限名称
     */
    String[] limits() default {};

}
