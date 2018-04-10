/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author HHQ
 */
public class Test_20140721_01 {

    public static void main(String[] args) throws Exception {
        Pattern pListStocks = Pattern.compile("证券列表.*([0-9]{6}).*详细信息", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = null;
        Matcher m2 = null;
        Matcher m3 = null;

        List<StockInfo> stockLists = new ArrayList<StockInfo>();

        Pattern pStockInfo = Pattern.compile("([0-9]{6}).*", Pattern.CASE_INSENSITIVE);

        Pattern pMainInfo = Pattern.compile("-*\\s.*-*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);


        try {
            URL url = new URL("http://www.szse.cn/szseWeb/common/szse/files/text/jy/jy140721.txt");
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            String s = IOUtils.toString(in, "gb2312");
            //System.out.println(s);
            m = pListStocks.matcher(s);
            while (m.find()) {
                //System.out.println(m.group());
                m2 = pStockInfo.matcher(m.group());
                while (m2.find()) {
                    System.out.println("x:" + m2.group());
                    String[] array = m2.group().split("\\s+");
                    stockLists.add(new StockInfo(array[0].trim(), array[1].trim(), array[2].trim()));
                }
            }

            m3 = pMainInfo.matcher(s);
            while (m3.find()) {
                System.out.println("********M3:" + m3.group() + "**********");
                String[] mainInfos = m3.group().split("--+");
                System.out.println("********M3-N:"+mainInfos.length);
                for(String mainInfo : mainInfos){
                    System.out.println("********M3-N-X:"+mainInfo);
                }
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        new Test_20140721_01().printStockInfo(stockLists);


    }

    private void printStockInfo(List<StockInfo> lists) {
        for (StockInfo si : lists) {
            System.out.println("X:" + si.getStockCode() + "-" + si.getStockName() + "-" + si.getReason());
        }
    }
}

class StockInfo {

    private String stockCode = "";
    private String stockName = "";
    private String reason = "";

    public StockInfo(String stockCode, String stockName, String reason) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.reason = reason;
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
     * @return the reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * @param reason the reason to set
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
