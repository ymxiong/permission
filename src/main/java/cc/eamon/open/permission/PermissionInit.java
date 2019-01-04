package cc.eamon.open.permission;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;


public class PermissionInit {

    private String name;

    private HashSet<String> roles = new HashSet<>();

    private static HashSet<String> roleSet = new HashSet<>();

    private static HashMap<String, PermissionInit> nameToPermissionMap = new HashMap<>();

    static {
        Properties prop = new Properties();
        try {
            ClassLoader classLoader = PermissionInit.class.getClassLoader();
            /**
             getResource()方法会去classpath下找这个文件，获取到url resource, 得到这个资源后，调用url.getFile获取到 文件 的绝对路径
             */
            URL url = classLoader.getResource("permission.properties");
            /**
             * url.getFile() 得到这个文件的绝对路径
             */
            assert url != null;
            prop.load(new InputStreamReader(url.openStream(), "UTF-8"));
            prop.stringPropertyNames().forEach((e) -> {
                PermissionInit permission = new PermissionInit();
                permission.name = e.trim();
                permission.roles.addAll(Arrays.asList(prop.getProperty(e).replaceAll(" ","").replaceAll("\t","").trim().split(",")));
                roleSet.addAll(Arrays.asList(prop.getProperty(e).trim().split(",")));
                nameToPermissionMap.put(permission.name, permission);
            });
        } catch (Exception e) {
            System.err.println("permission.properties IO exception");
            e.printStackTrace();
        }
    }

    public static boolean checkRolePermission(String name, String role) {
        PermissionInit permission = nameToPermissionMap.get(name.toUpperCase());
        if (permission == null) return false;
        if (permission.roles.contains(role.trim()))
            return true;
        return false;
    }

    public static HashMap<String, PermissionInit> getNameToPermissionMap(){
        return nameToPermissionMap;
    }

    public static HashSet<String> getRoleSet() { return roleSet; }

}
