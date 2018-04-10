/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.corp;

import cn.idaze.stockfunny.database.Connector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * 导入深圳上市公司基本信息
 * @author hhq
 */
public class SzStockCorpBreifInfoImporter {

    public static void main(String[] args) {

        SzStockCorpBreifInfoImporter importer = new SzStockCorpBreifInfoImporter();
        Map<String, String> map = new HashMap<String, String>();
        map.put("DTE", "20180119");
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
        Logger logger = Logger.getLogger(SzStockCorpBreifInfoImporter.class);
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
        String fileName = "深圳上市公司信息列表_" + dateStr;
        //数据来源：SZ--深交所网站
        String dataSource = "SZ";
        boolean b;

        SzStockCorpBreifInfoImporter importer = new SzStockCorpBreifInfoImporter();
        if (new File(filePath + "/" + fileName + ".xlsx").exists() == true) {//文件存在
            //System.out.println("文件\"" + filePath + "/" + fileName + ".xlsx\"已存在，直接导入");
            logger.debug("文件\"" + filePath + "/" + fileName + ".xlsx\"已存在，直接导入");
            if (importer.importStockBreifInfo(filePath + "/" + fileName + ".xlsx", dateStr, dataSource) == true) {
                //System.out.println("深市股票文件导入成功！");
                logger.debug("深市股票文件导入成功！");
                return true;
            } else {
                //System.out.println("深市股票文件导入失败！");
                logger.debug("深市股票文件导入失败！");
                return false;
            }
        } else {//文件不存在，先下载再导入
            if (dateStr.compareTo(new SimpleDateFormat("yyyyMMdd").format(new Date())) < 0) {
                //System.out.println("要导入的数据文件不存在，且指定要下载的日期小于当前日期，导入失败！");
                logger.debug("要导入的数据文件不存在，且指定要下载的日期小于当前日期，导入失败！");
                return false;
            } else {
                b = importer.downloadStockCorpInfo(filePath, fileName);
                if (b == true) {
                    //System.out.println("文件下载成功，开始导入数据库...");
                    logger.debug("文件下载成功，开始导入数据库...");

                    if (importer.importStockBreifInfo(filePath + "/" + fileName + ".xlsx", dateStr, dataSource) == true) {
                        //System.out.println("深市股票文件导入成功！");
                        logger.debug("深市股票文件导入成功！");
                        return true;
                    } else {
                        //("深市股票文件导入失败！");
                        logger.debug("深市股票文件导入失败！");
                        return false;
                    }

                } else {
                    logger.debug("下载文件失败！");
                    return false;
                }
            }

        }
    }

    /**
     * 导入深市上市公司数据。文件格式为.xlsx文件
     *
     * @param file 要导入的文件
     * @param dateStr 要导入的数据日期
     * @param dataSource 数据来源
     * @return
     */
    public boolean importStockBreifInfo(String file, String dateStr, String dataSource) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(SzStockCorpBreifInfoImporter.class);

        String delSql = "delete from STOCK_LIST_SZ where dte=? and datasource=?";
        String insertSql = "insert into STOCK_LIST_SZ (dte, corpid, corpabbrname, corpfullname, "
                + "engname, regaddr, stockid_a, stockabbrname_a, ipodate_a, capitalstock_a, "
                + "tradableshares_a, stockid_b, stockabbrname_b, ipodate_b, capitalstock_b, "
                + "tradableshares_b, area, province, city, industry, website, datasource, "
                + "lastupdatetime, remarks) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, sysdate(6), ?)";
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

            InputStream inp = new FileInputStream(file);
            Workbook wb;
            wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = sheet.getLastRowNum();

