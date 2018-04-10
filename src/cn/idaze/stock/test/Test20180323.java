/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author hhq
 */
public class Test20180323 {
    public static void main(String[] args) {
        Calendar calendar = Calendar.getInstance();  
        calendar.setTime(new Date());  
        calendar.add(Calendar.DAY_OF_MONTH, -23);  
        System.out.println(new SimpleDateFormat("yyyyMMdd").format(calendar.getTime()));  
    }
}
