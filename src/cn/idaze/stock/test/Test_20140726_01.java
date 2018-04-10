/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author HHQ
 */
public class Test_20140726_01 {
    public static void main(String[] args) {
         try {
            URLConnection conn = new URL("http://www.szse.cn/main/marketdata/hqcx/zqhq_history?ACTIONID=7&AJAX=AJAX-TRUE&CATALOGID=1815_stock&TABKEY=tab1&txtDMorJC=000790&txtBeginDate=2010-07-23&txtEndDate=2010-07-23&SEARCH=TRUE").openConnection();
            
            InputStream in = conn.getInputStream();
            String s = IOUtils.toString(in, "gb2312");
             System.out.println(s);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
