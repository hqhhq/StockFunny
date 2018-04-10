/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Yahoo股票数据获取
 * 
 * @author hhq
 */
public class YahooStockFetcher {

    //Yahoo历史数据下载URL
    private static final String YAHOO_FINANCE_HISTORY_URL = "http://table.finance.yahoo.com/table.csv?";
    //Yahoo实时数据下载URL
    private static final String YAHOO_FINANCE_REAL_URL = "http://download.finance.yahoo.com/d/quotes.csv?";

    public YahooStockFetcher() {
    }

    /**
     * 将获取的数据加载到数据库中
     * 
     * @param stock
     * @return 
     */
    public boolean getHistoryData2db(String stock) {
        return true;
    }

    /**
     * 将获取的数据转为字符串，并返回
     * 
     * @param stock 股票代码
     * @return 
     * @throws Exception
     */
    public String getHistoryData2Str(String stock) throws Exception {
        return getHistoryData2Str(stock, "");
    }

    /**
     * 将获取的数据转为字符串，并返回(周期默认为日)
     * 
     * @param stock 股票代码
     * @param startYear 开始年份
     * @param startMonth 开始月份
     * @param startDay 开始日期
     * @param endYear 结束年份
     * @param endMonth 结束月份
     * @param endDay 结束日期
     * @return 
     * @throws Exception
     */
    public String getHistoryData2Str(String stock,
            int startYear, int startMonth, int startDay,
            int endYear, int endMonth, int endDay) throws Exception {
        String option = "&a=" + (startMonth - 1) + "&b=" + startDay + "&c=" + startYear
                + "&d=" + (endMonth - 1) + "&e=" + endDay + "&f=" + endYear + "&g=d";

        return getHistoryData2Str(stock, option);
    }

    /**
     * 将获取的数据转为字符串，并返回
     * 
     * @param stock 股票代码
     * @param startYear 开始年份
     * @param startMonth 开始月份
     * @param startDay 开始日期
     * @param endYear 结束年份
     * @param endMonth 结束月份
     * @param endDay 结束日期
     * @param period 周期，d：日；w：周；m：月；v：dividends only
     * @return 
     * @throws Exception
     */
    public String getHistoryData2Str(String stock,
            int startYear, int startMonth, int startDay,
            int endYear, int endMonth, int endDay, String period) throws Exception {
        String option = "&a=" + (startMonth - 1) + "&b=" + startDay + "&c=" + startYear
                + "&d=" + (endMonth - 1) + "&e=" + endDay + "&f=" + endYear + "&g=" + period;

        return getHistoryData2Str(stock, option);
    }

    /**
     * 将获取的数据转为字符串，并返回(末尾含一空行)
     * 
     * @param stock 股票代码
     * @param option 各选项列拼接成的字符串
     * @return 
     * @throws Exception
     */
    private String getHistoryData2Str(String stock, String option) throws Exception {
        URL MyURL = null;
        URLConnection con = null;
        InputStreamReader ins = null;
        BufferedReader in = null;
        StringBuilder sb = new StringBuilder();


        MyURL = new URL(YAHOO_FINANCE_HISTORY_URL + "s=" + stock + option);
        con = MyURL.openConnection();
        ins = new InputStreamReader(con.getInputStream(), "UTF-8");
        in = new BufferedReader(ins);

        // 跳过标题行
        String line = in.readLine();
        if (line == null) {
            return "";
        }

        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception ex) {
        }

        return sb.toString();
    }
    
    /**
     * 将获取的数据加载到数据库中
     * 
     * @param stock
     * @return
     * @throws Exception 
     */
    public boolean getHistoryData2Db(String stock) throws Exception {
        return getHistoryData2Db(stock, "");
    }
    
    /**
     * 将获取的数据加载到数据库中，周期默认为日
     * 
     * @param stock
     * @param startYear
     * @param startMonth
     * @param startDay
     * @param endYear
     * @param endMonth
     * @param endDay
     * @return
     * @throws Exception 
     */
    public boolean getHistoryData2Db(String stock,
            int startYear, int startMonth, int startDay,
            int endYear, int endMonth, int endDay) throws Exception {
        String option = "&a=" + (startMonth - 1) + "&b=" + startDay + "&c=" + startYear
                + "&d=" + (endMonth - 1) + "&e=" + endDay + "&f=" + endYear + "&g=d";

        return getHistoryData2Db(stock, option);
    }
    
    /**
     * 将获取的数据加载到数据库中
     * 
     * @param stock
     * @param startYear
     * @param startMonth
     * @param startDay
     * @param endYear
     * @param endMonth
     * @param endDay
     * @param period
     * @return
     * @throws Exception 
     */
    public boolean getHistoryData2Db(String stock,
            int startYear, int startMonth, int startDay,
            int endYear, int endMonth, int endDay, String period) throws Exception {
        String option = "&a=" + (startMonth - 1) + "&b=" + startDay + "&c=" + startYear
                + "&d=" + (endMonth - 1) + "&e=" + endDay + "&f=" + endYear + "&g=" + period;

        return getHistoryData2Db(stock, option);
    }
    
    private boolean getHistoryData2Db(String stock, String option) throws Exception {
        URL MyURL = null;
        URLConnection con = null;
        InputStreamReader ins = null;
        BufferedReader in = null;
        StringBuilder sb = new StringBuilder();


        MyURL = new URL(YAHOO_FINANCE_HISTORY_URL + "s=" + stock + option);
        con = MyURL.openConnection();
        ins = new InputStreamReader(con.getInputStream(), "UTF-8");
        in = new BufferedReader(ins);

        // 跳过标题行
        String line = in.readLine();
        if (line == null) {
            return true;
        }

        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception ex) {
        }

        return false;
    }

    public static void main(String[] args) throws Exception {
        YahooStockFetcher ysf = new YahooStockFetcher();
        //System.out.println(ysf.getHistoryData2Str("300003.sz"));
        //System.out.println(ysf.getHistoryData2Str("601288.ss", 2011, 9, 1, 2011, 12, 31, "d"));
        //System.out.println(ysf.getHistoryData2Str("300003.sz", 2009, 1, 1, 2010, 5, 1, "d"));
    }
}