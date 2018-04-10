/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

/**
 *
 * @author HHQ
 */
public class Test_20140721_02 {
    public static void main(String[] args) {
        String[] array = "0021  中国   成功".split("\\s+");
        System.out.println(array.length);
        for(String s: array){
            System.out.println("x:"+s);
        }
    }
}
