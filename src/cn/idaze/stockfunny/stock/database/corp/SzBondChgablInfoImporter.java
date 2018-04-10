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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 导入深市可转债信息
 *
 * @author hhq
 */
public class SzBondChgablInfoImporter {

    public static void main(String[] args) {
        SzBondChgablInfoImporter importer = new SzBondChgablInfoImporter();
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
        Logger logger = Logger.getLogger(SzBondChgablInfoImporter.class);
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
        String fileName = "深市可转债列表_" + dateStr;
        //数据来源：SZ--深交所网站
        String dataSource = "SZ";
        boolean b;

        SzBondChgablInfoImporter importer = new SzBondChgablInfoImporter();
        if (new File(filePath + "/" + fileName + ".xlsx").exists() == true) {//文件存在
            logger.debug("文件\"" + filePath + "/" + fileName + ".xlsx\"已存在，直接导入");
            if (importer.importChgBondInfo(filePath + "/" + fileName + ".xlsx", dateStr, dataSource) == true) {
                logger.debug("深市可转债文件导入成功！");
                return true;
            } else {
                logger.debug("深市可转债文件导入失败！");
                return false;
            }
        } else {//文件不存在，先下载再导入
            if (dateStr.compareTo(new SimpleDateFormat("yyyyMMdd").format(new Date())) < 0) {
                //System.out.println("要导入的数据文件不存在，且指定要下载的日期小于当前日期，导入失败！");
                logger.debug("要导入的数据文件不存在，且指定要下载的日期小于当前日期，导入失败！");
                return false;
            } else {
                b = importer.downloadChgablBondInfo(filePath, fileName, dateStr);
                if (b == true) {
                    logger.debug("文件下载成功，开始导入数据库...");

                    if (importer.importChgBondInfo(filePath + "/" + fileName + ".xlsx", dateStr, dataSource) == true) {
                        logger.debug("深市可转债文件导入成功！");
                        return true;
                    } else {
                        logger.debug("深市可转债文件导入失败！");
                        return false;
                    }

                } else {
                    logger.debug("深市可转债文件下载失败！");
                    return false;
                }
            }

        }
    }

    /**
     * 导入深市可转债列表。文件格式为.xlsx文件
     *
     * @param file 要导入的文件
     * @param dateStr 要导入的数据日期
     * @param dataSource 数据来源
     * @return
     */
    public boolean importChgBondInfo(String file, String dateStr, String dataSource) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(SzBondChgablInfoImporter.class);

        String delSql = "delete from BOND_CHGABL_SZ where dte=? and datasource=?";
        String insertSql = "insert into BOND_CHGABL_SZ (dte, bondid, abbrname, ipodate, "
                + "issuenum, chgprice, unchgnum, unchgper, begdate, enddate, "
                + "stockid, validdate, datasource, lastupdatetime, remarks) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate(6), ?)";
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

            XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(file));
            XSSFSheet sheet = workbook.getSheetAt(0);
            //int columnum = sheet.getColumns(); //得到列数 
            int rownum = sheet.getLastRowNum(); //得到行数 
            System.out.println("开始处理文件：" + file + "，共" + rownum + "条记录");

            if (sheet.getRow(1).getCell(0).getStringCellValue().trim().equals("没有找到符合条件的数据！") == true) {
                logger.info("没有找到符合条件的数据，直接返回成功！");
                ps.close();
                conn.close();
                return true;
            }

            for (int i = 1; i <= rownum; i++) //跳过第一行表头
            {
                ps.setString(1, dateStr);
                ps.setString(2, sheet.getRow(i).getCell(0).getStringCellValue().trim());
                ps.setString(3, sheet.getRow(i).getCell(1).getStringCellValue().trim());
                ps.setString(4, sheet.getRow(i).getCell(2).getStringCellValue().replace("-", ""));
                ps.setDouble(5, Double.valueOf(sheet.getRow(i).getCell(3).getStringCellValue().trim().replace(",", "")));
                ps.setDouble(6, Double.valueOf(sheet.getRow(i).getCell(4).getStringCellValue().trim().replace(",", "")));
                ps.setDouble(7, Double.valueOf(sheet.getRow(i).getCell(5).getStringCellValue().trim().replace(",", "")));
                ps.setDouble(8, Double.valueOf(sheet.getRow(i).getCell(6).getStringCellValue().trim().replace(",", "")));
                ps.setString(9, sheet.getRow(i).getCell(7).getStringCellValue().trim().replace("-", ""));
                ps.setString(10, sheet.getRow(i).getCell(8).getStringCellValue().trim().replace("-", ""));
                ps.setString(11, sheet.getRow(i).getCell(9).getStringCellValue().trim());
                ps.setString(12, sheet.getRow(i).getCell(10).getStringCellValue().replace("-", ""));
                ps.setString(13, dataSource);
                ps.setString(14, "");

                ps.addBatch();
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
            }

            ps.executeBatch(); // insert remaining records
            ps.close();
            conn.close();

        } catch (SQLException sqlex) {
            logger.error("SQL执行失败！" + sqlex.getMessage());
            return false;
        } catch (IOException ioex) {
            logger.error("IOException！" + ioex.getMessage());
            return false;
        }

        return true;
    }

    /**
     * 下载深圳可转债信息，并保存在指定的文件夹内。获取的是指定日期前一天的可转债列表。
     *
     * @param filePath 文件保存路径，末尾不需要分隔符，如“E:/Documents and Settings/HHQ/桌面/stock”
     * @param fileName 保存的文件名称，不带后缀
     * @param dateStr 日期
     * @return 下载成功返回true，下载失败返回false
     */
    private boolean downloadChgablBondInfo(String filePath, String fileName, String dateStr) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(SzBondChgablInfoImporter.class);

        //获取前一天日期
        Calendar calendar = Calendar.getInstance();
        //calendar.setTime(new Date());  
        //calendar.setTime(new Date());
        calendar.set(Integer.valueOf(dateStr.trim().substring(0, 4)), Integer.valueOf(dateStr.trim().substring(4, 6)) - 1, Integer.valueOf(dateStr.trim().substring(6)));
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String beforeDay = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
        String urlStr = "http://www.szse.cn/szseWeb/ShowReport.szse?SHOWTYPE=xlsx&CATALOGID=1277&txtDate="
                + beforeDay + "&ENCODE=1&TABKEY=tab1";
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
