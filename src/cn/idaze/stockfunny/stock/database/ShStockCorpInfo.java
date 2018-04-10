/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database;

/**
 *
 * @author HHQ
 */
public class ShStockCorpInfo {
    //A股
    public static final String ASTOCK = "1";
    //B股
    public static final String BSTOCK = "2";
    //A、B股标识
    private String abFlag = "";
    //公司代码
    private String corpId = "";
    //股票代码
    private String stockId = "";
    //上市日期
    private String ipoDate = "";
    //公司简称
    private String abbrName = "";
    //公司全称
    private String fullName = "";
    //英文名称
    private String engName = "";
    //注册地址
    private String regAddr = "";
    //公司网址
    private String website = "";
    //所属行业(CSRC行业)
    private String industry = "";
    //所属省份
    private String province = "";
    //所属市
    private String city = "";

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
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("公司代码：");
        sb.append(this.getCorpId());
        sb.append("；股票代码：");
        sb.append(this.getStockId());
        sb.append("；A股B股标识：");
        sb.append(this.getAbFlag());
        sb.append("；上市日期：");
        sb.append(this.getIpoDate());
        sb.append("；公司简称：");
        sb.append(this.getAbbrName());
        sb.append("；公司全称：");
        sb.append(this.getFullName());
        sb.append("；英文名称：");
        sb.append(this.getEngName());
        sb.append("；注册地址：");
        sb.append(this.getRegAddr());
        sb.append("；公司网址：");
        sb.append(this.getWebsite());
        sb.append("；所属行业：");
        sb.append(this.getIndustry());
        sb.append("；所属省份：");
        sb.append(this.getProvince());
        sb.append("；所属市：");
        sb.append(this.getCity());
        
        return sb.toString();
    }
}