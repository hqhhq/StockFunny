/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.corp;

import cn.idaze.stockfunny.database.Connector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * 从上交所网站导入上交所B股股票清单
 *
 * @author hhq
 */
public class ShBStockCorpBreifInfoImporter {

    public static void main(String[] args) {

        ShBStockCorpBreifInfoImporter importer = new ShBStockCorpBreifInfoImporter();
        Map<String, String> map = new HashMap<String, String>();
        map.put("DTE", "20180326");
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
        Logger logger = Logger.getLogger(ShBStockCorpBreifInfoImporter.class);
        String dateStr;
        if (params.get("DTE") == null) {
            logger.error("参数非法，需要传入日期!");
            return false;
        } else {
            dateStr = params.get("DTE").trim();
            logger.debug("执行参数为：DTE=" + dateStr);
        }

        //String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filePath = System.getProperty("user.dir") + "/resources/stock";
        String bFileName = "上海B股上市公司信息列表_" + dateStr;
        //数据来源：SH--上交所网站
        String dataSource = "SH";
        boolean b;

        if (new File(filePath + "/" + bFileName + ".xls").exists() == true) {//文件存在
            //System.out.println("文件\"" + filePath + "/" + bFileName + ".xls\"已存在，直接导入");
            logger.debug("文件\"" + filePath + "/" + bFileName + ".xls\"已存在，直接导入");
            if (importStockBreifInfo(filePath + "/" + bFileName + ".xls", dateStr, dataSource) == true) {
                //System.out.println("B股文件导入成功！");
                logger.debug("B股文件导入成功！");
                return true;
            } else {
                //System.out.println("B股文件导入失败！");
                logger.debug("B股文件导入失败！");
                return false;
            }
        } else {//文件不存在，先下载再导入
            if (dateStr.compareTo(new SimpleDateFormat("yyyyMMdd").format(new Date())) < 0) {
                //System.out.println("要导入的数据文件不存在，且指定要下载的日期小于当前日期，导入失败！");
                logger.debug("要导入的数据文件不存在，且指定要下载的日期小于当前日期，导入失败！");
                return false;
            } else {
                b = downloadStockCorpInfo(filePath, bFileName);
                if (b == true) {
                    //System.out.println("文件下载成功，开始导入数据库...");
                    logger.debug("文件下载成功，开始导入数据库...");

                    if (importStockBreifInfo(filePath + "/" + bFileName + ".xls", dateStr, dataSource) == true) {
                        //System.out.println("B股文件导入成功！");
                        logger.debug("B股文件导入成功！");
                        return true;
                    } else {
                        //System.out.println("B股文件导入失败！");
                        logger.debug("B股文件导入失败！");
                        return false;
                    }

                }else{
                    logger.error("下载文件失败！");
                    return false;
                }
            }

        }
    }

    /**
     * 导入沪市上市公司基本信息。虽然导出时显示文件为.xls文件，但实际上是以tab分额的文本文件，因此此时使用poi解析有问题，除非手动另存一份。此处直接对文本文件进行处理。
     *
     * @param file 要导入的文件
     * @param dateStr 要导入的数据日期
     * @param dataSource 数据来源
     * @return
     */
    public boolean importStockBreifInfo(String file, String dateStr, String dataSource) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(ShBStockCorpBreifInfoImporter.class);

        String delSql = "delete from STOCK_LIST_B_SH where dte=? and datasource=?";
        String insertSql = "insert into STOCK_LIST_B_SH (dte, corpid, corpabbrname, stockid, stockabbrname, ipodate, "
                + "capitalstock, tradableshares, datasource, lastupdatetime, remarks) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate(6), ?)";
        Connector connector = new Connector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);

        try {
            ps.setString(1, dateStr);
            ps.setString(2, dataSource);
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
            String newLine;
            in.readLine(); //跳过第一行表头
            while ((newLine = in.readLine()) != null) {
                String ss[] = newLine.split("	  ");//虽然是以.xls结尾的文件，但实际上是文本文件
//                for (String s : ss) {
//                    System.out.println(s);
//                }

                //数据日期
                ps.setString(1, dateStr);
                //公司代码
                ps.setString(2, ss[0].trim());
                //公司简称
                ps.setString(3, ss[1].trim());
                //股票代码
                ps.setString(4, ss[2].trim());
                //股票简称
                ps.setString(5, ss[3].trim());
                //IPO日期
                ps.setString(6, ss[4].trim().replace("-", ""));
                //总股本
                ps.setDouble(7, Double.valueOf(ss[5].trim()));
                //流通股本
                ps.setDouble(8, Double.valueOf(ss[6].trim()));
                //数据来源
                ps.setString(9, dataSource);
                //备注
                ps.setString(10, "");

                ps.addBatch();
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
            }

            ps.executeBatch(); // insert remaining records
            ps.close();
            conn.close();
            in.close();
        } catch (SQLException sqlex) {
            //System.out.println("SQL执行失败！");
            logger.error("SQL执行失败！" + sqlex.getMessage());
            return false;
        } catch (IOException ioex) {
            //System.out.println("数据读写失败！");
            logger.error("数据读写失败！" + ioex.getMessage());
            return false;
        }

        return true;
    }

    /**
     * 下载上海上市公司信息，并保存在指定的文件夹内。
     *
     * @param filePath 文件保存路径，末尾不需要分隔符，如“E:/Documents and Settings/HHQ/桌面/stock”
     * @param fileName 保存的股票文件名称，不带后缀
     * @return 下载成功返回true，下载失败返回false
     */
    private boolean downloadStockCorpInfo(String filePath, String fileName) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(ShBStockCorpBreifInfoImporter.class);
        
        String bUrlStr = "http://query.sse.com.cn/security/stock/downloadStockListFile.do?csrcCode=&stockCode=&areaName=&stockType=2";
        String referer = "http://www.sse.com.cn/assortment/stock/list/share/";

        // 创建默认的httpClient实例.    
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建httpget.    
        HttpGet bHttpGet = new HttpGet(bUrlStr);
        Header header;
        header = new BasicHeader("Referer", referer);
        bHttpGet.setHeader(header);

        try { //下载B股公司列表
            HttpResponse httpResponse = httpClient.execute(bHttpGet);
            HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            OutputStream os = new FileOutputStream(new File(filePath + "/" + fileName + ".xls"));

            byte[] b;
            b = new byte[1024];
            int n;

            while ((n = is.read(b)) != -1) {
                os.write(b, 0, n);
            }

            os.close();
            is.close();
            //System.out.println("文件 \"" + fileName + ".xls\" 下载成功");
            logger.debug("文件 \"" + fileName + ".xls\" 下载成功");
        } catch (IOException ioe) {
            //System.out.println("下载失败!");
            logger.error("下载失败!" + ioe.getMessage());
            return false;
        }

        return true;
    }
}
