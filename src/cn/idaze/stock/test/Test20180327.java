/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author hhq
 */
public class Test20180327 {
    public static void main(String[] args) {
        Calendar calendar = Calendar.getInstance(); 
        calendar.set(2018, 2, 27);
        System.out.println(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
        
        String dateStr = "20180327";
        calendar.set(Integer.valueOf(dateStr.trim().substring(0, 4)), Integer.valueOf(dateStr.trim().substring(4, 6))-1, Integer.valueOf(dateStr.trim().substring(6,8)));
        System.out.println(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
    }
}
