/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database;

import cn.idaze.stockfunny.database.MSSQLConnector;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * 导入沪市上市公司基本信息 由于沪市网页改版，暂改为从excel中加载基本信息
 *
 * @author hhq
 */
public class ShStockCorpBasicImporter {

    /**
     *
     * @param file 要导入的文件，为沪市网站下载
     * @param shszFlag 1-沪市；2－深市
     * @param abFlag 1-A股；2-B股
     * @param date 要导入的数据日期
     * @return
     */
    public boolean importData(String file, String shszFlag, String abFlag, String date) {
        String delSql = "delete from stockcorp where dte=? and shszflag=? and abflag=?";
        String insertSql = "insert into stockcorp (dte, corpid, stockid, abflag, shszflag, abbrname, "
                + "fullname, engname, regaddr, ipodate, capitalstock, "
                + "tradableshares, areacode, area, provincecode, province, "
                + "citycode, city, industrycode, industry, website, remarks) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?)";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);

        try {
            ps.setString(1, date);
            ps.setString(2, shszFlag);
            ps.setString(3, abFlag);
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

            HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
            HSSFSheet sheet = workbook.getSheetAt(0);
            //int columnum = sheet.getColumns(); //得到列数 
            int rownum = sheet.getLastRowNum(); //得到行数
            if (shszFlag.equals("1") == true) { //沪市数据处理
                for (int i = 1; i <= rownum; i++) //跳过第一行表头
                {
                    ps.setString(1, date);
                    //公司代码
                    ps.setString(2, (int)(sheet.getRow(i).getCell(0).getNumericCellValue())+"");
                    //股票代码
                    ps.setString(3, (int)(sheet.getRow(i).getCell(2).getNumericCellValue())+"");
                    ps.setString(4, abFlag);
                    ps.setString(5, shszFlag);
                    //简称
                    ps.setString(6, sheet.getRow(i).getCell(3).getStringCellValue().trim());
                    //全称
                    ps.setString(7, "");
                    //英文名称
                    ps.setString(8, "");
                    //注册地址
                    ps.setString(9, "");
                    //IPO日期
                    ps.setString(10, sheet.getRow(i).getCell(4).getStringCellValue().trim().replace("-", ""));
                    //总股本
                    ps.setDouble(11, sheet.getRow(i).getCell(5).getNumericCellValue());
                    //流通股本
                    ps.setDouble(12, sheet.getRow(i).getCell(6).getNumericCellValue());
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


                    ps.addBatch();
                    if (++count % batchSize == 0) {
                        ps.executeBatch();
                    }


                }

                ps.executeBatch(); // insert remaining records
                ps.close();
                conn.close();
            } else { 
               
            }
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
     * 获取excel中的数据，以文本形式返回
     *
     * @param cell
     * @return
     */
    private static String getCellString(HSSFCell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_NUMERIC:
                return cell.getNumericCellValue() + "";
            case HSSFCell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            case HSSFCell.CELL_TYPE_FORMULA:
                return cell.getCellFormula();
            case HSSFCell.CELL_TYPE_BLANK:
                return "";
            case HSSFCell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue() + "";
            case HSSFCell.CELL_TYPE_ERROR:
                return cell.getErrorCellValue() + "";
        }
        return "";
    }

    public static void main(String[] args) {
        ShStockCorpBasicImporter importer = new ShStockCorpBasicImporter();
        importer.importData("E:/Documents and Settings/HHQ/桌面/stock/database/stock/沪市A股列表_20160930.xls", "1", "1", "20160930");
        importer.importData("E:/Documents and Settings/HHQ/桌面/stock/database/stock/沪市B股列表_20160930.xls", "1", "2", "20160930");
    }
}
