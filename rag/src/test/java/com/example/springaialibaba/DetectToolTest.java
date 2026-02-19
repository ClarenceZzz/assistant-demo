package com.example.springaialibaba;

import com.example.springaialibaba.tool.WeatherTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

public class DetectToolTest {

    @Test
    public void testProxyAnnotation() {
        WeatherTool target = new WeatherTool();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true); // CGLIB
        Object proxy = proxyFactory.getProxy();

        System.out.println("Proxy class: " + proxy.getClass().getName());
        System.out.println("Target class: " + target.getClass().getName());

        Method[] methods = proxy.getClass().getMethods();
        boolean found = false;
        for (Method m : methods) {
            if (m.getName().equals("getWeather")) {
                Tool tool = m.getAnnotation(Tool.class);
                if (tool != null) {
                    System.out.println("Found @Tool on proxy method: " + m.getName());
                    found = true;
                } else {
                    System.out.println("Did NOT find @Tool on proxy method: " + m.getName());
                }
            }
        }
        
        if (!found) {
            System.out.println("Likely cause: @Tool annotation not visible on CGLIB proxy method.");
        }
    }
}
