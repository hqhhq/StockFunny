/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database;

import cn.idaze.stockfunny.database.MSSQLConnector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 从合并后的文件导入数据至数据库
 *
 * @author HHQ
 */
public class TDXStockTradeInfoFromMergedFileImporter {

    public static void main(String[] args) {
        File file = new File("E:/tmp/desFile.txt");
        String cycle = "D1";
        String adjtype = "F"; //Forward、Backward、No
        String beginDate = "19880101";
        String endDate = "20160930";
        try {
            System.out.println("Start...");
            new TDXStockTradeInfoFromMergedFileImporter().importTradeInfo(file, cycle, adjtype, beginDate, endDate);
            System.out.println("End...");
        } catch (FileNotFoundException ex) {
            System.out.println("文件【" + file.getName() + "】不存在！");
        }
    }

    /**
     * 导入指定文件数据
     *
     * @param file 数据文件
     * @param cycle 数据周期
     * @param adjtype 复权类型
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @return
     */
    private boolean importTradeInfo(File file, String cycle, String adjtype, String beginDate, String endDate) throws FileNotFoundException {
        String delSql = "delete from T_stocktradeinfo where cycle=? and adjtype=? and datasource='TDX' and dte between ? and ?";
        String insertSql = "insert into T_stocktradeinfo (stockid, cycle, dte, [open], "
                + "high, low, [close], volumn, totalmoney, adjtype, datasource, importtime) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, getdate())";
        MSSQLConnector connector = new MSSQLConnector();
        Connection sqlConn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);


        try {
            ps.setString(1, cycle);
            ps.setString(2, adjtype);
            ps.setString(3, beginDate);
            ps.setString(4, endDate);
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 5000;
            int count = 0;

            //BufferedReader in = new BufferedReader(new FileReader(file));
            //数据文件为GBK编码，用FileReader不支持指定编码
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
            String newLine;
            while ((newLine = in.readLine()) != null) {

                String[] strs = newLine.split(",");

                String stockId = strs[0];
                String tmpDate = strs[1];
                if (tmpDate.compareTo(beginDate) < 0 || tmpDate.compareTo(endDate) > 0) { //不在指定日期范围的数据不导入
                    continue;
                }

                ps.setString(1, stockId);
                ps.setString(2, cycle);
                ps.setString(3, strs[1].trim());
                ps.setDouble(4, Double.valueOf(strs[2].trim()));
                ps.setDouble(5, Double.valueOf(strs[3].trim()));
                ps.setDouble(6, Double.valueOf(strs[4].trim()));
                ps.setDouble(7, Double.valueOf(strs[5].trim()));
                ps.setDouble(8, Double.valueOf(strs[6].trim()));
                ps.setDouble(9, Double.valueOf(strs[7].trim()));
                ps.setString(10, adjtype);
                ps.setString(11, "TDX");

                ps.addBatch();
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
            }

            ps.executeBatch(); // insert remaining records
            ps.close();
            sqlConn.close();

            in.close();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }

        return true;
    }
}
