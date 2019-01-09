package cc.eamon.open.permission;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * Author: eamon
 * Email: eamon@eamon.cc
 * Time: 2019-01-05 13:28:50
 */
public class LimitInit {

    private static Map<String, String> limitMap = new HashMap<>();


    static {
        Properties prop = new Properties();
        try {
            ClassLoader classLoader = LimitInit.class.getClassLoader();
            /**
             getResource()方法会去classpath下找这个文件，获取到url resource, 得到这个资源后，调用url.getFile获取到 文件 的绝对路径
             */
            URL url = classLoader.getResource("limit.properties");
            /**
             * url.getFile() 得到这个文件的绝对路径
             */
            assert url != null;
            prop.load(new InputStreamReader(url.openStream(), "UTF-8"));
            prop.stringPropertyNames().forEach((e) -> {
                for (String item : prop.getProperty(e).replaceAll(" ", "").replaceAll("\t", "").trim().split(",")) {
                    limitMap.put(e.toUpperCase() + "_" + item.toUpperCase(), e.toLowerCase() + "_" + item.toLowerCase());
                }
            });
            System.out.println(limitMap);
        } catch (Exception e) {
            System.err.println("limit.properties IO exception");
            e.printStackTrace();
        }
    }

    public static Map<String, String> getLimitMap() {
        return limitMap;
    }

}
