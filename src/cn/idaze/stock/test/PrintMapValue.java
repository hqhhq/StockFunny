/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hhq
 */
public class PrintMapValue {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        
        PrintMapValue pmv = new PrintMapValue();
        pmv.printMapValue(map);
    }
    
    public void printMapValue(Map<String, String> map){
        for(String key : map.keySet()){
            System.out.println("key:" + key + ", value:" + map.get(key));
        }
    }
    
}
