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
 *
 * @author hhq
 */
public class ShStockCorpPauseInfoImporter {
        public static void main(String[] args) {

        ShStockCorpPauseInfoImporter importer = new ShStockCorpPauseInfoImporter();
        Map<String, String> map = new HashMap<String, String>();
        map.put("DTE", "20180325");
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
        Logger logger = Logger.getLogger(ShStockCorpPauseInfoImporter.class);
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
        String aFileName = "上海暂停上市公司信息列表_" + dateStr;
        //数据来源：SH--上交所网站
        String dataSource = "SH";
        boolean b;

        if (new File(filePath + "/" + aFileName + ".xls").exists() == true) {//文件存在
            //System.out.println("文件\"" + filePath + "/" + aFileName + ".xls\"已存在，直接导入");
            logger.debug("文件\"" + filePath + "/" + aFileName + ".xls\"已存在，直接导入");
            if (importPauseCorpInfo(filePath + "/" + aFileName + ".xls", dateStr, dataSource) == true) {
                //System.out.println("A股文件导入成功！");
                logger.debug("暂停上市公司文件导入成功！");
                return true;
            } else {
                logger.debug("暂停上市公司文件导入失败！");
                return false;
            }
        } else {//文件不存在，先下载再导入
            if (dateStr.compareTo(new SimpleDateFormat("yyyyMMdd").format(new Date())) < 0) {
                //System.out.println("要导入的数据文件不存在，且指定要下载的日期小于当前日期，导入失败！");
                logger.debug("要导入的数据文件不存在，且指定要下载的日期小于当前日期，导入失败！");
                return false;
            } else {
                b = downloadPauseCorpInfo(filePath, aFileName);
                if (b == true) {
                    //System.out.println("文件下载成功，开始导入数据库...");
                    logger.debug("文件下载成功，开始导入数据库...");

                    if (importPauseCorpInfo(filePath + "/" + aFileName + ".xls", dateStr, dataSource) == true) {
                        //System.out.println("A股文件导入成功！");
                        logger.debug("暂停上市公司文件导入成功！");
                        return true;
                    } else {
                        //System.out.println("A股文件导入失败！");
                        logger.debug("暂停上市公司文件导入失败！");
                        return false;
                    }

                } else {
                    logger.error("文件下载失败!");
                    return false;
                }
            }

        }
    }

    /**
     * 导入沪市暂停上市公司基本信息。虽然导出时显示文件为.xls文件，但实际上是以tab分额的文本文件，因此此时使用poi解析有问题，除非手动另存一份。此处直接对文本文件进行处理。
     *
     * @param file 要导入的文件
     * @param dateStr 要导入的数据日期
     * @param dataSource 数据来源
     * @return
     */
    public boolean importPauseCorpInfo(String file, String dateStr, String dataSource) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(ShStockCorpPauseInfoImporter.class);

        String delSql = "delete from CORP_PAUSE_LIST_SH where dte=? and datasource=?";
        String insertSql = "insert into CORP_PAUSE_LIST_SH (dte, corpid, corpabbrname, ipodate, pausedate, "
                + " datasource, lastupdatetime, remarks) "
                + " values (?, ?, ?, ?, ?, ?, sysdate(6), ?)";
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
                //上市日期
                ps.setString(4, ss[2].trim().replace("-", ""));
                //暂停上市日期
                ps.setString(5, ss[3].trim().replace("-", ""));
                //数据来源
                ps.setString(6, dataSource);
                //备注
                ps.setString(7, "");

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
     * 下载上海暂停上市公司信息，并保存在指定的文件夹内。
     *
     * @param filePath 文件保存路径，末尾不需要分隔符，如“E:/Documents and Settings/HHQ/桌面/stock”
     * @param fileName 保存的股票文件名称，不带后缀
     * @return 下载成功返回true，下载失败返回false
     */
    private boolean downloadPauseCorpInfo(String filePath, String fileName) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(ShStockCorpPauseInfoImporter.class);

        String aUrlStr = "http://query.sse.com.cn/security/stock/downloadStockListFile.do?csrcCode=&stockCode=&areaName=&stockType=4";
        String referer = "http://www.sse.com.cn/assortment/stock/list/share/";

        // 创建默认的httpClient实例.    
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建httpget.    
        HttpGet aHttpGet = new HttpGet(aUrlStr);
        Header header;
        header = new BasicHeader("Referer", referer);
        aHttpGet.setHeader(header);

        try { //下载A股公司列表
            HttpResponse httpResponse = httpClient.execute(aHttpGet);
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
