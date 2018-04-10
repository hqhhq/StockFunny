/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.corp;

import cn.idaze.stockfunny.database.MSSQLConnector;
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
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @deprecated 
 * 
 * 上海上市公司信息导入器
 * @author HHQ
 */
public class ShStockCorpImporter {

    public static void main(String[] args) {
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filePath = "E:/Documents and Settings/HHQ/桌面/stock/database/stock";
        String aFileName = "上海A股上市公司信息列表_" + dateStr;
        String bFileName = "上海B股上市公司信息列表_" + dateStr;
        //数据来源：SH--上交所网站
        String dataSource = "SH";
        boolean b;

        ShStockCorpImporter importer = new ShStockCorpImporter();
        b = importer.downloadShStockCorpInfo(filePath, aFileName, bFileName);
        if (b == true) {
            System.out.println("文件下载成功，开始导入数据库...");
//            if (importer.importBasicData(filePath + "/" + aFileName + ".xls", "1", dateStr, dataSource) == true) {
//                System.out.println("A股文件导入成功！");
//            } else {
//                System.out.println("A股文件导入失败！");
//            }
//
//            if (importer.importBasicData(filePath + "/" + bFileName + ".xls", "2", dateStr, dataSource) == true) {
//                System.out.println("B股文件导入成功！");
//            } else {
//                System.out.println("B股文件导入失败！");
//            }
            
            if (importer.importFullData(filePath + "/" + aFileName + ".xls", "1", dateStr, dataSource) == true) {
                System.out.println("A股文件导入成功！");
            } else {
                System.out.println("A股文件导入失败！");
            }

            if (importer.importFullData(filePath + "/" + bFileName + ".xls", "2", dateStr, dataSource) == true) {
                System.out.println("B股文件导入成功！");
            } else {
                System.out.println("B股文件导入失败！");
            }
        }
    }

