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
 * 导入深市公司名称变更历史
 *
 * @author HHQ
 */
public class SZCorpNameChgHisImporter {
    // 变更日期

    private String chgDte;
    // 证券代码
    private String stockId;
    // 证券简称
    private String abbrName;
    // 上海深市标识
    private String shszFlag;
    // 简称全称标识
    private String shortFullFlag;
    // 原名称
    private String oldName;
    // 新名称
    private String newName;

    /**
     * @return the chgDte
     */
    public String getChgDte() {
        return chgDte;
    }

    /**
     * @param chgDte the chgDte to set
     */
    public void setChgDte(String chgDte) {
        this.chgDte = chgDte;
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
     * @return the abbrName
     */
    public String getAbbrName() {
        return abbrName;
    }

    /**
     * @param abbrName the abbrName to set
     */
    public void setAbbrName(String abbrName) {
        this.abbrName = abbrName;
    }

    /**
     * @return the shszFlag
     */
    public String getShszFlag() {
        return shszFlag;
    }

    /**
     * @param shszFlag the shszFlag to set
     */
    public void setShszFlag(String shszFlag) {
        this.shszFlag = shszFlag;
    }

    /**
     * @return the shortFullFlag
     */
    public String getShortFullFlag() {
        return shortFullFlag;
    }

    /**
     * @param shortFullFlag the shortFullFlag to set
     */
    public void setShortFullFlag(String shortFullFlag) {
        this.shortFullFlag = shortFullFlag;
    }

    /**
     * @return the oldName
     */
    public String getOldName() {
        return oldName;
    }

    /**
     * @param oldName the oldName to set
     */
    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    /**
     * @return the newName
     */
    public String getNewName() {
        return newName;
    }

    /**
     * @param newName the newName to set
     */
    public void setNewName(String newName) {
        this.newName = newName;
    }

    /**
     *
     * @param file 要导入的文件，为深市网站下载
     * @param shszFlag 1-沪市；2－深市
     * @param shortFullFlag 简称全称标识，S-简称；F-全称
     * @return
     */
    public boolean importData(String file, String shszFlag, String shortFullFlag) {
        String delSql = "delete from corpnamechghis where shszflag=? and shortfullflag=? ";
        String insertSql = "insert into corpnamechghis (chgdte, stockid, abbrname, shszflag, shortfullflag, "
                + "oldname, newname) "
                + " values (?, ?, ?, ?, ?, ?, ?)";

        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);

        try {
            ps.setString(1, shszFlag);
            ps.setString(2, shortFullFlag);
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

            HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
            HSSFSheet sheet = workbook.getSheetAt(0);
            //int columnum = sheet.getColumns(); //得到列数 
            int rownum = sheet.getLastRowNum(); //得到行数 
            System.out.println("开始处理文件：" + file + "，共" + rownum + "条记录");
            if (shszFlag.equals("2") == true) { //深市数据处理
                for (int i = 1; i <= rownum; i++) //跳过第一行表头
                {
                    ps.setString(1, sheet.getRow(i).getCell(0).getStringCellValue().trim().replace("-", ""));
                    ps.setString(2, sheet.getRow(i).getCell(1).getStringCellValue().trim());
                    ps.setString(3, sheet.getRow(i).getCell(2).getStringCellValue().trim());
                    ps.setString(4, shszFlag);
                    ps.setString(5, shortFullFlag);
                    ps.setString(6, sheet.getRow(i).getCell(3).getStringCellValue().trim());
                    ps.setString(7, sheet.getRow(i).getCell(4).getStringCellValue().trim());

                    ps.addBatch();
                    if (++count % batchSize == 0) {
                        ps.executeBatch();
                    }
                }

                ps.executeBatch(); // insert remaining records
                ps.close();
                conn.close();
            } else {  //沪市数据处理
                //TODO:沪市数据处理
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
        SZCorpNameChgHisImporter importer = new SZCorpNameChgHisImporter();
        importer.importData("E:/Documents and Settings/HHQ/桌面/stock/database/stock/简称变更_深交所_20160930.xls", "2", "S");
        importer.importData("E:/Documents and Settings/HHQ/桌面/stock/database/stock/全称变更_深交所_20160930.xls", "2", "F");
    }
}
