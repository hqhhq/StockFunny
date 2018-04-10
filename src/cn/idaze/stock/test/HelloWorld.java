/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

/**
 *
 * @author hhq
 */
public class HelloWorld {
    public String name = "Cathy";
    public int age = 4;
    
    public boolean printHelloWorld(){
        System.out.println("Hello, world!");
        return true;
    }
    
    public boolean printInfo(){
        System.out.println("name=" + name + ", age=" + age);
        return false;
    }
    
}
