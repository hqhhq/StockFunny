/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database;

import cn.idaze.stockfunny.database.MSSQLConnector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 抽取各股票交易信息
 *
 * @author HHQ
 */
public class StockHisTransImporter {

    /**
     * 导入股票历史交易信息至数据库
     *
     * @param stockCode 股票代码
     * @param shszFlag 沪市股市标识
     * @param szshFlag 沪深股票标识
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @param cycle 周期
     * @return
     */
    public boolean importTrans(String stockCode, String shszFlag, String beginDate, String endDate, String cycle) {
        String baseUrl = "http://table.finance.yahoo.com/table.csv?s=";
        URL url = null;
        URLConnection urlConn = null;
        InputStreamReader ins = null;
        BufferedReader in = null;

        String delSql = "delete from stockhistrans where stockid=? and cycle=? and dte between ? and ?";
        String insertSql = "insert into stockhistrans (stockid, cycle, dte, [open], "
                + "high, low, [close], volumn, totalmoney, adjpreclose, adjaftclose) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        MSSQLConnector connector = new MSSQLConnector();
        Connection sqlConn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);

        try {
            ps.setString(1, stockCode);
            ps.setString(2, cycle);
            ps.setString(3, beginDate);
            ps.setString(4, endDate);
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            if (shszFlag.equals("1")) {
                url = new URL(baseUrl + stockCode + ".ss"
                        + "&a=" + (Integer.valueOf(beginDate.substring(4, 6)) - 1)
                        + "&b=" + beginDate.substring(6, 8)
                        + "&c=" + beginDate.substring(0, 4)
                        + "&d=" + (Integer.valueOf(endDate.substring(4, 6)) - 1)
                        + "&e=" + endDate.substring(6, 8)
                        + "&f=" + endDate.substring(0, 4)
                        + "&g=" + cycle);
            } else if (shszFlag.equals("2")) {
                url = new URL(baseUrl + stockCode + ".sz"
                        + "&a=" + (Integer.valueOf(beginDate.substring(4, 6)) - 1)
                        + "&b=" + beginDate.substring(6, 8)
                        + "&c=" + beginDate.substring(0, 4)
                        + "&d=" + (Integer.valueOf(endDate.substring(4, 6)) - 1)
                        + "&e=" + endDate.substring(6, 8)
                        + "&f=" + endDate.substring(0, 4)
                        + "&g=" + cycle);
            } else {
                return false;
            }

            urlConn = url.openConnection();
            ins = new InputStreamReader(urlConn.getInputStream(), "UTF-8");
            in = new BufferedReader(ins);
            //跳过标题行
            String newLine = in.readLine();

            //历史数据，不含当天数据
            while ((newLine = in.readLine()) != null) {
                String[] strs = newLine.split(",");
                ps.setString(1, stockCode);
                ps.setString(2, cycle);
                ps.setString(3, strs[0].trim().replaceAll("-", ""));
                ps.setDouble(4, Double.valueOf(strs[1].trim()));
                ps.setDouble(5, Double.valueOf(strs[2].trim()));
                ps.setDouble(6, Double.valueOf(strs[3].trim()));
                ps.setDouble(7, Double.valueOf(strs[4].trim()));
                ps.setDouble(8, Double.valueOf(strs[5].trim()));
                ps.setDouble(9, 0.0d);
                ps.setDouble(10, Double.valueOf(strs[6].trim()));
                ps.setDouble(11, 0.0d);

                ps.addBatch();
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
            }

            ps.executeBatch(); // insert remaining records
            ps.close();
            sqlConn.close();
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

    /**
     * 导入股票历史交易信息至数据库，默认导出指定日期的日线交易信息
     *
     * @param stockCode 股票代码
     * @param shszFlag 沪市股市标识
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @return
     */
    public boolean importTrans(String stockCode, String shszFlag, String beginDate, String endDate) {
        return importTrans(stockCode, shszFlag, beginDate, endDate, StockHisTransInfo.CYCLE_DAY);
    }

    /**
     * 导入股票历史交易信息至数据库,默认导出所有日期的交易信息
     *
     * @param stockCode 股票代码
     * @param shszFlag 沪市股市标识
     * @param cycle 周期
     * @return
     */
    public boolean importTrans(String stockCode, String shszFlag, String cycle) {
        return importTrans(stockCode, shszFlag, "19900101", "20991231", cycle);
    }

    /**
     * 导入股票历史交易信息至数据库，默认导出所有日期的日线交易信息
     *
     * @param stockCode 股票代码
     * @param shszFlag 沪市股市标识
     * @return
     */
    public boolean importTrans(String stockCode, String shszFlag) {
        return importTrans(stockCode, shszFlag, "19900101", "20991231", StockHisTransInfo.CYCLE_DAY);
    }

    /**
     * 导入股票历史交易信息至数据库中
     *
     * @param stockCodes 股票代码列表
     * @param shszFlag 沪深股市标识
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @param cycle 周期
     */
    public void importTrans(List<String> stockCodes, String shszFlag, String beginDate, String endDate, String cycle) {
        boolean b;
        String cycleType = null;
        if (cycle.equals(StockHisTransInfo.CYCLE_DAY)) {
            cycleType = "日线";
        } else if (cycle.equals(StockHisTransInfo.CYCLE_WEEK)) {
            cycleType = "周线";
        } else if (cycle.equals(StockHisTransInfo.CYCLE_MONTH)) {
            cycleType = "月线";
        } else {
            cycleType = cycle;
        }

        for (String code : stockCodes) {
            System.out.print("开始导入股票" + code + "的历史交易信息["
                    + beginDate + "," + endDate + "," + cycleType + "]");
            b = importTrans(code, shszFlag, beginDate, endDate, cycle);
            if (b == true) {
                System.out.println("||成功！");
            } else {
                System.out.println("||失败！");
            }
            setImportState(code, beginDate, endDate, cycle, b);
        }
    }

    /**
     * 设置股票导入状况
     *
     * @param stockCode 股票代码
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @param cycle 周期
     * @param b 导入状况
     */
    public void setImportState(String stockCode, String beginDate, String endDate, String cycle, boolean b) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfTime = new SimpleDateFormat("kkmmss");

        String insertSql = "insert into stockhistransimportinfo (dte, time, stockid, shszflag, "
                + "abflag, begindate, enddate, cycle, state) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        MSSQLConnector connector = new MSSQLConnector();
        Connection sqlConn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(insertSql);

        try {
            ps.setString(1, sdfDate.format(new GregorianCalendar().getTime()));
            ps.setString(2, sdfTime.format(new GregorianCalendar().getTime()));
            ps.setString(3, stockCode);
            ps.setString(4, "");
            ps.setString(5, "");
            ps.setString(6, beginDate);
            ps.setString(7, endDate);
            ps.setString(8, cycle);
            if(b == true){
                ps.setString(9, "N");
            }else{
                ps.setString(9, "E");
            }
            ps.execute();

            ps.close();
            sqlConn.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();

        }
    }

