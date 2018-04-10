/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.corp;

import cn.idaze.stockfunny.database.MSSQLConnector;
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
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * @deprecated 
 * 
 * 深圳上市公司信息导入器
 *
 * @author HHQ
 */
public class SzStockCorpImporter {

    public static void main(String[] args) {
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filePath = "E:/Documents and Settings/HHQ/桌面/stock/database/stock";
        String fileName = "深圳上市公司信息列表_" + dateStr;
        //数据来源：SZ--深交所网站
        String dataSource = "SZ";
        boolean b;

        SzStockCorpImporter importer = new SzStockCorpImporter();
        b = importer.downloadSzStockCorpInfo(filePath, fileName);
        if (b == true) {
            System.out.println("文件下载成功，开始导入数据库...");
            if (importer.importData(filePath + "/" + fileName + ".xlsx", dateStr, dataSource) == true) {
                System.out.println("文件导入成功！");
            } else {
                System.out.println("文件导入失败！");
            }
        }
    }

    /**
     * 导入深市上市公司数据。文件格式为.xlsx文件
     * @param file 要导入的文件
     * @param dateStr 要导入的数据日期
     * @param dataSource 数据来源
     * @return
     */
    public boolean importData(String file, String dateStr, String dataSource) {
        String delSql = "delete from stockcorp where dte=? and datasource=? and shszflag='2' ";
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
                //读取A股代码
                String idA = sheet.getRow(i).getCell(5).getStringCellValue().trim();
                if (idA.equals("") == false) { //A股代码不为空
                    //数据日期
                    ps.setString(1, dateStr);
                    //公司代码
                    ps.setString(2, sheet.getRow(i).getCell(0).getStringCellValue().trim());
                    //A股代码
                    ps.setString(3, idA);
                    //设置为A股
                    ps.setString(4, "1");
                    //设置为深市股票
                    ps.setString(5, "2");
                    //A股简称
                    ps.setString(6, sheet.getRow(i).getCell(6).getStringCellValue().trim());
                    //公司全称
                    ps.setString(7, sheet.getRow(i).getCell(2).getStringCellValue().trim());
                    //英文名称
                    ps.setString(8, sheet.getRow(i).getCell(3).getStringCellValue().trim());
                    //注册地址
                    ps.setString(9, sheet.getRow(i).getCell(4).getStringCellValue().trim());
                    //上市日期
                    ps.setString(10, sheet.getRow(i).getCell(7).getStringCellValue().trim().replace("-", ""));
                    //总股本
                    ps.setDouble(11, Double.valueOf(sheet.getRow(i).getCell(8).getStringCellValue().trim().replace(",", "")));
                    //流通股本
                    ps.setDouble(12, Double.valueOf(sheet.getRow(i).getCell(9).getStringCellValue().trim().replace(",", "")));
                    //地区代码
                    ps.setString(13, "");
                    //地区
                    ps.setString(14, sheet.getRow(i).getCell(15).getStringCellValue().trim());
                    //省份代码
                    ps.setString(15, "");
                    //省份
                    ps.setString(16, sheet.getRow(i).getCell(16).getStringCellValue().trim());
                    //城市代码
                    ps.setString(17, "");
                    //城市
                    ps.setString(18, sheet.getRow(i).getCell(17).getStringCellValue().trim());
                    //所属行业代码
                    ps.setString(19, "");
                    //所属行业
                    ps.setString(20, sheet.getRow(i).getCell(18).getStringCellValue().trim());
                    //公司网址
                    ps.setString(21, sheet.getRow(i).getCell(19).getStringCellValue().trim());
                    //备注
                    ps.setString(22, "");
                    //数据来源
                    ps.setString(23, dataSource);

                    ps.addBatch();
                    if (++count % batchSize == 0) {
                        ps.executeBatch();
                    }
                }

                //读取B股代码
                String idB = sheet.getRow(i).getCell(10).getStringCellValue().trim();
                if (idB.equals("") == false) { //B股代码不为空
                    //数据日期
                    ps.setString(1, dateStr);
                    //公司代码
                    ps.setString(2, sheet.getRow(i).getCell(0).getStringCellValue().trim());
                    //B股代码
                    ps.setString(3, idB);
                    //设置为B股
                    ps.setString(4, "2");
                    //设置为深市股票
                    ps.setString(5, "2");
                    //B股简称
                    ps.setString(6, sheet.getRow(i).getCell(11).getStringCellValue().trim());
                    //公司全称
                    ps.setString(7, sheet.getRow(i).getCell(2).getStringCellValue().trim());
                    //英文名称
                    ps.setString(8, sheet.getRow(i).getCell(3).getStringCellValue().trim());
                    //注册地址
                    ps.setString(9, sheet.getRow(i).getCell(4).getStringCellValue().trim());
                    //上市日期
                    ps.setString(10, sheet.getRow(i).getCell(12).getStringCellValue().trim().replace("-", ""));
                    //总股本
                    ps.setDouble(11, Double.valueOf(sheet.getRow(i).getCell(13).getStringCellValue().trim().replace(",", "")));
                    //流通股本
                    ps.setDouble(12, Double.valueOf(sheet.getRow(i).getCell(14).getStringCellValue().trim().replace(",", "")));
                    //地区代码
                    ps.setString(13, "");
                    //地区
                    ps.setString(14, sheet.getRow(i).getCell(15).getStringCellValue().trim());
                    //省份代码
                    ps.setString(15, "");
                    //省份
                    ps.setString(16, sheet.getRow(i).getCell(16).getStringCellValue().trim());
                    //城市代码
                    ps.setString(17, "");
                    //城市
                    ps.setString(18, sheet.getRow(i).getCell(17).getStringCellValue().trim());
                    //所属行业代码
                    ps.setString(19, "");
                    //所属行业
                    ps.setString(20, sheet.getRow(i).getCell(18).getStringCellValue().trim());
                    //公司网址
                    ps.setString(21, sheet.getRow(i).getCell(19).getStringCellValue().trim());
                    //备注
                    ps.setString(22, "");
                    //数据来源
                    ps.setString(23, "SZ");

                    ps.addBatch();
                    if (++count % batchSize == 0) {
                        ps.executeBatch();
                    }
                }
            }

            ps.executeBatch(); // insert remaining records
            ps.close();
            conn.close();
            inp.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        } catch (IOException ioex) {
            ioex.printStackTrace();
            return false;
        } catch (InvalidFormatException ifex) {
            ifex.printStackTrace();
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
    private boolean downloadSzStockCorpInfo(String filePath, String fileName) {
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
            System.out.println("文件 \"" + fileName + ".xlsx\" 下载成功");
            return true;
        } catch (MalformedURLException ex) {
            System.out.println("下载失败!");
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            System.out.println("下载失败!");
            ex.printStackTrace();
            return false;
        }

    }
}
