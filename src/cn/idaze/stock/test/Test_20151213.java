/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

/**
 *
 * @author HHQ
 */
public class Test_20151213 {
    
    public static void main(String[] args) throws Exception{
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream("E:/tmp/export/SH#500038.txt"));
        int p = (bis.read() << 8) + bis.read();
        switch (p){
            case 0xefbb: System.out.println("UTF-8");
            case 0xfffe: System.out.println("Unicode");
            case 0xfeff: System.out.println("UTF-16BE");
            default: System.out.println("GBK");
        }
    }
    
}
