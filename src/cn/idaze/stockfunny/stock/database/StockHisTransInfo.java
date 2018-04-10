/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database;

import java.io.Serializable;

/**
 * 股票历史交易信息
 * 
 * @author hhq
 */
public class StockHisTransInfo implements Serializable {
   //周期：d-日, w-周，m-月
    public static final String CYCLE_DAY = "d";
    public static final String CYCLE_WEEK = "w";
    public static final String CYCLE_MONTH = "m";
    
    //日期
    private String date;
    //股票代码
    private String stockId;
    //周期
    private String cycle;
    //开盘价
    private double openPrice = 0.0d;
    //最高价
    private double highPrice = 0.0d;
    //最低价
    private double lowPrice = 0.0d;
    //收市价
    private double closePrice = 0.0d;
    //成交量
    private double volume = 0.0d;
    //成交金额
    private double totalMoney = 0.0d;
    //向前复权价
    private double adjPreClosePrice = 0.0d;
    //向后复权价
    private double adjAftClosePrice = 0.0d;

    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * @return the cycle
     */
    public String getCycle() {
        return cycle;
    }

    /**
     * @param cycle the cycle to set
     */
    public void setCycle(String cycle) {
        this.cycle = cycle;
    }

    /**
     * @return the openPrice
     */
    public double getOpenPrice() {
        return openPrice;
    }

    /**
     * @param openPrice the openPrice to set
     */
    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }

    /**
     * @return the highPrice
     */
    public double getHighPrice() {
        return highPrice;
    }

    /**
     * @param highPrice the highPrice to set
     */
    public void setHighPrice(double highPrice) {
        this.highPrice = highPrice;
    }

    /**
     * @return the lowPrice
     */
    public double getLowPrice() {
        return lowPrice;
    }

    /**
     * @param lowPrice the lowPrice to set
     */
    public void setLowPrice(double lowPrice) {
        this.lowPrice = lowPrice;
    }

    /**
     * @return the closePrice
     */
    public double getClosePrice() {
        return closePrice;
    }

    /**
     * @param closePrice the closePrice to set
     */
    public void setClosePrice(double closePrice) {
        this.closePrice = closePrice;
    }

    /**
     * @return the volume
     */
    public double getVolume() {
        return volume;
    }

    /**
     * @param volume the volume to set
     */
    public void setVolume(double volume) {
        this.volume = volume;
    }

    /**
     * @return the totalMoney
     */
    public double getTotalMoney() {
        return totalMoney;
    }

    /**
     * @param totalMoney the totalMoney to set
     */
    public void setTotalMoney(double totalMoney) {
        this.totalMoney = totalMoney;
    }

    /**
     * @return the adjPreClosePrice
     */
    public double getAdjPreClosePrice() {
        return adjPreClosePrice;
    }

    /**
     * @param adjPreClosePrice the adjPreClosePrice to set
     */
    public void setAdjPreClosePrice(double adjPreClosePrice) {
        this.adjPreClosePrice = adjPreClosePrice;
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
