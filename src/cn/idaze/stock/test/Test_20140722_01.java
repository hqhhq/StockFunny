/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import cn.idaze.stockfunny.database.MSSQLConnector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author HHQ
 */
public class Test_20140722_01 {

    public static void main(String[] args) {
        Test_20140722_01 test = new Test_20140722_01();

        //披露日期范围的起始日期
        String beginDate = "20140802";
        //披露日期范围的起始日期
        String endDate = "20140807";
        
        String debug = "1";

        String tmpDate = beginDate;
        while (tmpDate.compareTo(endDate) <= 0) {
            if(debug.equals("1")){
                System.out.println("开始获取"+tmpDate+"信息......");
            }
            if(debug.equals("1")){
                System.out.println("开始获取主板信息......");
            }
            //深市主板A股
            test.getNoticeStockInfoIntoDB("http://www.szse.cn/szseWeb/common/szse/files/text/jy/jy" + tmpDate.substring(2, 8) + ".txt",
                    StockBuySaleInfo.MAIN_BOARD_A, "gb2312", tmpDate);
            if(debug.equals("1")){
                System.out.println("开始获取中小板信息......");
            }
            //深市中小板
            test.getNoticeStockInfoIntoDB("http://www.szse.cn/szseWeb/common/szse/files/text/smeTxt/gk/sme_jy" + tmpDate.substring(2, 8) + ".txt",
                    StockBuySaleInfo.SMALL_MID_BOARD_A, "gb2312", tmpDate);
            if(debug.equals("1")){
                System.out.println("开始获取创业板信息......");
            }
            //创业板
            test.getNoticeStockInfoIntoDB("http://www.szse.cn/szseWeb/common/szse/files/text/nmTxt/gk/nm_jy" + tmpDate.substring(2, 8) + ".txt",
                    StockBuySaleInfo.GEM_BOARD_A, "gb2312", tmpDate);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Integer.valueOf(tmpDate.substring(0, 4)), Integer.valueOf(tmpDate.substring(4, 6)) - 1, Integer.valueOf(tmpDate.substring(6, 8)));
            //日期后延1天
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            tmpDate = sdf.format(calendar.getTime());
        }

    }

    /**
     * 获取证券交易公开信息并插入数据库中
     *
     * @param url
     * @param stockType
     * @param encoding
     * @param date
     * @return
     */
    public boolean getNoticeStockInfoIntoDB(String url, String stockType, String encoding, String date) {

        String s = this.getNetText(url, encoding);

        //获取本次公示的证券信息列表
        List<NoticeStockInfo> noticeStockInfoList = this.getNoticeStockInfo(stockType, s);
        if (noticeStockInfoList.size() > 0) {
            System.out.println("****** " + date + "日披露的证券列表（共" + noticeStockInfoList.size() + "只） ******");
            for (NoticeStockInfo nsi : noticeStockInfoList) {
                System.out.println(nsi);
            }
            System.out.println("****** " + date + "日披露的证券列表(end) ******");
            
            //将数据写入数据库中
            //return this.insertNotickStockInfoIntoDatabase(date, noticeStockInfoList);
        }

        //获取公示的证券买卖前五信息
        List<StockBuySaleInfo> stockBuySaleInfoList = this.getStockBuySaleInfo(stockType, s);
        if (stockBuySaleInfoList.size() > 0) {
            System.out.println("****** " + date + "日买卖前五信息 ******");
            for (StockBuySaleInfo sbsi : stockBuySaleInfoList) {
                System.out.println(sbsi);
            }
            System.out.println("****** " + date + "日买卖前五信息(end) ******");

            //将数据写入数据库中
            return this.insertIntoDatabase(date, stockBuySaleInfoList);
        }

        return true;
    }

    /**
     * 读取给定URL对应的文本
     *
     * @param url 网络地址
     * @param encoding 字符编码
     * @return
     */
    public String getNetText(String url, String encoding) {
        try {
            URLConnection conn = new URL(url).openConnection();
            InputStream in = conn.getInputStream();
            String s = IOUtils.toString(in, encoding);
            return s;
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * 获取批露证券信息列表，包括证券代码、证券简称、披露原因
     *
     * @param stockType
     * @param s
     * @return
     */
    private List<NoticeStockInfo> getNoticeStockInfo(String stockType, String s) {
        List<NoticeStockInfo> noticeStockInfoList = new ArrayList<NoticeStockInfo>();

        //匹配公告证券列表，多行匹配，不区分大小写
        Pattern pNoticeStockInfos = Pattern.compile("证券列表.*([0-9]{6}).*详细信息", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        //匹配每一条公告的证券信息，单行匹配，不区分大小写
        Pattern pNoticeStockInfo = Pattern.compile("([0-9]{6}).*", Pattern.CASE_INSENSITIVE);
        Matcher m1, m2;

        m1 = pNoticeStockInfos.matcher(s);
        while (m1.find()) {
            //System.out.println(m1.group());
            m2 = pNoticeStockInfo.matcher(m1.group());
            while (m2.find()) {
                String[] elements = m2.group().split("\\s+");
                noticeStockInfoList.add(new NoticeStockInfo(stockType, elements[0].trim(), elements[1].trim(), elements[2].trim()));
            }
        }

        return noticeStockInfoList;
    }

    /**
     * 获取买卖前五交易信息
     *
     * @param stockType
     * @param s
     * @return
     */
    private List<StockBuySaleInfo> getStockBuySaleInfo(String stockType, String s) {
        List<StockBuySaleInfo> stockBuySaleInfoList = new ArrayList<StockBuySaleInfo>();

        String[] ss = s.split("--------------------------------------------------------------------------------");
        for (String si : ss) {
            si = si.trim();
            if (si.indexOf("买入金额最大的前5名") < 0) {  //不包含待披露信息
                continue;
            }

            BufferedReader br = new BufferedReader(new StringReader(si));
            Pattern tmpPattern = Pattern.compile("(代码[0-9]{6}).*", Pattern.CASE_INSENSITIVE);
            Matcher m;
            String line;

            try {
                line = br.readLine(); //获取披露原因，字符"："前为披露原因，但有时披露原因分为多行，需要特殊处理，如：
                //连续三个交易日内，
                //日均换手率与前五个交易日的日均换手率的比值达到30倍，且换手率累计达20%的证券：
                //System.out.println("First line:" + line);
                StringBuilder sb = new StringBuilder();
                while (line.indexOf("：") < 0) {
                    sb.append(line);
                    line = br.readLine();
                }
                sb.append(line.substring(0, line.indexOf("：")));
                String noticeReason = sb.toString();
                //System.out.println(noticeReason);

                line = br.readLine();
                while (line != null) {
                    m = tmpPattern.matcher(line);
                    if (m.find()) {
                        String stockName = line.substring(0, line.indexOf("("));//披露的证券名称
                        String stockCode = line.substring(line.indexOf("(") + 3, line.indexOf(")"));//披露的证券代码
                        //System.out.println(stockName + "(" + stockCode + ")的买卖前五(" + noticeReason + ")：");

                        line = br.readLine();//跳过中间无效信息，定位到买入前五
                        while (line != null && line.indexOf("买入金额(元)") < 0) {
                            line = br.readLine();
                        }

                        line = br.readLine(); //定位到买入前五
                        while (line != null && line.indexOf("卖出金额最大的前5名") < 0) {//卖出前五之前的非空文本为买入前五
                            if (line.trim().length() > 0) {
                                String[] buySaleDetail = line.split("\\s+");
                                stockBuySaleInfoList.add(new StockBuySaleInfo(stockType, stockCode, stockName, noticeReason, "BUY",
                                        buySaleDetail[0], Double.valueOf(buySaleDetail[1]), Double.valueOf(buySaleDetail[2])));
                            }
                            line = br.readLine();
                        }

                        line = br.readLine();//跳过中间无效信息，定位到卖出前五
                        while (line != null && line.indexOf("卖出金额(元)") < 0) {
                            line = br.readLine();
                        }

                        line = br.readLine();//定位到卖出前五
                        while (line != null && line.indexOf("(代码") < 0) {//下一证券买卖信息前为卖出前五
                            if (line.trim().length() > 0) {
                                String[] buySaleDetail = line.split("\\s+");
                                stockBuySaleInfoList.add(new StockBuySaleInfo(stockType, stockCode, stockName, noticeReason, "SALE",
                                        buySaleDetail[0], Double.valueOf(buySaleDetail[1]), Double.valueOf(buySaleDetail[2])));
                            }
                            line = br.readLine();
                        }
                    } else {//未匹配到证券代码转到下一行，如果已匹配到，则退出时又匹配到了下一证券代码或者读到结尾
                        line = br.readLine();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

        return stockBuySaleInfoList;
    }

    /**
     * 将证券买卖信息插入数据库中
     *
     * @param date
     * @param stockBuySaleInfoList
     * @return
     */
    private boolean insertIntoDatabase(String date, List<StockBuySaleInfo> stockBuySaleInfoList) {
        if (stockBuySaleInfoList.isEmpty() == true) {
            System.out.println("无入库数据!");
            return true;
        }

        String delSql = "delete from notice_stock_buy_sale_info where dte=? and stocktype=?";
        String insertSql = "insert into notice_stock_buy_sale_info (dte, stocktype, stockcode, stockname, noticereason, "
                + "buysaleflag, corpname, buyamt, saleamt) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);

        try {
            ps.setString(1, date);
            ps.setString(2, stockBuySaleInfoList.get(0).getStockType());
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            for (StockBuySaleInfo sbsi : stockBuySaleInfoList) {
                ps.setString(1, date);
                ps.setString(2, sbsi.getStockType());
                ps.setString(3, sbsi.getStockCode());
                ps.setString(4, sbsi.getStockName());
                ps.setString(5, sbsi.getNoticeReason());
                ps.setString(6, sbsi.getBuySaleFlag());
                ps.setString(7, sbsi.getCorpName());
                ps.setDouble(8, sbsi.getBuyAmt());
                ps.setDouble(9, sbsi.getSaleAmt());

                ps.addBatch();
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch(); // insert remaining records
            ps.close();
            conn.close();

        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 废弃，待完善：将证券披露原因插入库中（2008与现在的格式有所区别，之前的无“证券列表***详细信息”字符）
     * @param date
     * @param noticeStockInfoList
     * @return 
     */
    private boolean insertNotickStockInfoIntoDatabase(String date, List<NoticeStockInfo> noticeStockInfoList) {
        if (noticeStockInfoList.isEmpty() == true) {
            System.out.println("无入库数据!");
            return true;
        }

        String delSql = "delete from notice_stock_info where dte=? and stocktype=?";
        String insertSql = "insert into notice_stock_info (dte, stocktype, stockcode, stockname, noticereason "
                + " values (?, ?, ?, ?, ?)";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);

        try {
            ps.setString(1, date);
            ps.setString(2, noticeStockInfoList.get(0).getStockType());
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            for (NoticeStockInfo nsi : noticeStockInfoList) {
                ps.setString(1, date);
                ps.setString(2, nsi.getStockType());
                ps.setString(3, nsi.getStockCode());
                ps.setString(4, nsi.getStockName());
                ps.setString(5, nsi.getNoticeReason());
                
                ps.addBatch();
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch(); // insert remaining records
            ps.close();
            conn.close();

        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            return false;
        }

        return true;
    }
}

/**
 * 证券买卖信息
 *
 * @author HHQ
 */
class StockBuySaleInfo {

    public static String MAIN_BOARD_A = "深市主板A股";
    public static String SMALL_MID_BOARD_A = "深市中小板";
    public static String GEM_BOARD_A = "深市创业板";
    //证券类别
    private String stockType;
    //证券代码
    private String stockCode;
    //证券简称
    private String stockName;
    //披露原因
    private String noticeReason;
    //买卖标志
    private String buySaleFlag;
    //营业部或交易单元名称
    private String corpName;
    //买入金额
    private double buyAmt;
    //卖出金额
    private double saleAmt;

    /**
     *
     * @param stockCode
     * @param stockName
     * @param noticeReason
     * @param buySaleFlag
     * @param corpName
     * @param buyAmt
     * @param saleAmt
     */
    public StockBuySaleInfo(String stockType, String stockCode, String stockName,
            String noticeReason, String buySaleFlag, String corpName, double buyAmt, double saleAmt) {
        this.stockType = stockType;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.noticeReason = noticeReason;
        this.buySaleFlag = buySaleFlag;
        this.corpName = corpName;
        this.buyAmt = buyAmt;
        this.saleAmt = saleAmt;
    }

    /**
     * @return the stockType
     */
    public String getStockType() {
        return stockType;
    }

    /**
     * @param stockType the stockType to set
     */
    public void setStockType(String stockType) {
        this.stockType = stockType;
    }

    /**
     * @return the stockCode
     */
    public String getStockCode() {
        return stockCode;
    }

    /**
     * @param stockCode the stockCode to set
     */
    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    /**
     * @return the stockName
     */
    public String getStockName() {
        return stockName;
    }

    /**
     * @param stockName the stockName to set
     */
    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    /**
     * @return the noticeReason
     */
    public String getNoticeReason() {
        return noticeReason;
    }

    /**
     * @param noticeReason the noticeReason to set
     */
    public void setNoticeReason(String noticeReason) {
        this.noticeReason = noticeReason;
    }

    /**
     * @return the buySaleFlag
     */
    public String getBuySaleFlag() {
        return buySaleFlag;
    }

    /**
     * @param buySaleFlag the buySaleFlag to set
     */
    public void setBuySaleFlag(String buySaleFlag) {
        this.buySaleFlag = buySaleFlag;
    }

    /**
     * @return the corpName
     */
    public String getCorpName() {
        return corpName;
    }

    /**
     * @param corpName the corpName to set
     */
    public void setCorpName(String corpName) {
        this.corpName = corpName;
    }

    /**
     * @return the buyAmt
     */
    public double getBuyAmt() {
        return buyAmt;
    }

    /**
     * @param buyAmt the buyAmt to set
     */
    public void setBuyAmt(float buyAmt) {
        this.buyAmt = buyAmt;
    }

    /**
     * @return the saleAmt
     */
    public double getSaleAmt() {
        return saleAmt;
    }

    /**
     * @param saleAmt the saleAmt to set
     */
    public void setSaleAmt(float saleAmt) {
        this.saleAmt = saleAmt;
    }

    @Override
    public String toString() {
        return "证券代码：" + stockCode + "，证券简称：" + stockName + "，披露原因：" + noticeReason
                + "，买卖标识：" + buySaleFlag + "，营业部名称：" + corpName + "，买入金额：" + buyAmt + "，卖出金额：" + saleAmt;
    }
}

/**
 * 公告证券信息
 *
 * @author HHQ
 */
class NoticeStockInfo {

    //证券类别
    private String stockType;
    //证券代码
    private String stockCode;
    //证券简称
    private String stockName;
    //披露原因
    private String noticeReason;

    public NoticeStockInfo(String stockType, String stockCode, String stockName, String noticeReason) {
        this.stockType = stockType;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.noticeReason = noticeReason;
    }

    /**
     * @return the stockType
     */
    public String getStockType() {
        return stockType;
    }

    /**
     * @param stockType the stockType to set
     */
    public void setStockType(String stockType) {
        this.stockType = stockType;
    }

    /**
     * @return the stockCode
     */
    public String getStockCode() {
        return stockCode;
    }

    /**
     * @param stockCode the stockCode to set
     */
    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    /**
     * @return the stockName
     */
    public String getStockName() {
        return stockName;
    }

    /**
     * @param stockName the stockName to set
     */
    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    /**
     * @return the noticeReason
     */
    public String getNoticeReason() {
        return noticeReason;
    }

    /**
     * @param noticeReason the noticeReason to set
     */
    public void setNoticeReason(String noticeReason) {
        this.noticeReason = noticeReason;
    }

    @Override
    public String toString() {
        return "(" + stockType + ")证券代码：" + stockCode + "，证券简称：" + stockName + "，披露原因：" + noticeReason;
    }
}