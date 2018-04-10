/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hhq
 */
public class TestReflect {

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        String className = "cn.idaze.stock.test.HelloWorld";
        Class<?> clazz = Class.forName(className);
        Class<?> clazzNew = PrintMapValue.class;
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");

        // 调用demo2class类中的getId方法
        Method method1 = clazz.getMethod("printHelloWorld");
        Method method2 = clazz.getMethod("printInfo");
        Method method3 = clazzNew.getMethod("printMapValue", Map.class);
        Object b1 = method1.invoke(clazz.newInstance());
        Object b2 = method2.invoke(clazz.newInstance());
        Object b3 = method3.invoke(clazzNew.newInstance(), map);
        
        if ((Boolean)b2.equals(Boolean.TRUE) == true){
            System.out.println("good");
        }else{
            System.out.println("bad");
        }
        System.out.println("b1=" + b1.toString());
        System.out.println("b2=" + b2.toString());

        try {
            Field f = clazz.getDeclaredField("name");
            //当属性是私有的时候需要f.setAccessible(true)，本处可以不要
            f.setAccessible(true);
            Object o = f.get(clazz.newInstance());
            System.out.println(o.toString());

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

}
