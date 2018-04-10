/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author hhq
 */
public class Test {

    public static void main(String[] args) throws Exception {
        String ida="1";
        System.out.println((""+(1000000+Integer.valueOf(ida))).substring(1));
        URL MyURL = null;
        URLConnection con = null;
        InputStreamReader ins = null;
        BufferedReader in = null;
        try {
            MyURL = new URL("http://table.finance.yahoo.com/table.csv?s=300350.sz&f=l2l3");
            con = MyURL.openConnection();
            ins = new InputStreamReader(con.getInputStream(), "UTF-8");
            in = new BufferedReader(ins);
            // 标题行
            String newLine = in.readLine();

            // 历史数据，不含今天数据
            while ((newLine = in.readLine()) != null) {
               System.out.println(newLine);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
        }

    }
}