    /**
     * 导入沪市上市公司完整信息，结合给定的excel文件，再从网页上下载每个公司信息。
     * 虽然导出时显示文件为.xls文件，但实际上是以tab分额的文本文件，因此此时使用poi解析有问题，除非手动另存一份。此处直接对文本文件进行处理。
     *
     * @param file 要导入的文件
     * @param abFlag A、B股标识
     * @param dateStr 要导入的数据日期
     * @param dataSource 数据来源
     * @return
     */
    public boolean importFullData(String file, String abFlag, String dateStr, String dataSource) {
        //获取公司全称、英文全称、注册地址、省份、所属行业、公司网址、
        String baseUrlStr = "http://query.sse.com.cn/commonQuery.do?jsonCallBack=jsonpCallback75619&isPagination=false&sqlId=COMMON_SSE_ZQPZ_GP_GPLB_C&productid=";
        String baseRefererStr = "http://www.sse.com.cn/assortment/stock/list/info/company/index.shtml?COMPANY_CODE=";
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String delSql = "delete from stockcorp where dte=? and abflag=? and datasource=? and shszflag='1' ";
        String insertSql = "insert into stockcorp (dte, corpid, stockid, abflag, shszflag, abbrname, "
                + "fullname, engname, regaddr, ipodate, capitalstock, "
                + "tradableshares, areacode, area, provincecode, province, "
                + "citycode, city, industrycode, industry, website, remarks, datasource, optime) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, getdate())";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);
        try {
            ps.setString(1, dateStr);
            ps.setString(2, abFlag);
            ps.setString(3, dataSource);
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            //读取股票代码文件
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
            String newLine;
            in.readLine(); //跳过第一行表头
            while ((newLine = in.readLine()) != null) {
                String ss[] = newLine.split("	  ");
//                for (String s : ss) {
//                    System.out.println(s);
//                }

                //针对给定的股票代码，爬取网页详细信息
                HttpGet httpGet = new HttpGet(baseUrlStr + ss[2].trim());
                Header header;
                header = new BasicHeader("Referer", baseRefererStr + ss[2].trim());
                httpGet.setHeader(header);
                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity entity = httpResponse.getEntity();
                InputStream is = entity.getContent();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String brLine;
                StringBuilder sb = new StringBuilder();
                while ((brLine = br.readLine()) != null) {
                    sb.append(brLine);
                }
                String corpDetailInfo = sb.toString();
                String corpDetailInfoJson = corpDetailInfo.substring(19, corpDetailInfo.length() - 1); //去除返回串前的"jsonpCallback75619("及最后的")"，以拼出合法的json串
                //System.out.println(corpDetailInfoJson);
                JSONObject jsonObj = new JSONObject(corpDetailInfoJson);
                JSONArray jsonarray = jsonObj.getJSONArray("result");

                ps.setString(1, dateStr);
                //公司代码
                ps.setString(2, ss[0].trim());
                //股票代码
                ps.setString(3, ss[2].trim());
                ps.setString(4, abFlag);
                ps.setString(5, "1");
                //简称
                ps.setString(6, ss[3].trim());
                //全称
                ps.setString(7, jsonarray.getJSONObject(0).getString("FULLNAME").trim());
                //英文名称
                ps.setString(8, jsonarray.getJSONObject(0).getString("FULL_NAME_IN_ENGLISH").trim());
                //注册地址
                ps.setString(9, jsonarray.getJSONObject(0).getString("COMPANY_ADDRESS").trim());
                //IPO日期
                ps.setString(10, ss[4].trim().replace("-", ""));
                //总股本
                ps.setDouble(11, Double.valueOf(ss[5].trim()));
                //流通股本
                ps.setDouble(12, Double.valueOf(ss[6].trim()));
                //地域代码
                ps.setString(13, "");
                //地域名称
                ps.setString(14, "");
                //省市代码
                ps.setString(15, "");
                //省市名称
                ps.setString(16, jsonarray.getJSONObject(0).getString("AREA_NAME_DESC").trim());
                //城市代码
                ps.setString(17, "");
                //城市名称
                ps.setString(18, "");
                //行业代码
                ps.setString(19, "");
                //行业名称
                ps.setString(20, jsonarray.getJSONObject(0).getString("CSRC_CODE_DESC").trim() + "/"
                        + jsonarray.getJSONObject(0).getString("CSRC_GREAT_CODE_DESC").trim() + "/" + jsonarray.getJSONObject(0).getString("CSRC_MIDDLE_CODE_DESC").trim());
                //网址
                ps.setString(21, jsonarray.getJSONObject(0).getString("WWW_ADDRESS").trim());
                //备注
                ps.setString(22, "");
                //数据来源
                ps.setString(23, dataSource);

                ps.addBatch();
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
                
                is.close();
            }


            ps.executeBatch(); // insert remaining records
            ps.close();
            conn.close();
            in.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        } catch (IOException ioex) {
            ioex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 导入沪市上市公司基本信息。虽然导出时显示文件为.xls文件，但实际上是以tab分额的文本文件，因此此时使用poi解析有问题，除非手动另存一份。此处直接对文本文件进行处理。
     *
     * @param file 要导入的文件
     * @param abFlag A、B股标识
     * @param dateStr 要导入的数据日期
     * @param dataSource 数据来源
     * @return
     */
    public boolean importBasicData(String file, String abFlag, String dateStr, String dataSource) {
        String delSql = "delete from stockcorp where dte=? and abflag=? and datasource=? and shszflag='1' ";
        String insertSql = "insert into stockcorp (dte, corpid, stockid, abflag, shszflag, abbrname, "
                + "fullname, engname, regaddr, ipodate, capitalstock, "
                + "tradableshares, areacode, area, provincecode, province, "
                + "citycode, city, industrycode, industry, website, remarks, datasource, optime) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, getdate())";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);

        try {
            ps.setString(1, dateStr);
            ps.setString(2, abFlag);
            ps.setString(3, dataSource);
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
            String newLine;
            in.readLine(); //跳过第一行表头
            while ((newLine = in.readLine()) != null) {
                String ss[] = newLine.split("	  ");
//                for (String s : ss) {
//                    System.out.println(s);
//                }

                ps.setString(1, dateStr);
                //公司代码
                ps.setString(2, ss[0].trim());
                //股票代码
                ps.setString(3, ss[2].trim());
                ps.setString(4, abFlag);
                ps.setString(5, "1");
                //简称
                ps.setString(6, ss[3].trim());
                //全称
                ps.setString(7, "");
                //英文名称
                ps.setString(8, "");
                //注册地址
                ps.setString(9, "");
                //IPO日期
                ps.setString(10, ss[4].trim().replace("-", ""));
                //总股本
                ps.setDouble(11, Double.valueOf(ss[5].trim()));
                //流通股本
                ps.setDouble(12, Double.valueOf(ss[6].trim()));
                //地域代码
                ps.setString(13, "");
                //地域名称
                ps.setString(14, "");
                //省市代码
                ps.setString(15, "");
                //省市名称
                ps.setString(16, "");
                //城市代码
                ps.setString(17, "");
                //城市名称
                ps.setString(18, "");
                //行业代码
                ps.setString(19, "");
                //行业名称
                ps.setString(20, "");
                //网址
                ps.setString(21, "");
                //备注
                ps.setString(22, "");
                //数据来源
                ps.setString(23, dataSource);

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
            sqlex.printStackTrace();
            return false;
        } catch (IOException ioex) {
            ioex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 下载上海上市公司信息，并保存在指定的文件夹内。
     *
     * @param filePath 文件保存路径，末尾不需要分隔符，如“E:/Documents and Settings/HHQ/桌面/stock”
     * @param aFileName 保存的A股文件名称，不带后缀
     * @param bFileName 保存的B股文件名称，不带后缀
     * @return 下载成功返回true，下载失败返回false
     */
    private boolean downloadShStockCorpInfo(String filePath, String aFileName, String bFileName) {
        String aUrlStr = "http://query.sse.com.cn/security/stock/downloadStockListFile.do?csrcCode=&stockCode=&areaName=&stockType=1";
        String bUrlStr = "http://query.sse.com.cn/security/stock/downloadStockListFile.do?csrcCode=&stockCode=&areaName=&stockType=2";
        String referer = "http://www.sse.com.cn/assortment/stock/list/share/";

        // 创建默认的httpClient实例.    
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建httpget.    
        HttpGet aHttpGet = new HttpGet(aUrlStr);
        HttpGet bHttpGet = new HttpGet(bUrlStr);
        Header header;
        header = new BasicHeader("Referer", referer);
        aHttpGet.setHeader(header);
        bHttpGet.setHeader(header);

        try { //下载A股公司列表
            HttpResponse httpResponse = httpClient.execute(aHttpGet);
            HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            OutputStream os = new FileOutputStream(new File(filePath + "/" + aFileName + ".xls"));

            byte[] b;
            b = new byte[1024];
            int n;

            while ((n = is.read(b)) != -1) {
                os.write(b, 0, n);
            }

            os.close();
            is.close();
            System.out.println("文件 \"" + aFileName + ".xls\" 下载成功");
        } catch (IOException ioe) {
            System.out.println("下载失败!");
            ioe.printStackTrace();
            return false;
        }

        try { //下载B股公司列表
            HttpResponse httpResponse = httpClient.execute(bHttpGet);
            HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            OutputStream os = new FileOutputStream(new File(filePath + "/" + bFileName + ".xls"));

            byte[] b;
            b = new byte[1024];
            int n;

            while ((n = is.read(b)) != -1) {
                os.write(b, 0, n);
            }

            os.close();
            is.close();
            System.out.println("文件 \"" + bFileName + ".xls\" 下载成功");
        } catch (IOException ioe) {
            System.out.println("下载失败!");
            ioe.printStackTrace();
            return false;
        }

        return true;
    }
}
