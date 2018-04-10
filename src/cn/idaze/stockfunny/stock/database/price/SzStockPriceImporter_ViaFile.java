/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.price;

import cn.idaze.stockfunny.database.MSSQLConnector;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

/**
 * @deprecated 
 * 
 * 通过将获取的股价信息暂存文件再导入的方式抓取股价信息。这是因为由于网络或交易所网站拒绝连接等原因会较大概率导致下载失败，
 * 因此通过将获取的每只股票的股价信息暂存为文件，则可以通过只重新执行失败的任务而完成整个数据下载工作。
 *
 * @author HHQ
 */
public class SzStockPriceImporter_ViaFile {

    public static String pricePath = "E:/tmp/price/";

    public static void main(String[] args) {
        SzStockPriceImporter_ViaFile importer = new SzStockPriceImporter_ViaFile();
        String cycle = "D1";

        Map<String, Boolean> map = new HashMap<String, Boolean>();
        List<String> codes = importer.getSzStockCodes();
        for (String code : codes) {
            map.put(code, Boolean.FALSE);
        }

        while (codes.size() > 0) {
            for (String code : codes) {
                StockPrice sp = importer.getPrices(code);
                if (sp == null || sp.getStockId().equals("000000")) {
                    //该股票股价获取失败，跳过
                    map.put(code, Boolean.FALSE);
                } else if (sp.getOpen() == 0.0 && sp.getHigh() == 0.0 && sp.getLow() == 0.0 && sp.getVolume() == 0.0 && sp.getTotalMoney() == 0.0) { //股票停牌时开盘价、最高价、最低价、成交量、成交金额均为0
                    //该股票当日停牌，跳过
                    map.put(code, Boolean.TRUE);
                } else {
                    importer.writePrice(sp);
                    map.put(code, Boolean.TRUE);
                }
            }
            codes = importer.getFailedDownloadStockCodes(map);
        }
    }