            for (int i = 1; i <= rowNum; i++) //跳过第一行表头
            {

                //数据日期
                ps.setString(1, dateStr);
                //公司代码
                ps.setString(2, sheet.getRow(i).getCell(0).getStringCellValue().trim());
                //公司简称
                ps.setString(3, sheet.getRow(i).getCell(1).getStringCellValue().trim());
                //公司全称
                ps.setString(4, sheet.getRow(i).getCell(2).getStringCellValue().trim());
                //英文名称
                ps.setString(5, sheet.getRow(i).getCell(3).getStringCellValue().trim());
                //注册地址
                ps.setString(6, sheet.getRow(i).getCell(4).getStringCellValue().trim());
                //A股代码
                ps.setString(7, sheet.getRow(i).getCell(5).getStringCellValue().trim());
                //A股简称
                ps.setString(8, sheet.getRow(i).getCell(6).getStringCellValue().trim());
                //A股上市日期
                ps.setString(9, sheet.getRow(i).getCell(7).getStringCellValue().trim().replace("-", ""));
                //A股总股本
                ps.setDouble(10, Double.valueOf(sheet.getRow(i).getCell(8).getStringCellValue().trim().replace(",", "")) / 10000);
                //A股流通股本
                ps.setDouble(11, Double.valueOf(sheet.getRow(i).getCell(9).getStringCellValue().trim().replace(",", "")) / 10000);
                //B股代码
                ps.setString(12, sheet.getRow(i).getCell(10).getStringCellValue().trim());
                //B股简称
                ps.setString(13, sheet.getRow(i).getCell(11).getStringCellValue().trim());
                //B股上市日期
                ps.setString(14, sheet.getRow(i).getCell(12).getStringCellValue().trim().replace("-", ""));
                //B股总股本
                ps.setDouble(15, Double.valueOf(sheet.getRow(i).getCell(13).getStringCellValue().trim().replace(",", "")) / 10000);
                //B股流通股本
                ps.setDouble(16, Double.valueOf(sheet.getRow(i).getCell(14).getStringCellValue().trim().replace(",", "")) / 10000);
                //地区
                ps.setString(17, sheet.getRow(i).getCell(15).getStringCellValue().trim());
                //省份
                ps.setString(18, sheet.getRow(i).getCell(16).getStringCellValue().trim());
                //城市
                ps.setString(19, sheet.getRow(i).getCell(17).getStringCellValue().trim());
                //所属行业
                ps.setString(20, sheet.getRow(i).getCell(18).getStringCellValue().trim());
                //公司网址
                ps.setString(21, sheet.getRow(i).getCell(19).getStringCellValue().trim());
                //数据来源
                ps.setString(22, dataSource);
                //备注
                ps.setString(23, "");

                ps.addBatch();
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
            }

            ps.executeBatch(); // insert remaining records
            ps.close();
            conn.close();
            inp.close();
        } catch (SQLException sqlex) {
            logger.error("SQL执行失败！" + sqlex.getMessage());
            return false;
        } catch (IOException ioex) {
            logger.error("IOException！" + ioex.getMessage());
            return false;
        } catch (InvalidFormatException ifex) {
            logger.error("InvalidFormatException！" + ifex.getMessage());
            return false;
        }

        return true;
    }

    /**
     * 下载深圳上市公司信息，并保存在指定的文件夹内。
     *
     * @param filePath 文件保存路径，末尾不需要分隔符，如“E:/Documents and Settings/HHQ/桌面/stock”
     * @param fileName 保存的文件名称，不带后缀
     * @return 下载成功返回true，下载失败返回false
     */
    private boolean downloadStockCorpInfo(String filePath, String fileName) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(SzStockCorpBreifInfoImporter.class);

        String urlStr = "http://www.szse.cn/szseWeb/ShowReport.szse?SHOWTYPE=xlsx&CATALOGID=1110&tab1PAGENUM=1&ENCODE=1&TABKEY=tab1";
        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();
            OutputStream os = new FileOutputStream(new File(filePath + "/" + fileName + ".xlsx"));

            byte[] b;
            b = new byte[1024];
            int n = 0;

            while ((n = is.read(b)) != -1) {
                os.write(b, 0, n);
            }

            os.close();
            is.close();
            //System.out.println("文件 \"" + fileName + ".xlsx\" 下载成功");
            logger.debug("文件 \"" + fileName + ".xls\" 下载成功");
            return true;
        } catch (MalformedURLException ex) {
            //System.out.println("下载失败!");
            logger.error("MalformedURLException!" + ex.getMessage());
            return false;
        } catch (IOException ex) {
            //System.out.println("下载失败!");
            logger.error("MalformedURLException!" + ex.getMessage());
            return false;
        }

    }
}
