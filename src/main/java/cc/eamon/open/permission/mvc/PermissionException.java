package cc.eamon.open.permission.mvc;


/**
 * Created by Eamon on 2018/10/2.
 */
public class PermissionException extends Exception {

    public PermissionException(String msg, Object... args) {
        super(String.format(msg, args));
    }

}

