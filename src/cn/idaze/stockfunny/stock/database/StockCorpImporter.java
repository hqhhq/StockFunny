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
 * 导入上市公司信息
 * 由于无法直接在上海证券交易所下载到所有公司的excel，此处只处理深圳交易所股票，沪市股票通过网页解析
 *
 * @author hhq
 */
public class StockCorpImporter {

    //公司代码
    private String corpId;
    //股票代码
    private String stockId;
    //A股、B股标识
    private String abFlag;
    //沪市、深市股票标识
    private String shszFlag;
    //公司简称
    private String abbrName;
    //公司全称
    private String fullName;
    //英文名称
    private String engName;
    //注册地址
    private String regAddr;
    //上市日期
    private String ipoDate;
    //总股本
    private double capitalStock;
    //流通股本
    private double tradableShares;
    //地区代码
    private String areaCode;
    //地区，如华南、华东
    private String area;
    //省份代码，由地区代码作前缀
    private String provinceCode;
    //省份，如广东、北京
    private String province;
    //城市代码，由地区、省市代码当前缀
    private String cityCode;
    //城市，如深圳市
    private String city;
    //所属行业代码
    private String industryCode;
    //所属行业
    private String industry;
    //公司网址
    private String website;
    //备注
    private String remarks;

    /**
     * @return the corpId
     */
    public String getCorpId() {
        return corpId;
    }

    /**
     * @param corpId the corpId to set
     */
    public void setCorpId(String corpId) {
        this.corpId = corpId;
    }

    /**
     * @return the abFlag
     */
    public String getAbFlag() {
        return abFlag;
    }