    /**
     * 导出给定股票的所有历史交易信息至数据库中
     *
     * @param stockCodes 股票代码列表
     * @param shszFlag 沪深股市标识
     */
    public void importAllTrans(List<String> stockCodes, String shszFlag) {
        System.out.println("开始导入日线数据");
        importTrans(stockCodes, shszFlag, "19900101", "20991231", StockHisTransInfo.CYCLE_DAY);
        System.out.println("开始导入周线数据");
        importTrans(stockCodes, shszFlag, "19900101", "20991231", StockHisTransInfo.CYCLE_WEEK);
        System.out.println("开始导入年线数据");
        importTrans(stockCodes, shszFlag, "19900101", "20991231", StockHisTransInfo.CYCLE_MONTH);
    }

    /**
     * 从数据库中读取股票代码
     *
     * @param shszFlag 沪深股市标识
     * @return
     */
    public List<String> getStockCodes(String shszFlag) {
        List<String> codes = new ArrayList<String>();
        MSSQLConnector connector = new MSSQLConnector();
        Connection sqlConn = connector.getConnection();
        ResultSet rs = connector.executeQuery("select stockid from stockcorp where dte=(select max(dte) from stockcorp) and shszflag='" + shszFlag + "'");
        try {
            while (rs.next()) {
                codes.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return codes;
    }

    public static void main(String[] args) {
        StockHisTransImporter importer = new StockHisTransImporter();
        List<String> aCodes = importer.getStockCodes("1");
        System.out.println("沪市股票数为" + aCodes.size());
        importer.importAllTrans(aCodes, "1");

        List<String> bCodes = importer.getStockCodes("2");
        System.out.println("深市股票数为" + bCodes.size());
        importer.importAllTrans(bCodes, "2");
    }
}
