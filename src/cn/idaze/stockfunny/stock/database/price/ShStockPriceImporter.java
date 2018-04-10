/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.price;

import cn.idaze.stockfunny.database.MSSQLConnector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 上海上市公司股价导入器
 *
 * @author HHQ
 */
public class ShStockPriceImporter {

    public static void main(String[] args) {
        ShStockPriceImporter importer = new ShStockPriceImporter();
        String cycle = "D1";
        //System.out.println(importer.getPrices("600708")); //当天无成交的不纳入表中

        if (importer.importData(cycle, importer.getShStockCodes()) == true) {
            System.out.println("导入成功");
            if (importer.transferData(cycle) == true) {
                System.out.println("数据转移成功");
            } else {
                System.out.println("数据转移失败");
            }
        } else {
            System.out.println("导入失败~");
        }
    }

    /**
     * 将临时表数据l_stocktradeinfo转移至最终结果表stocktradeinfo
     *
     * @param cycle 数据周期，目前只支持日线
     * @return
     */
    public boolean transferData(String cycle) {
        if (cycle.equals("D1") == false) {
            System.out.println("目前只支持日线！");
            return false;
        }

        String delSql = "delete a from stocktradeinfo a join l_stocktradeinfo b on a.dte=b.dte and a.cycle=b.cycle and a.stockid=b.stockid and a.adjtype=b.adjtype and a.datasource=b.datasource and b.datasource='SH' and b.cycle=?";
        String insertSql = "insert into stocktradeinfo select * from l_stocktradeinfo where cycle=? and adjtype='N' and datasource='SH'";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        try {
            PreparedStatement delPs = connector.prepareStmt(delSql);
            delPs.setString(1, "D1");
            delPs.addBatch();
            delPs.executeBatch();
            delPs.close();;
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }

        try {
            PreparedStatement insPs = connector.prepareStmt(insertSql);
            insPs.setString(1, "D1");
            insPs.addBatch();
            insPs.executeBatch();
            insPs.close();
            conn.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 导入给定股票代码股价信息至临时表l_stocktradeinfo
     *
     * @param cycle 数据周期，目前只支持日线
     * @param codes 给定股票代码
     * @return
     */
    public boolean importData(String cycle, List<String> codes) {
        if (cycle.equals("D1") == false) {
            System.out.println("目前只支持日线！");
            return false;
        }

        List<StockPrice> prices = new ArrayList<StockPrice>();
        for (String code : codes) {
            StockPrice sp = getPrices(code);
            if (sp.getStockId().equals("000000")) {
                //该股票股价获取失败，跳过
            } else if (sp.getOpen() == 0.0 && sp.getHigh() == 0.0 && sp.getLow() == 0.0 && sp.getVolume() == 0.0 && sp.getTotalMoney() == 0.0) { //股票停牌时开盘价、最高价、最低价、成交量、成交金额均为0
                //该股票当日停牌，跳过
            } else {
                prices.add(sp);
            }
        }

        //String delSql = "delete from l_stocktradeinfo where stockid=? and cycle=? and dte=? and adjtype='N' and datasource='SH'";
        String delSql = "delete from l_stocktradeinfo where cycle=? and adjtype='N' and datasource='SH'";
        String insertSql = "insert into l_stocktradeinfo (stockid, cycle, dte, preclose, [open], "
                + "high, low, [close], risespread, risepercent, volumn, totalmoney, adjtype, datasource, optime) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, getdate())";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        final int batchSize = 1000;
        int count = 0;
        try {
//            conn.setAutoCommit(false); //关闭自动提交。当使用手动事务模式时，必须把SelectMethod 属性的值设置为 Cursor(连接URL中加上SelectMethod= Cursor), 或者确保在连接上只有一个STATEMENT操作
            PreparedStatement delPs = connector.prepareStmt(delSql);
//            for (StockPrice price : prices) {
//                delPs.setString(1, price.getStockId());
//                delPs.setString(2, "D1");
//                delPs.setString(3, price.getDate());
//                delPs.addBatch();
//
//                if (++count % batchSize == 0) {
//                    delPs.executeBatch();
//                    insPs.executeBatch();
//                }
//            }

            delPs.setString(1, "D1");
            delPs.addBatch();
            delPs.executeBatch();// insert remaining records

//            conn.commit();

            delPs.close();
//            conn.setAutoCommit(true); //打开自动提交 
//            conn.close();
        } catch (SQLException sqlex) {
//            try {
//                conn.rollback();
//                conn.commit();
//            } catch (SQLException ex) {
//                ex.printStackTrace();
//                System.out.println("回滚失败！");
//                return false;
//            }
            sqlex.printStackTrace();
            return false;
        }

        count = 0;
        try {
            //conn.setAutoCommit(false); //关闭自动提交。当使用手动事务模式时，必须把SelectMethod 属性的值设置为 Cursor(连接URL中加上SelectMethod= Cursor), 或者确保在连接上只有一个STATEMENT操作
            PreparedStatement insPs = connector.prepareStmt(insertSql);
            for (StockPrice price : prices) {
                insPs.setString(1, price.getStockId());
                insPs.setString(2, "D1");
                insPs.setString(3, price.getDate());
                insPs.setDouble(4, 0.00);
                insPs.setDouble(5, price.getOpen());
                insPs.setDouble(6, price.getHigh());
                insPs.setDouble(7, price.getLow());
                insPs.setDouble(8, price.getClose());
                insPs.setDouble(9, price.getRiseSpread());
                insPs.setDouble(10, price.getRisePercent());
                insPs.setDouble(11, price.getVolume());
                insPs.setDouble(12, price.getTotalMoney());
                insPs.setString(13, "N");
                insPs.setString(14, "SH");
                insPs.addBatch();

                if (++count % batchSize == 0) {
                    insPs.executeBatch();
                }
            }
            insPs.executeBatch();// insert remaining records

//            conn.commit();

            insPs.close();
//            conn.setAutoCommit(true); //打开自动提交 
            conn.close();
        } catch (SQLException sqlex) {
//            try {
//                conn.rollback();
//                conn.commit();
//            } catch (SQLException ex) {
//                ex.printStackTrace();
//                System.out.println("回滚失败！");
//                return false;
//            }
            sqlex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 获取最近上海交易所股票代码列表
     *
     * @return
     */
    public List<String> getShStockCodes() {
        List<String> codes = new ArrayList<String>();
        MSSQLConnector connector = new MSSQLConnector();
        Connection sqlConn = connector.getConnection();
        ResultSet rs = connector.executeQuery("select stockid from stockcorp where dte=(select max(dte) from stockcorp) and shszflag='1'");
        try {
            while (rs.next()) {
                codes.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return codes;
    }

    /**
     * 获取指定日期上海交易所股票代码列表
     *
     * @param dateStr 给定日期
     * @return
     */
    public List<String> getShStockCodes(String dateStr) {
        List<String> codes = new ArrayList<String>();
        MSSQLConnector connector = new MSSQLConnector();
        Connection sqlConn = connector.getConnection();
        ResultSet rs = connector.executeQuery("select stockid from stockcorp where dte='" + dateStr + "' and shszflag='1'");
        try {
            while (rs.next()) {
                codes.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return codes;
    }

    /**
     * 获取给定股票的价格信息
     *
     * @param stockId
     * @return
     */
    private StockPrice getPrices(String stockId) {
        String baseUrlStr1 = "http://yunhq.sse.com.cn:32041/v1/sh1/snap/";
        String baseUrlStr2 = "?callback=jQuery111208947947407669049_1478019828268&select=name%2Clast%2Cchg_rate%2Cchange%2Camount%2Cvolume%2Copen%2Cprev_close%2Cask%2Cbid%2Chigh%2Clow%2Ctradephase&_=1478019828269";
        String baseRefererStr = "http://www.sse.com.cn/assortment/stock/list/info/price/index.shtml?COMPANY_CODE=";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        StockPrice sp = new StockPrice();

        System.out.println("开始获取股票 " + stockId + " 价格信息...");
        try {
            HttpGet httpGet = new HttpGet(baseUrlStr1 + stockId + baseUrlStr2);
            Header header;
            header = new BasicHeader("Referer", baseRefererStr + stockId);
            httpGet.setHeader(header);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
            String brLine;
            StringBuilder sb = new StringBuilder();
            while ((brLine = br.readLine()) != null) {
                sb.append(brLine);
            }
            String corpDetailInfo = sb.toString();
            String corpDetailInfoJson = corpDetailInfo.substring(42, corpDetailInfo.length() - 1); //去除返回串前的"jQuery111208947947407669049_1478019828268("及最后的")"，以拼出合法的json串
            //System.out.println(corpDetailInfoJson);
            JSONObject jsonObj = new JSONObject(corpDetailInfoJson);
            JSONArray jsonArray = jsonObj.getJSONArray("snap");

            sp.setStockId(stockId);
            sp.setDate(jsonObj.getInt("date") + "");
            sp.setPreClose(jsonArray.getDouble(7));
            sp.setOpen(jsonArray.getDouble(6));
            sp.setHigh(jsonArray.getDouble(10));
            sp.setLow(jsonArray.getDouble(11));
            sp.setClose(jsonArray.getDouble(1));
            sp.setRiseSpread(jsonArray.getDouble(3));
            sp.setRisePercent(jsonArray.getDouble(2));
            sp.setVolume(jsonArray.getDouble(5));
            sp.setTotalMoney(jsonArray.getDouble(4));
//            System.out.println("昨收盘：" + jsonArray.getDouble(7));
//            System.out.println("开盘价：" + jsonArray.getDouble(6));
//            System.out.println("最高价：" + jsonArray.getDouble(10));
//            System.out.println("最低价：" + jsonArray.getDouble(11));
//            System.out.println("收盘价：" + jsonArray.getDouble(1));
//            System.out.println("成交量：" + jsonArray.getDouble(5));
//            System.out.println("成交金额：" + jsonArray.getDouble(4));
//            System.out.println("涨跌：" + jsonArray.getDouble(3));
//            System.out.println("涨幅：" + jsonArray.getDouble(2) + "%");

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (JSONException jex) { //如600710就无法显示股价信息，此时JSON转换存在问题
            System.out.println("股票 " + stockId + " 股价获取失败！");
            sp.setStockId("000000");
        }

        return sp;
    }

    class StockPrice {

        //股票代码
        private String stockId;
        //股价日期
        private String date;
        //昨收盘
        private double preClose;
        //开盘
        private double open;
        //最高
        private double high;
        //最低
        private double low;
        //收盘
        private double close;
        //涨跌
        private double riseSpread;
        //涨幅(百分比)
        private double risePercent;
        //成交量
        private double volume;
        //成交额
        private double totalMoney;

        public StockPrice() {
            this.preClose = 0;
            this.open = 0;
            this.high = 0;
            this.low = 0;
            this.close = 0;
            this.riseSpread = 0;
            this.risePercent = 0;
            this.volume = 0;
            this.totalMoney = 0;
        }

        public StockPrice(String stockId, String date, double preClose, double open, double high, double low, double close, double riseSpread, double risePercent, double volume, double totalMoney) {
            this.stockId = stockId;
            this.date = date;
            this.preClose = preClose;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.riseSpread = riseSpread;
            this.risePercent = risePercent;
            this.volume = volume;
            this.totalMoney = totalMoney;
        }

        /**
         * @return the preClose
         */
        public double getPreClose() {
            return preClose;
        }

        /**
         * @param preClose the preClose to set
         */
        public void setPreClose(double preClose) {
            this.preClose = preClose;
        }

        /**
         * @return the open
         */
        public double getOpen() {
            return open;
        }

        /**
         * @param open the open to set
         */
        public void setOpen(double open) {
            this.open = open;
        }

        /**
         * @return the high
         */
        public double getHigh() {
            return high;
        }

        /**
         * @param high the high to set
         */
        public void setHigh(double high) {
            this.high = high;
        }

        /**
         * @return the low
         */
        public double getLow() {
            return low;
        }

        /**
         * @param low the low to set
         */
        public void setLow(double low) {
            this.low = low;
        }

        /**
         * @return the close
         */
        public double getClose() {
            return close;
        }

        /**
         * @param close the close to set
         */
        public void setClose(double close) {
            this.close = close;
        }

        /**
         * @return the riseSpread
         */
        public double getRiseSpread() {
            return riseSpread;
        }

        /**
         * @param riseSpread the riseSpread to set
         */
        public void setRiseSpread(double riseSpread) {
            this.riseSpread = riseSpread;
        }

        /**
         * @return the risePercent
         */
        public double getRisePercent() {
            return risePercent;
        }

        /**
         * @param risePercent the risePercent to set
         */
        public void setRisePercent(double risePercent) {
            this.risePercent = risePercent;
        }

        /**
         * @return the volume
         */
        public double getVolume() {
            return volume;
        }

        /**
         * @param volume the volume to set
         */
        public void setVolume(double volume) {
            this.volume = volume;
        }

        /**
         * @return the totalMoney
         */
        public double getTotalMoney() {
            return totalMoney;
        }

        /**
         * @param totalMoney the totalMoney to set
         */
        public void setTotalMoney(double totalMoney) {
            this.totalMoney = totalMoney;
        }

        @Override
        public String toString() {
            return "股票：" + stockId + "(" + date + ")\n昨收盘：" + preClose + "\n开盘：" + open + "\n最高价：" + high
                    + "\n最低价：" + low + "\n收盘：" + close + "\n涨跌：" + riseSpread
                    + "\n涨幅：" + risePercent + "%\n成交量：" + volume + "\n成交金额：" + totalMoney + "\n";
        }

        /**
         * @return the stockId
         */
        public String getStockId() {
            return stockId;
        }

        /**
         * @param stockId the stockId to set
         */
        public void setStockId(String stockId) {
            this.stockId = stockId;
        }

        /**
         * @return the date
         */
        public String getDate() {
            return date;
        }

        /**
         * @param date the date to set
         */
        public void setDate(String date) {
            this.date = date;
        }
    }
}