    /**
     * 将股价信息写入文件
     *
     * @param sp
     * @return
     */
    public boolean writePrice(StockPrice sp) {
        try {
            File file = new File(pricePath + "/" + sp.getDate());
            file.mkdir();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pricePath + sp.getDate() + "/" + sp.getDate() + "_" + sp.getStockId()), "GBK"));
            out.write(sp.getStockId() + "," + sp.getDate() + "," + sp.getPreClose() + "," + sp.getOpen() + "," + sp.getHigh() + ","
                    + sp.getLow() + "," + sp.getClose() + "," + sp.getRiseSpread() + "," + sp.getRisePercent() + ","
                    + sp.getVolume() + "," + sp.getTotalMoney());
            out.newLine();
            for (StockCloseEntrustDetail detail : sp.getBuyCloseEntrustDetails()) {
                out.write(detail.getBuySellFlag() + "," + detail.getSequence() + "," + detail.getPrice() + "," + detail.getVolume());
                out.newLine();
            }
            for (StockCloseEntrustDetail detail : sp.getSellCloseEntrustDetails()) {
                out.write(detail.getBuySellFlag() + "," + detail.getSequence() + "," + detail.getPrice() + "," + detail.getVolume());
                out.newLine();
            }

            out.flush();
            out.close();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 返回未成功下载股价数据的股票代码
     *
     * @param codes
     * @return
     */
    public List<String> getFailedDownloadStockCodes(Map<String, Boolean> codes) {
        List<String> failedCodes = new ArrayList<String>();
        Set<String> codeSet = codes.keySet();
        Iterator<String> it = codeSet.iterator();
        while (it.hasNext()) {
            String code = it.next();
            if (codes.get(code) == Boolean.FALSE) {
                failedCodes.add(code);
            }
        }
        return failedCodes;
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

        String delTradeInfoSql = "delete a from stocktradeinfo a join l_stocktradeinfo b on a.dte=b.dte and a.cycle=b.cycle and a.stockid=b.stockid and a.adjtype=b.adjtype and a.datasource=b.datasource and b.datasource='SZ' and b.cycle=?";
        String insertTradeInfoSql = "insert into stocktradeinfo select * from l_stocktradeinfo where cycle=? and adjtype='N' and datasource='SZ'";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        try {
            PreparedStatement delPs = connector.prepareStmt(delTradeInfoSql);
            delPs.setString(1, "D1");
            delPs.addBatch();
            delPs.executeBatch();
            delPs.close();;
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }

        try {
            PreparedStatement insPs = connector.prepareStmt(insertTradeInfoSql);
            insPs.setString(1, "D1");
            insPs.addBatch();
            insPs.executeBatch();
            insPs.close();
//            conn.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }

        String delEntrustDetailSql = "delete a from stockbuyselldetail a join l_stockbuyselldetail b on a.dte=b.dte and a.stockid=b.stockid and a.datasource=b.datasource and b.datasource='SZ'";
        String indertEntrustDetailSql = "insert into stocktradeinfo select * from l_stocktradeinfo where datasource='SZ'";
        try {
            PreparedStatement delEntrustDetailPs = connector.prepareStmt(delEntrustDetailSql);
            delEntrustDetailPs.executeBatch();
            delEntrustDetailPs.close();;
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }

        try {
            PreparedStatement insEntrustDetailPs = connector.prepareStmt(indertEntrustDetailSql);
            insEntrustDetailPs.executeBatch();
            insEntrustDetailPs.close();
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

        //String delTradeInfoSql = "delete from l_stocktradeinfo where stockid=? and cycle=? and dte=? and adjtype='N' and datasource='SZ'";
        String delTradeInfoSql = "delete from l_stocktradeinfo where cycle=? and adjtype='N' and datasource='SZ'";
        String insertTradeInfoSql = "insert into l_stocktradeinfo (stockid, cycle, dte, preclose, [open], "
                + "high, low, [close], risespread, risepercent, volumn, totalmoney, adjtype, datasource, optime) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, getdate())";
        String delEntrustDetailSql = "truncate table l_stockbuyselldetail";
        String indertEntrustDetailSql = "insert into l_stockbuyselldetail(stockid, dte, buysellflag, seq, price, volume, datasource, optime) "
                + "values (?, ?, ?, ?, ?, ?, ?, getdate())";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        final int batchSize = 1000;
        int count = 0;
        try {
//            conn.setAutoCommit(false); //关闭自动提交。当使用手动事务模式时，必须把SelectMethod 属性的值设置为 Cursor(连接URL中加上SelectMethod= Cursor), 或者确保在连接上只有一个STATEMENT操作
            PreparedStatement delTradeInfoPs = connector.prepareStmt(delTradeInfoSql);
//            for (StockPrice price : prices) {
//                delTradeInfoPs.setString(1, price.getStockId());
//                delTradeInfoPs.setString(2, "D1");
//                delTradeInfoPs.setString(3, price.getDate());
//                delTradeInfoPs.addBatch();
//
//                if (++count % batchSize == 0) {
//                    delTradeInfoPs.executeBatch();
//                    insTradeInfoPs.executeBatch();
//                }
//            }

            delTradeInfoPs.setString(1, "D1");
            delTradeInfoPs.addBatch();
            delTradeInfoPs.executeBatch();// insert remaining records

//            conn.commit();

            delTradeInfoPs.close();
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
        System.out.println("成功删除交易信息~");


        count = 0;
        try {
            //conn.setAutoCommit(false); //关闭自动提交。当使用手动事务模式时，必须把SelectMethod 属性的值设置为 Cursor(连接URL中加上SelectMethod= Cursor), 或者确保在连接上只有一个STATEMENT操作
            PreparedStatement insTradeInfoPs = connector.prepareStmt(insertTradeInfoSql);
            for (StockPrice price : prices) {
                insTradeInfoPs.setString(1, price.getStockId());
                insTradeInfoPs.setString(2, "D1");
                insTradeInfoPs.setString(3, price.getDate());
                insTradeInfoPs.setDouble(4, 0.00);
                insTradeInfoPs.setDouble(5, price.getOpen());
                insTradeInfoPs.setDouble(6, price.getHigh());
                insTradeInfoPs.setDouble(7, price.getLow());
                insTradeInfoPs.setDouble(8, price.getClose());
                insTradeInfoPs.setDouble(9, price.getRiseSpread());
                insTradeInfoPs.setDouble(10, price.getRisePercent());
                insTradeInfoPs.setDouble(11, price.getVolume());
                insTradeInfoPs.setDouble(12, price.getTotalMoney());
                insTradeInfoPs.setString(13, "N");
                insTradeInfoPs.setString(14, "SZ");
                insTradeInfoPs.addBatch();

                if (++count % batchSize == 0) {
                    insTradeInfoPs.executeBatch();
                }
            }
            insTradeInfoPs.executeBatch();// insert remaining records

//            conn.commit();

            insTradeInfoPs.close();
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
        System.out.println("成功导入交易信息~");

        try {
            PreparedStatement delEntrustDetailPs = connector.prepareStmt(delEntrustDetailSql);
            delEntrustDetailPs.executeBatch();
            delEntrustDetailPs.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }
        System.out.println("成功删除收市买卖委托信息~");

        count = 0;
        try {
            //conn.setAutoCommit(false); //关闭自动提交。当使用手动事务模式时，必须把SelectMethod 属性的值设置为 Cursor(连接URL中加上SelectMethod= Cursor), 或者确保在连接上只有一个STATEMENT操作
            PreparedStatement insEntrustDetailPs = connector.prepareStmt(indertEntrustDetailSql);
            for (StockPrice price : prices) {
                for (StockCloseEntrustDetail sced : price.getBuyCloseEntrustDetails()) {    //买委托
                    insEntrustDetailPs.setString(1, price.getStockId());
                    insEntrustDetailPs.setString(2, price.getDate());
                    insEntrustDetailPs.setString(3, sced.getBuySellFlag());
                    insEntrustDetailPs.setString(4, sced.getSequence());
                    insEntrustDetailPs.setDouble(5, sced.getPrice());
                    insEntrustDetailPs.setDouble(6, sced.getVolume());
                    insEntrustDetailPs.setString(7, "SZ");
                    insEntrustDetailPs.addBatch();
                    if (++count % batchSize == 0) {
                        insEntrustDetailPs.executeBatch();
                    }
                }
                for (StockCloseEntrustDetail sced : price.getSellCloseEntrustDetails()) {   //卖委托
                    insEntrustDetailPs.setString(1, price.getStockId());
                    insEntrustDetailPs.setString(2, price.getDate());
                    insEntrustDetailPs.setString(3, sced.getBuySellFlag());
                    insEntrustDetailPs.setString(4, sced.getSequence());
                    insEntrustDetailPs.setDouble(5, sced.getPrice());
                    insEntrustDetailPs.setDouble(6, sced.getVolume());
                    insEntrustDetailPs.setString(7, "SZ");
                    insEntrustDetailPs.addBatch();
                    if (++count % batchSize == 0) {
                        insEntrustDetailPs.executeBatch();
                    }
                }
            }

            insEntrustDetailPs.executeBatch();// insert remaining records
            insEntrustDetailPs.close();
            conn.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }
        System.out.println("成功插入收市买卖委托信息~");

        return true;
    }

    /**
     * 获取最近深圳交易所股票代码列表
     *
     * @return
     */
    public List<String> getSzStockCodes() {
        List<String> codes = new ArrayList<String>();
        MSSQLConnector connector = new MSSQLConnector();
        Connection sqlConn = connector.getConnection();
        ResultSet rs = connector.executeQuery("select stockid from stockcorp where dte=(select max(dte) from stockcorp) and shszflag='2'");
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
     * 获取指定日期深圳交易所股票代码列表
     *
     * @param dateStr 给定日期
     * @return
     */
    public List<String> getSzStockCodes(String dateStr) {
        List<String> codes = new ArrayList<String>();
        MSSQLConnector connector = new MSSQLConnector();
        Connection sqlConn = connector.getConnection();
        ResultSet rs = connector.executeQuery("select stockid from stockcorp where dte='" + dateStr + "' and shszflag='2'");
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
        String baseUrlStr = "http://www.szse.cn/szseWeb/FrontController.szse";
        String baseRefererStr = "http://www.szse.cn/main/marketdata/hqcx/hqlb/index.shtml?code=";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        StockPrice sp = new StockPrice();

        System.out.println("开始获取股票 " + stockId + " 价格信息...");
        try {
            HttpPost httpPost = new HttpPost(baseUrlStr);
            Header header;
            header = new BasicHeader("Referer", baseRefererStr + stockId);
            httpPost.setHeader(header);

            //创建参数列表
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("ACTIONID", "3"));
            list.add(new BasicNameValuePair("ISAJAXLOAD", "true"));
            list.add(new BasicNameValuePair("CATALOGID", "mainzqhq"));
            list.add(new BasicNameValuePair("code", stockId));

            //url格式编码
            UrlEncodedFormEntity uefEntity = new UrlEncodedFormEntity(list, "GBK");
            httpPost.setEntity(uefEntity);

            HttpResponse httpResponse = httpClient.execute(httpPost); //throws IOException, ClientProtocolException
            HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
            String brLine;
            StringBuilder sb = new StringBuilder();
            while ((brLine = br.readLine()) != null) {
                sb.append(brLine);
            }

            //返回内容
            String reponseStr = sb.toString();
            //System.out.println(reponseStr);

            //日期
            Pattern pDate = Pattern.compile("月线</TD><TD align=\"right\"><SPAN>(.*)</SPAN>", Pattern.DOTALL);
            //开盘价
            Pattern pOpen = Pattern.compile("开盘价.*<font color=.*>(.*)</font>.*最新价", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //收盘价
            Pattern pClose = Pattern.compile("最新价.*<font color=.*>(.*)</font>.*涨跌", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //涨跌
            Pattern pRiseSpread = Pattern.compile("涨跌.*<font color=.*>(.*)</font>.*涨幅", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //涨幅(%)
            Pattern pRisePercent = Pattern.compile("涨幅\\(%\\).*<font color=.*>(.*)</font>.*最高价", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //最高价
            Pattern pHigh = Pattern.compile("最高价.*<font color=.*>(.*)</font>.*最低价", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //最低价
            Pattern pLow = Pattern.compile("最低价.*<font color=.*>(.*)</font>.*成交量", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //成交量(手)
            Pattern pVolume = Pattern.compile("成交量\\(手\\)</td><td>(.*)</td>.*成交额", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //成交额(万元)
            Pattern pTotalMoney = Pattern.compile("成交额\\(万元\\)</td><td>(.*)(</td></tr></tbody></table>){2}.*卖五", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //卖五
            Pattern pSell05 = Pattern.compile("卖五</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)</td></tr>.*卖四", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //卖四
            Pattern pSell04 = Pattern.compile("卖四</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)</td></tr>.*卖三", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //卖三
            Pattern pSell03 = Pattern.compile("卖三</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)</td></tr>.*卖二", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //卖二
            Pattern pSell02 = Pattern.compile("卖二</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)</td></tr>.*卖一", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //卖一
            Pattern pSell01 = Pattern.compile("卖一</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)</td></tr>.*买一", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //买一
            Pattern pBuy01 = Pattern.compile("买一</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)</td></tr>.*买二", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //买二
            Pattern pBuy02 = Pattern.compile("买二</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)</td></tr>.*买三", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //买三
            Pattern pBuy03 = Pattern.compile("买三</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)</td></tr>.*买四", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //买四
            Pattern pBuy04 = Pattern.compile("买四</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)</td></tr>.*买五", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            //买五
            Pattern pBuy05 = Pattern.compile("买五</td><td align='center'>(.*)</td><td align='right' class='pl11'>(.*)(</td></tr></tbody></table>){3}", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

            Matcher m = null;

            sp.setStockId(stockId);

//可能出现如下信息，需特殊处理
//开始获取股票 200029 价格信息...
//日期：20161125
//开盘价：
//收盘价：
//涨跌：0.00
//最高价：---
//最低价：---
//成交量(手)：0
//成交额(万元)：0.00
//卖五：---,---
//卖四：---,---
//卖三：---,---
//卖二：---,---
//卖一：---,---
//买一：---,---
//买二：---,---
//买三：---,---
//买四：---,---
//买五：---,---
//----------------------------
//开始获取股票 000033 价格信息...
//日期：20161125
//开盘价：
//收盘价：
//涨跌：
//最高价：
//最低价：
//成交量(手)：
//成交额(万元)：
//卖五：,
//卖四：,
//卖三：,
//卖二：,
//卖一：,
//买一：,
//买二：,
//买三：,
//买四：,
//买五：,
//开始获取股票 000511 价格信息...
//日期：20161125
//开盘价：7.27
//收盘价：7.27
//涨跌：0.38
//最高价：7.27
//最低价：7.27
//成交量(手)：18149
//成交额(万元)：1319.43
//卖五：7.31,799
//卖四：7.30,1384
//卖三：7.29,2419
//卖二：7.28,6769
//卖一：7.27,1284445
//买一：---,---
//买二：---,---
//买三：---,---
//买四：---,---
//买五：---,---

            m = pDate.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("日期：" + m.group(1).substring(0, 8));
                sp.setDate(m.group(1).substring(0, 8));
            }
            m = pOpen.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("开盘价：" + m.group(1));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                    sp.setOpen(0.0);
                } else {
                    sp.setOpen(Double.valueOf(m.group(1).trim()));
                }
            }
            m = pClose.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("收盘价：" + m.group(1));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                    sp.setClose(0.0);
                } else {
                    sp.setClose(Double.valueOf(m.group(1).trim()));
                }
            }
            m = pRiseSpread.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("涨跌：" + m.group(1));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                    sp.setRiseSpread(0.0);
                } else {
                    sp.setRiseSpread(Double.valueOf(m.group(1).trim()));
                }
            }
            m = pRisePercent.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("涨幅：" + m.group(1));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                    sp.setRisePercent(0.0);
                } else {
                    sp.setRisePercent(Double.valueOf(m.group(1).trim()));
                }
            }
            m = pHigh.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("最高价：" + m.group(1));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                    sp.setHigh(0.0);
                } else {
                    sp.setHigh(Double.valueOf(m.group(1).trim()));
                }
            }
            m = pLow.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("最低价：" + m.group(1));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                    sp.setLow(0.0);
                } else {
                    sp.setLow(Double.valueOf(m.group(1).trim()));
                }
            }
            m = pVolume.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("成交量(手)：" + m.group(1));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                    sp.setVolume(0.0);
                } else {
                    sp.setVolume(Double.valueOf(m.group(1).trim()) * 100);    //转换为股
                }
            }
            m = pTotalMoney.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("成交额(万元)：" + m.group(1));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                    sp.setTotalMoney(0.0);
                } else {
                    sp.setTotalMoney(Double.valueOf(m.group(1).trim()) * 10000);  //转换为元
                }
            }

            List<StockCloseEntrustDetail> buyCloseEntrustDetails = new ArrayList<StockCloseEntrustDetail>();
            List<StockCloseEntrustDetail> sellCloseEntrustDetails = new ArrayList<StockCloseEntrustDetail>();
            m = pSell05.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("卖五：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("S");
                    sced.setSequence("05");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    sellCloseEntrustDetails.add(sced);
                }
            }
            m = pSell04.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("卖四：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("S");
                    sced.setSequence("04");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    sellCloseEntrustDetails.add(sced);
                }
            }
            m = pSell03.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("卖三：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("S");
                    sced.setSequence("03");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    sellCloseEntrustDetails.add(sced);
                }
            }
            m = pSell02.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("卖二：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("S");
                    sced.setSequence("02");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    sellCloseEntrustDetails.add(sced);
                }
            }
            m = pSell01.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("卖一：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("S");
                    sced.setSequence("01");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    sellCloseEntrustDetails.add(sced);
                }
            }
            m = pBuy01.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("买一：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("B");
                    sced.setSequence("01");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    buyCloseEntrustDetails.add(sced);
                }
            }
            m = pBuy02.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("买二：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("B");
                    sced.setSequence("02");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    buyCloseEntrustDetails.add(sced);
                }
            }
            m = pBuy03.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("买三：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("B");
                    sced.setSequence("03");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    buyCloseEntrustDetails.add(sced);
                }
            }
            m = pBuy04.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("买四：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("B");
                    sced.setSequence("04");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    buyCloseEntrustDetails.add(sced);
                }
            }
            m = pBuy05.matcher(reponseStr);
            if (m.find()) {
//                System.out.println("买五：" + m.group(1) + "," + m.group(2));
                if (m.group(1).trim().equals("") || m.group(1).trim().equals("---")) {
                } else {
                    StockCloseEntrustDetail sced = new StockCloseEntrustDetail();
                    sced.setBuySellFlag("B");
                    sced.setSequence("05");
                    sced.setPrice(Double.valueOf(m.group(1)));
                    sced.setVolume(Double.valueOf(m.group(2)));
                    buyCloseEntrustDetails.add(sced);
                }
            }

            sp.setBuyCloseEntrustDetails(buyCloseEntrustDetails);
            sp.setSellCloseEntrustDetails(sellCloseEntrustDetails);

        } catch (ClientProtocolException cpe) {
            cpe.printStackTrace();
            return null;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return sp;
    }

    /**
     * 股票收盘时委托详情
     */
    class StockCloseEntrustDetail {
        //买卖标识

        private String buySellFlag;
        //委托顺序
        private String sequence;
        //委托价格
        private double price;
        //委托数量
        private double volume;

        /**
         * @return the buySellFlag
         */
        public String getBuySellFlag() {
            return buySellFlag;
        }

        /**
         * @param buySellFlag the buySellFlag to set
         */
        public void setBuySellFlag(String buySellFlag) {
            this.buySellFlag = buySellFlag;
        }

        /**
         * @return the sequence
         */
        public String getSequence() {
            return sequence;
        }

        /**
         * @param sequence the sequence to set
         */
        public void setSequence(String sequence) {
            this.sequence = sequence;
        }

        /**
         * @return the price
         */
        public double getPrice() {
            return price;
        }

        /**
         * @param price the price to set
         */
        public void setPrice(double price) {
            this.price = price;
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
    }

    /**
     * 股票价格信息
     */
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
        //收盘买入委托详情
        private List<StockCloseEntrustDetail> buyCloseEntrustDetails = new ArrayList<StockCloseEntrustDetail>();
        //收盘卖出委托详情
        private List<StockCloseEntrustDetail> sellCloseEntrustDetails = new ArrayList<StockCloseEntrustDetail>();

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

        /**
         * @return the buyCloseEntrustDetails
         */
        public List<StockCloseEntrustDetail> getBuyCloseEntrustDetails() {
            return buyCloseEntrustDetails;
        }

        /**
         * @param buyCloseEntrustDetails the buyCloseEntrustDetails to set
         */
        public void setBuyCloseEntrustDetails(List<StockCloseEntrustDetail> buyCloseEntrustDetails) {
            this.buyCloseEntrustDetails = buyCloseEntrustDetails;
        }

        /**
         * @return the sellCloseEntrustDetails
         */
        public List<StockCloseEntrustDetail> getSellCloseEntrustDetails() {
            return sellCloseEntrustDetails;
        }

        /**
         * @param sellCloseEntrustDetails the sellCloseEntrustDetails to set
         */
        public void setSellCloseEntrustDetails(List<StockCloseEntrustDetail> sellCloseEntrustDetails) {
            this.sellCloseEntrustDetails = sellCloseEntrustDetails;
        }
    }
}
