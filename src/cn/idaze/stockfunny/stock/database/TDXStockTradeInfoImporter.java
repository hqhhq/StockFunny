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
import java.util.ArrayList;
import java.util.List;

/**
 * 从通达信文件中读取股票分时数据导入数据库
 *
 * @author HHQ
 */
public class TDXStockTradeInfoImporter {

    public static void main(String[] args) {
        String path = "E:/tmp/forward/export";
        String cycle = "D1";
        String adjtype = "F"; //Forward、Backward、No
        String beginDate = "19880101";
        String endDate = "20160930";
        new TDXStockTradeInfoImporter().importTradeInfos(path, cycle, adjtype, beginDate, endDate);
    }

    private void importTradeInfos(String path, String cycle, String adjtype, String beginDate, String endDate) {
        for (File f : getFiles(path)) {
            try {
                if (importTradeInfo(f, cycle, adjtype, beginDate, endDate) == false) {
                    System.out.println("######文件【" + f.getName() + "】导入失败！######");
                } else {
                    System.out.println("文件【" + f.getName() + "】导入成功！");
                }
            } catch (FileNotFoundException ex) {
                System.out.println("文件【" + f.getName() + "】不存在！");
            }
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
        String delSql = "delete from stocktradeinfo where stockid=? and cycle=? and adjtype=? and datasource='TDX' and dte between ? and ?";
        String insertSql = "insert into stocktradeinfo (stockid, cycle, dte, [open], "
                + "high, low, [close], volumn, totalmoney, adjtype, datasource, importtime) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, getdate())";
        MSSQLConnector connector = new MSSQLConnector();
        Connection sqlConn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);

        String stockId = file.getName().split("\\.")[0].split("#")[1];

        try {
            ps.setString(1, stockId);
            ps.setString(2, cycle);
            ps.setString(3, adjtype);
            ps.setString(4, beginDate);
            ps.setString(5, endDate);
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            //BufferedReader in = new BufferedReader(new FileReader(file));
            //数据文件为GBK编码，用FileReader不支持指定编码
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
            String newLine;
            while ((newLine = in.readLine()) != null) {
                //跳过非数据行
                if (newLine.trim().isEmpty() || newLine.trim().equals("数据来源:通达信")) {
                    continue;
                }

                String[] strs = newLine.split(",");

                String tmpDate = strs[0].trim().replaceAll("/", "");
                if (tmpDate.compareTo(beginDate) < 0 || tmpDate.compareTo(endDate) > 0) { //不在指定日期范围的数据不导入
                    continue;
                }

                ps.setString(1, stockId);
                ps.setString(2, cycle);
                ps.setString(3, strs[0].trim().replaceAll("/", ""));
                ps.setDouble(4, Double.valueOf(strs[1].trim()));
                ps.setDouble(5, Double.valueOf(strs[2].trim()));
                ps.setDouble(6, Double.valueOf(strs[3].trim()));
                ps.setDouble(7, Double.valueOf(strs[4].trim()));
                ps.setDouble(8, Double.valueOf(strs[5].trim()));
                ps.setDouble(9, Double.valueOf(strs[6].trim()));
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
            return false;
        } catch (IOException ex) {
            return false;
        } catch (SQLException sqlex) {
            return false;
        }

        return true;
    }

    //获取文件夹下所有文件
    private List<File> getFiles(String path) {
        File file = new File(path);
        File[] tmpFiles = file.listFiles();
        List<File> fileList = new ArrayList<File>();
        for (File f : tmpFiles) {
            if (f.isFile()) {
                fileList.add(f);
            }
        }

        return fileList;
    }
}
