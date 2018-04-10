/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.price;

import cn.idaze.stockfunny.database.Connector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

/**
 * 导入深交所股票交易价格信息
 *
 * @author hhq
 */
public class SzStockTradePriceImporter {

    public static void main(String[] args) throws Exception {
        SzStockTradePriceImporter importer = new SzStockTradePriceImporter();
        Map<String, String> map = new HashMap<String, String>();
        map.put("DTE", "20180409");
        map.put("STOCK", "000590");
        importer.execJob(map);
    }

    /**
     * 执行作业
     *
     * @param params 作业执行参数
     * @return
     */
    public boolean execJob(Map<String, String> params) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(SzStockTradePriceImporter.class);

        String dateStr;
        String stockId;
        if (params.get("DTE") == null || params.get("STOCK") == null) {
            logger.error("参数非法，需要传入日期和股票代码!");
            return false;
        } else {
            dateStr = params.get("DTE").trim();
            stockId = params.get("STOCK").trim();
            logger.debug("执行参数为：DTE=" + dateStr + ", STOCK=" + stockId);
        }

        if (importTradePriceInfo(dateStr, stockId) == true) {
            logger.debug("导入股票" + stockId + "交易明细信息成功！");
            return true;
        } else {
            logger.debug("导入股票" + stockId + "交易明细信息失败！");
            return false;
        }

    }

    /**
     * 导入股票交易价格信息至数据库
     *
     * @param stockId 证券代码
     * @return
     */
    public boolean importTradePriceInfo(String dateStr, String stockId) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(SzStockTradePriceImporter.class);

        String baseUrlStr = "http://www.szse.cn/szseWeb/FrontController.szse";
        String baseRefererStr = "http://www.szse.cn/main/marketdata/hqcx/hqlb/index.shtml?code=";
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String delSql = "delete from STOCK_TRADE_PRICE_SZ where dte=? and stockid=? and datasource=? and cycle=? and adjtype=?";
        String insertSql = "insert into STOCK_TRADE_PRICE_SZ (dte, trdte, trtime, stockid, cycle, adjtype, "
                + "preclose, open, high, low, close, risespread, risepercent, volumn, totalmoney, "
                + "sell05, sell05_entrust, sell04, sell04_entrust, sell03, sell03_entrust, sell02, sell02_entrust, sell01, sell01_entrust, "
                + "buy01, buy01_entrust, buy02, buy02_entrust, buy03, buy03_entrust, buy04, buy04_entrust, buy05, buy05_entrust, "
                + "datasource, lastupdatetime, remarks) "
                + "values (?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, sysdate(6), ?)";
        Connector connector = new Connector();
        Connection conn = connector.getConnection();

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

            HttpResponse httpResponse = httpClient.execute(httpPost);
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
            TagNode tagNode = new HtmlCleaner().clean(reponseStr);
            org.w3c.dom.Document doc;
            doc = new DomSerializer(new CleanerProperties()).createDOM(tagNode);
            XPath xpath = XPathFactory.newInstance().newXPath();

            PreparedStatement ps = connector.prepareStmt(delSql);
            ps.setString(1, dateStr.trim());
            ps.setString(2, stockId.trim());
            ps.setString(3, "SZ");
            ps.setString(4, "D");
            ps.setString(5, "N");
            ps.execute();

            ps = connector.prepareStmt(insertSql);
            ps.setString(1, dateStr.trim());
            String trdte = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[1]/td[8]", doc, XPathConstants.STRING);
            try {
                ps.setString(2, trdte.trim().substring(0, 8));
            } catch (IndexOutOfBoundsException ioex) {
                logger.error("获取交易日期失败！" + ioex.getMessage());
                return false;
            }
            try {
                //ps.setDate(3, new java.sql.Date(new SimpleDateFormat("yyyyMMdd HH:mm").parse(trdte.trim()).getTime()));
                ps.setTimestamp(3, new java.sql.Timestamp(new SimpleDateFormat("yyyyMMdd HH:mm").parse(trdte.trim()).getTime()));
            } catch (ParseException pe) {
                logger.error("获取交易时间失败！" + pe.getMessage());
                return false;
            }
            ps.setString(4, stockId.trim());
            ps.setString(5, "D");
            ps.setString(6, "N");

            ps.setDouble(7, 0.0); //上一日收盘价
            String openPrice = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[1]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(8, Double.valueOf(openPrice.trim().replaceAll("\\u00A0", "")));
            } catch (NumberFormatException nfe) {
                //System.out.println((int)(openPrice.trim().charAt(0))); //存在ASCII值为160的空格，使用trim无法去除。
                logger.error("获取开盘价失败！" + nfe.getMessage());
                return false;
            }
            String closePrice = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[2]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(11, Double.valueOf(closePrice.trim().replaceAll("\\u00A0", "")));
            } catch (NumberFormatException nfe) {
                logger.error("获取收盘价失败！" + nfe.getMessage());
                return false;
            }
            String riseFlag = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[3]/td[2]/img/@src", doc, XPathConstants.STRING);
            String riseSpread = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[3]/td[2]", doc, XPathConstants.STRING);
            int sign; //用于标识涨跌、涨幅正负号
            if (riseFlag.trim().equals("/szseWeb/common/images/upar.gif") == true) {
                sign = 1;
            } else if (riseFlag.trim().equals("/szseWeb/common/images/downar.gif") == true) {
                sign = -1;
            } else {
                logger.error("未正确获取到涨跌幅的正负号标识！");
                return false;
            }
            try {
                ps.setDouble(12, sign * Double.valueOf(riseSpread.trim().replaceAll("\\u00A0", "")));
            } catch (NumberFormatException nfe) {
                logger.error("获取涨跌失败！" + nfe.getMessage());
                return false;
            }

            String risePercent = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[4]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(13, sign * Double.valueOf(risePercent.trim().replaceAll("\\u00A0", "")));
            } catch (NumberFormatException nfe) {
                logger.error("获取涨跌幅(%)失败！" + nfe.getMessage());
                return false;
            }
            String high = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[5]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(9, Double.valueOf(high.trim().replaceAll("\\u00A0", "")));
            } catch (NumberFormatException nfe) {
                logger.error("获取最高价失败！" + nfe.getMessage());
                return false;
            }
            String low = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[6]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(10, Double.valueOf(low.trim().replaceAll("\\u00A0", "")));
            } catch (NumberFormatException nfe) {
                logger.error("获取最低价失败！" + nfe.getMessage());
                return false;
            }
            String volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[7]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(14, Double.valueOf(volumn.trim().replaceAll("\\u00A0", "")));
            } catch (NumberFormatException nfe) {
                logger.error("获取成交量失败！" + nfe.getMessage());
                return false;
            }
            String totalMoney = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[8]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(15, Double.valueOf(totalMoney.trim().replaceAll("\\u00A0", "")));
            } catch (NumberFormatException nfe) {
                logger.error("获取成交金额失败！" + nfe.getMessage());
                return false;
            }

            String sell05 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[1]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(16, Double.valueOf(sell05.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(16, 0.0);
            }
            String sell05_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[1]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(17, Double.valueOf(sell05_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(17, 0.0);
            }
            String sell04 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[2]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(18, Double.valueOf(sell04.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(18, 0.0);
            }
            String sell04_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[2]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(19, Double.valueOf(sell04_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(19, 0.0);
            }
            String sell03 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[3]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(20, Double.valueOf(sell03.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(20, 0.0);
            }
            String sell03_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[3]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(21, Double.valueOf(sell03_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(21, 0.0);
            }
            String sell02 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[4]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(22, Double.valueOf(sell02.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(22, 0.0);
            }
            String sell02_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[4]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(23, Double.valueOf(sell02_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(23, 0.0);
            }
            String sell01 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[5]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(24, Double.valueOf(sell01.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(24, 0.0);
            }
            String sell01_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[5]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(25, Double.valueOf(sell01_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(25, 0.0);
            }

            String buy01 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[6]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(26, Double.valueOf(buy01.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(26, 0.0);
            }
            String buy01_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[6]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(27, Double.valueOf(buy01_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(27, 0.0);
            }
            String buy02 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[7]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(28, Double.valueOf(buy02.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(28, 0.0);
            }
            String buy02_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[7]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(29, Double.valueOf(buy02_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(29, 0.0);
            }
            String buy03 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[8]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(30, Double.valueOf(buy03.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(30, 0.0);
            }
            String buy03_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[8]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(31, Double.valueOf(buy03_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(31, 0.0);
            }
            String buy04 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[9]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(32, Double.valueOf(buy04.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(32, 0.0);
            }
            String buy04_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[9]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(33, Double.valueOf(buy04_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(33, 0.0);
            }
            String buy05 = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[10]/td[2]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(34, Double.valueOf(buy05.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(34, 0.0);
            }
            String buy05_volumn = (String) xpath.evaluate("//table[5]/tbody[1]/tr[1]/td[2]/table[3]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[10]/td[3]", doc, XPathConstants.STRING);
            try {
                ps.setDouble(35, Double.valueOf(buy05_volumn.trim()));
            } catch (NumberFormatException nfe) {
                ps.setDouble(35, 0.0);
            }

            ps.setString(36, "SZ");
            ps.setString(37, "");

//            System.out.println("trdte:" + trdte);
//            System.out.println("open:" + openPrice);
//            System.out.println("close:" + closePrice);
//            System.out.println("riseSpread:" + riseSpread);
//            System.out.println("risePercent:" + risePercent);
//            System.out.println("high:" + high);
//            System.out.println("low:" + low);
//            System.out.println("volumn:" + volumn);
//            System.out.println("totalMoney:" + totalMoney);
//            System.out.println("sell05:" + sell05 + " volumn:" + sell05_volumn);
//            System.out.println("sell04:" + sell04 + " volumn:" + sell04_volumn);
//            System.out.println("sell03:" + sell03 + " volumn:" + sell03_volumn);
//            System.out.println("sell02:" + sell02 + " volumn:" + sell02_volumn);
//            System.out.println("sell01:" + sell01 + " volumn:" + sell01_volumn);
//            System.out.println("buy01:" + buy01 + " volumn:" + buy01_volumn);
//            System.out.println("buy02:" + buy02 + " volumn:" + buy02_volumn);
//            System.out.println("buy03:" + buy03 + " volumn:" + buy03_volumn);
//            System.out.println("buy04:" + buy04 + " volumn:" + buy04_volumn);
//            System.out.println("buy05:" + buy05 + " volumn:" + buy05_volumn);

            ps.execute();
            ps.close();
            conn.close();
        } catch (SQLException sqlex) {
            logger.error("SQL执行失败！" + sqlex.getMessage());
            return false;
        } catch (IOException ioe) {
            logger.error("数据读写失败！" + ioe.getMessage());
            return false;
        } catch (ParserConfigurationException ex) {
            logger.error("转换配置异常！" + ex.getMessage());
            return false;
        } catch (XPathExpressionException ex) {
            logger.error("XPath表达式异常！" + ex.getMessage());
            return false;
        }
        
        return true;
    }

}