    /**
     * @param abFlag the abFlag to set
     */
    public void setAbFlag(String abFlag) {
        this.abFlag = abFlag;
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
     * @return the fullName
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * @param fullName the fullName to set
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * @return the engName
     */
    public String getEngName() {
        return engName;
    }

    /**
     * @param engName the engName to set
     */
    public void setEngName(String engName) {
        this.engName = engName;
    }

    /**
     * @return the regAddr
     */
    public String getRegAddr() {
        return regAddr;
    }

    /**
     * @param regAddr the regAddr to set
     */
    public void setRegAddr(String regAddr) {
        this.regAddr = regAddr;
    }

    /**
     * @return the ipoDate
     */
    public String getIpoDate() {
        return ipoDate;
    }

    /**
     * @param ipoDate the ipoDate to set
     */
    public void setIpoDate(String ipoDate) {
        this.ipoDate = ipoDate;
    }

    /**
     * @return the capitalStock
     */
    public double getCapitalStock() {
        return capitalStock;
    }

    /**
     * @param capitalStock the capitalStock to set
     */
    public void setCapitalStock(double capitalStock) {
        this.capitalStock = capitalStock;
    }

    /**
     * @return the tradableShares
     */
    public double getTradableShares() {
        return tradableShares;
    }

    /**
     * @param tradableShares the tradableShares to set
     */
    public void setTradableShares(double tradableShares) {
        this.tradableShares = tradableShares;
    }

    /**
     * @return the areaCode
     */
    public String getAreaCode() {
        return areaCode;
    }

    /**
     * @param areaCode the areaCode to set
     */
    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    /**
     * @return the area
     */
    public String getArea() {
        return area;
    }

    /**
     * @param area the area to set
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * @return the provinceCode
     */
    public String getProvinceCode() {
        return provinceCode;
    }

    /**
     * @param provinceCode the provinceCode to set
     */
    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    /**
     * @return the province
     */
    public String getProvince() {
        return province;
    }

    /**
     * @param province the province to set
     */
    public void setProvince(String province) {
        this.province = province;
    }

    /**
     * @return the cityCode
     */
    public String getCityCode() {
        return cityCode;
    }

    /**
     * @param cityCode the cityCode to set
     */
    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the industryCode
     */
    public String getIndustryCode() {
        return industryCode;
    }

    /**
     * @param industryCode the industryCode to set
     */
    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    /**
     * @return the industry
     */
    public String getIndustry() {
        return industry;
    }

    /**
     * @param industry the industry to set
     */
    public void setIndustry(String industry) {
        this.industry = industry;
    }

    /**
     * @return the website
     */
    public String getWebsite() {
        return website;
    }

    /**
     * @param website the website to set
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * @return the remarks
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * @param remarks the remarks to set
     */
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    /**
     *
     * @param file 要导入的文件，为深市网站下载
     * @param shszFlag 1-沪市；2－深市
     * @param date 要导入的数据日期
     * @return
     */
    public boolean importData(String file, String shszFlag, String date) {
        String delSql = "delete from stockcorp where dte=? and shszflag=?";
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
            ps.execute();

            ps = connector.prepareStmt(insertSql);
            
            final int batchSize = 1000;
            int count = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

            HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
            HSSFSheet sheet = workbook.getSheetAt(0);
            //int columnum = sheet.getColumns(); //得到列数 
            int rownum = sheet.getLastRowNum(); //得到行数 
            if (shszFlag.equals("2") == true) { //深市数据处理
                for (int i = 1; i <= rownum; i++) //跳过第一行表头
                {
                    //读取A股代码
                    String ida = getCellString(sheet.getRow(i).getCell(5)).trim();
                    if (ida.equals("") == false) {
                        ida = Float.valueOf(ida).intValue() + "";
                        //数据日期
                        ps.setString(1, date);
                        //公司代码
                        ps.setString(2, getCellString(sheet.getRow(i).getCell(0)).trim());
                        //A股代码,导出来的数据有可能没有前缀0
                        ps.setString(3, ("" + (1000000 + Integer.valueOf(ida))).substring(1));
                        //设置为A股
                        ps.setString(4, "1");
                        //设置为深市股票
                        ps.setString(5, "2");
                      
                        //A股简称
                        ps.setString(6, getCellString(sheet.getRow(i).getCell(6)).trim());
                        //公司全称
                        ps.setString(7, getCellString(sheet.getRow(i).getCell(2)).trim());
                        //英文名称
                        ps.setString(8, getCellString(sheet.getRow(i).getCell(3)).trim());
                        //注册地址
                        ps.setString(9, getCellString(sheet.getRow(i).getCell(4)).trim());
                        //上市日期，深交所网站开始导出的是数字格式，后来改为字符串格式
                        //ps.setString(10, sdf.format(sheet.getRow(i).getCell(7).getDateCellValue()));
                        ps.setString(10, sheet.getRow(i).getCell(7).getStringCellValue().trim().replace("-", ""));
                        
                        //总股本，深交所网站开始导出的是数字格式，后来改为字符串格式
                        //ps.setDouble(11, Double.valueOf(getCellString(sheet.getRow(i).getCell(8)).trim()));
                        ps.setDouble(11, Double.valueOf(getCellString(sheet.getRow(i).getCell(8)).trim().replace(",", "")));
                        //流通股本，深交所网站开始导出的是数字格式，后来改为字符串格式
                        //ps.setDouble(12, Double.valueOf(getCellString(sheet.getRow(i).getCell(9)).trim()));
                        ps.setDouble(12, Double.valueOf(getCellString(sheet.getRow(i).getCell(9)).trim().replace(",", "")));
                        //地区代码
                        ps.setString(13, "");
                        //地区
                        ps.setString(14, getCellString(sheet.getRow(i).getCell(15)).trim());
                        //省份代码
                        ps.setString(15, "");
                        
                        //省份
                        ps.setString(16, getCellString(sheet.getRow(i).getCell(16)).trim());
                        //城市代码
                        ps.setString(17, "");
                        //城市
                        ps.setString(18, getCellString(sheet.getRow(i).getCell(17)).trim());
                        //所属行业代码
                        ps.setString(19, "");
                        //所属行业
                        ps.setString(20, getCellString(sheet.getRow(i).getCell(18)).trim());
                        
                        //公司网址
                        ps.setString(21, getCellString(sheet.getRow(i).getCell(19)).trim());
                        //备注
                        ps.setString(22, "");                        
                        
                        ps.addBatch();
                        if (++count % batchSize == 0) {
                            ps.executeBatch();
                        }
                    }

                    //读取B股代码
                    String idb = getCellString(sheet.getRow(i).getCell(10)).trim();
                    if (idb.equals("") == false) {
                        idb = Float.valueOf(idb).intValue() + "";
                        //数据日期
                        ps.setString(1, date);
                        //公司代码
                        ps.setString(2, getCellString(sheet.getRow(i).getCell(0)).trim());
                        //B股代码,导出来的数据有可能没有前缀0
                        ps.setString(3, ("" + (1000000 + Integer.valueOf(idb))).substring(1));
                        //设置为B股
                        ps.setString(4, "2");
                        //设置为深市股票
                        ps.setString(5, "2");
                        
                        //B股简称
                        ps.setString(6, getCellString(sheet.getRow(i).getCell(11)).trim());
                        //公司全称
                        ps.setString(7, getCellString(sheet.getRow(i).getCell(2)).trim());
                        //英文名称
                        ps.setString(8, getCellString(sheet.getRow(i).getCell(3)).trim());
                        //注册地址
                        ps.setString(9, getCellString(sheet.getRow(i).getCell(4)).trim());
                        //上市日期，深交所网站开始导出的是数字格式，后来改为字符串格式
                        //ps.setString(10, sdf.format(sheet.getRow(i).getCell(12).getDateCellValue()));
                        ps.setString(10, sheet.getRow(i).getCell(12).getStringCellValue().trim().replace("-", ""));
                        
                        //总股本，深交所网站开始导出的是数字格式，后来改为字符串格式
                        //ps.setDouble(11, Double.valueOf(getCellString(sheet.getRow(i).getCell(13)).trim()));
                        ps.setDouble(11, Double.valueOf(getCellString(sheet.getRow(i).getCell(13)).trim().replace(",", "")));
                        //流通股本，深交所网站开始导出的是数字格式，后来改为字符串格式
                        //ps.setDouble(12, Double.valueOf(getCellString(sheet.getRow(i).getCell(14)).trim()));
                        ps.setDouble(12, Double.valueOf(getCellString(sheet.getRow(i).getCell(14)).trim().replace(",", "")));
                        //地区代码
                        ps.setString(13, "");
                        //地区
                        ps.setString(14, getCellString(sheet.getRow(i).getCell(15)).trim());
                        //省份代码
                        ps.setString(15, "");
                        
                        //省份
                        ps.setString(16, getCellString(sheet.getRow(i).getCell(16)).trim());
                        //城市代码
                        ps.setString(17, "");
                        //城市
                        ps.setString(18, getCellString(sheet.getRow(i).getCell(17)).trim());
                        //所属行业代码
                        ps.setString(19, "");
                        //所属行业
                        ps.setString(20, getCellString(sheet.getRow(i).getCell(18)).trim());
                        
                        //公司网址
                        ps.setString(21, getCellString(sheet.getRow(i).getCell(19)).trim());
                        //备注
                        ps.setString(22, "");

                        ps.addBatch();
                        if (++count % batchSize == 0) {
                            ps.executeBatch();
                        }
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
        StockCorpImporter importer = new StockCorpImporter();
        importer.importData("E:/Documents and Settings/HHQ/桌面/stock/database/stock/上市公司列表_20160930.xls", "2", "20160930");
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
}
