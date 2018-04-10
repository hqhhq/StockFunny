/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database;

import cn.idaze.stockfunny.database.MSSQLConnector;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

/**
 * 导入上海证券交易上市公司信息，由于沪市信息展示方式发生变化，此处自20160930采用此新版本
 *
 * @author HHQ
 */
public class ShStockCorpImporterV2 {

    /**
     * 获取沪市上市公司股票代码
     *
     * @param abflag A股、B股标识
     * @param corpNum 上市公司数目
     * @return
     */
    public List<String> getStockCodes(String abflag, int corpNum) {
        List<String> stockCodes = new ArrayList<String>();

        String baseURL = null;
        if (abflag.equals(ShStockCorpInfo.ASTOCK)) {
            baseURL = "http://biz.sse.com.cn/sseportal/webapp/datapresent/"
                    + "SSEQueryStockInfoAct?keyword=&reportName=BizCompStockInfoRpt"
                    + "&PRODUCTID=&PRODUCTJP=&PRODUCTNAME=&CURSOR=";
        } else if (abflag.equals(ShStockCorpInfo.BSTOCK)) {
            baseURL = "http://biz.sse.com.cn/sseportal/webapp/datapresent/"
                    + "SSEQueryStockInfoAct?reportName=BizCompStockInfoRpt&PRODUCTID="
                    + "&PRODUCTJP=&PRODUCTNAME=&keyword=&tab_flg=2&CURSOR=";
        } else {
            return stockCodes;
        }

        //公司信息分页显示，开始页码
        int beginCursor = 1;
        //公司信息分页显示，每页显示的公司数目
        int step = 50;
        //公司信息分页显示，结束页码，需要根据实际公司数目不定期调整，其值由公司总数及每页显示的公司数目决定
        int endCursor;

        if (corpNum % step == 0) {
            endCursor = (corpNum / step - 1) * step + 1;
        } else {
            endCursor = (corpNum / step) * step + 1;
        }

        Pattern p1 = Pattern.compile("<td class=.table3. bgcolor=.(white|#dbedf8). width=.40%.><a href=.*>\\d{6}</a></td>");
        Matcher m1 = null;

        Pattern p2 = Pattern.compile(">(\\d{6})</a></td>");
        Matcher m2 = null;

        int count = 0;

        for (int i = beginCursor; i <= endCursor; i = i + step) {
            try {
                URL url = new URL(baseURL + i);
                URLConnection conn = url.openConnection();
                InputStream in = conn.getInputStream();
                String s = IOUtils.toString(in, "gb2312");
                //System.out.println(s);
                //System.out.println("hello*****************");
                m1 = p1.matcher(s);
                while (m1.find()) {
                    //System.out.println(m1.group());

                    m2 = p2.matcher(m1.group());
                    if (m2.find()) {
                        //System.out.println(m2.group(1));
                        stockCodes.add(m2.group(1));
                    }
                }

            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }


        return stockCodes;
    }

    /**
     * 获取上海证券交易所股票的详细信息
     *
     * @param shStockCode 沪市股票代码
     * @param abFlag A股、B股标识
     * @return
     */
    public ShStockCorpInfo getShStockCorpInfo(String shStockCode, String abFlag) {
        ShStockCorpInfo info = new ShStockCorpInfo();

        String urlStr = "http://biz.sse.com.cn/sseportal/webapp/datapresent/"
                + "SSEQueryListCmpAct?reportName=QueryListCmpRpt&REPORTTYPE=GSZC&PRODUCTID="
                + shStockCode + "&COMPANY_CODE=" + shStockCode;

        if (abFlag.equals(ShStockCorpInfo.ASTOCK) == false && abFlag.equals(ShStockCorpInfo.BSTOCK) == false) {
            return info;
        }

        info.setStockId(shStockCode);
        info.setAbFlag("" + abFlag);

        //多行匹配，不区分大小写
        Pattern p = Pattern.compile("<td class=\"content_b\" >公司代码:</td>.*A股状态/B股状态:</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = null;

        //公司代码
        Pattern pCorpId = Pattern.compile("公司代码.*([0-9]{6}).*股票代码\\(A股/B股\\)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        //上市日期
        Pattern pIpoDate = Pattern.compile("上市日.*<span(.*?)>.*<a href.*可转债简称", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        //公司简称
        Pattern pAbbrName = Pattern.compile("公司简称.*<td width=\"100%\" >(.*)/.*</td>.*公司全称", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        //公司全称
        Pattern pFullName = Pattern.compile("公司全称.*<td width=\"100%\" >(.*)<BR>.*</td>.*注册地址", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        //英文名称
        Pattern pEngName = Pattern.compile("公司全称.*<BR>(.*)</td>.*注册地址", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        //注册地址
        Pattern pRegAddr = Pattern.compile("注册地址.*<td width=\"100%\" >(.*)</td>.*通讯地址", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        //公司网址
        Pattern pWebsite = Pattern.compile("网址.*<a.*>(.*)</a>.*CSRC行业", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        //所属行业(CSRC行业)
        Pattern pIndustry = Pattern.compile("CSRC行业.*<td >(.*)</td>.*SSE行业", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        //所属省份
        Pattern pProvince = Pattern.compile("所属省/直辖市.*<td >(.*)</td>.*A股状态", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        Matcher m2 = null;

        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            String s = IOUtils.toString(in, "gb2312");
            //System.out.println(s);
            m = p.matcher(s);
            while (m.find()) {
                //System.out.println(m.group());

                m2 = pCorpId.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("公司代码:" + m2.group());
                    //System.out.println("公司代码:" + m2.group(1).trim());
                    info.setCorpId(m2.group(1).trim());
                }
                m2 = pIpoDate.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("上市日期:" + m2.group());
                    //System.out.println("上市日期:" + m2.group(1).trim());
                    info.setIpoDate(m2.group(1).trim().replaceAll("-", ""));
                }
                m2 = pAbbrName.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("公司简称:" + m2.group());
                    //System.out.println("公司简称:" + m2.group(1).trim());
                    info.setAbbrName(m2.group(1).trim());
                }
                m2 = pFullName.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("公司全称:" + m2.group());
                    //System.out.println("公司全称:" + m2.group(1).trim());
                    info.setFullName(m2.group(1).trim());
                }
                m2 = pEngName.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("英文名称:" + m2.group());
                    //System.out.println("英文名称:" + m2.group(1).trim());
                    info.setEngName(m2.group(1).trim());
                }
                m2 = pRegAddr.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("注册地址:" + m2.group());
                    //System.out.println("注册地址:" + m2.group(1).trim());
                    info.setRegAddr(m2.group(1).trim());
                }
                m2 = pWebsite.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("公司网址:" + m2.group());
                    //System.out.println("公司网址:" + m2.group(1).trim());
                    info.setWebsite(m2.group(1).trim());
                }
                m2 = pIndustry.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("所属行业:" + m2.group());
                    //System.out.println("所属行业:" + m2.group(1).trim());
                    info.setIndustry(m2.group(1).trim());
                }
                m2 = pProvince.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("所属省份:" + m2.group());
                    //System.out.println("所属省份:" + m2.group(1).trim());
                    info.setProvince(m2.group(1).trim());
                }
            }

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return info;
    }

    /**
     * 导入沪市股票信息至数据库
     *
     * @param date 数据日期
     * @param corps 待导入的公司信息
     * @return
     */
    public boolean importData(String date, List<ShStockCorpInfo> corps) {
        String delSql = "delete from stockcorp where dte=? and shszflag='1' and abflag=?";
        String insertSql = "insert into stockcorp (dte, corpid, stockid, abflag, shszflag, abbrname, "
                + "fullname, engname, regaddr, ipodate, capitalstock, "
                + "tradableshares, areacode, area, provincecode, province, "
                + "citycode, city, industrycode, industry, website, remarks) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?)";
        MSSQLConnector connector = new MSSQLConnector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(delSql);

        if (corps.isEmpty()){
            return true;
        }
        
        try {
            ps.setString(1, date);
            ps.setString(2, corps.get(0).getAbFlag());
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            for (ShStockCorpInfo corp : corps) {
                ps.setString(1, date);
                ps.setString(2, corp.getCorpId());
                ps.setString(3, corp.getStockId());
                ps.setString(4, corp.getAbFlag());
                ps.setString(5, "1");
                ps.setString(6, corp.getAbbrName());
                ps.setString(7, corp.getFullName());
                ps.setString(8, corp.getEngName());
                ps.setString(9, corp.getRegAddr());
                ps.setString(10, corp.getIpoDate());
                ps.setDouble(11, 0.0d);
                ps.setDouble(12, 0.0d);
                ps.setString(13, "");
                ps.setString(14, "");
                ps.setString(15, "");
                ps.setString(16, corp.getProvince());
                ps.setString(17, "");
                ps.setString(18, "");
                ps.setString(19, "");
                ps.setString(20, corp.getIndustry());
                ps.setString(21, corp.getWebsite());
                ps.setString(22, "");

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
     * 导入沪市股票信息至数据库，数据日期默认用当前日期
     *
     * @param corps
     * @return
     */
    public boolean importData(List<ShStockCorpInfo> corps) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        return importData(sdf.format(new GregorianCalendar().getTime()), corps);
    }

    /**
     * 获取沪市公司信息
     *
     * @param stockCodes 股票代码
     * @param abFlag A股、B股标识
     * @return
     */
    public List<ShStockCorpInfo> getShStockCorpInfos(List<String> stockCodes, String abFlag) {
        List<ShStockCorpInfo> corpInfos = new ArrayList<ShStockCorpInfo>();
        ShStockCorpInfo corpInfo = null;

        for (String corpCode : stockCodes) {
            System.out.print("开始获取股票" + corpCode + "的信息~");
            corpInfo = getShStockCorpInfo(corpCode, abFlag);
            if (corpInfo.getCorpId().equals("")) {
                System.out.println("||失败！");
            } else {
                System.out.println("||成功！");
            }
            if (corpInfo.getCorpId().equals("") == false && corpInfo.getStockId().equals("") == false) {
                corpInfos.add(corpInfo);
            }
        }

        return corpInfos;
    }
    
    /**
     * 获取当前A/B股公司数目
     * @param abFlag
     * @return 
     */
    public int getCorpNum(String abFlag){
        String urlStr;
        if(abFlag.equals(ShStockCorpInfo.ASTOCK)){
            urlStr = "http://biz.sse.com.cn/sseportal/webapp/datapresent/SSEQueryStockInfoAct?keyword=&reportName=BizCompStockInfoRpt&PRODUCTID=&PRODUCTJP=&PRODUCTNAME=&CURSOR=1&tab_flg=1";
        }else if(abFlag.equals(ShStockCorpInfo.BSTOCK)){
            urlStr ="http://biz.sse.com.cn/sseportal/webapp/datapresent/SSEQueryStockInfoAct?keyword=&reportName=BizCompStockInfoRpt&PRODUCTID=&PRODUCTJP=&PRODUCTNAME=&CURSOR=1&tab_flg=2";
        }else {
            return 0;
        }
        
         //多行匹配，不区分大小写，非贪婪模式
        Pattern p = Pattern.compile("<td align=\"right\" class=\"content\" nowrap>.*第.*条到第.*条，共.*条(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = null;
        
        Pattern pNum = Pattern.compile("共(.*)条", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m2 = null;

        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            String s = IOUtils.toString(in, "gb2312");
            //System.out.println(s);
            m = p.matcher(s);
            while (m.find()) {
                //System.out.println(m.group());

                m2 = pNum.matcher(m.group());
                if (m2.find()) {
                    //System.out.println("公司数目:" + m2.group(1).trim());
                    return Integer.parseInt(m2.group(1).trim());
                }
                
            }

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return 0;
    }

    public static void main(String[] args) {
        ShStockCorpImporterV2 importer = new ShStockCorpImporterV2();
        Boolean b;
        
        //System.out.println(importer.getCorpNum(ShStockCorpInfo.ASTOCK));
        
        //List<String> aCodes = importer.getStockCodes(ShStockCorpInfo.ASTOCK, 1092);
        List<String> aCodes = importer.getStockCodes(ShStockCorpInfo.ASTOCK, importer.getCorpNum(ShStockCorpInfo.ASTOCK));
        System.out.println("共获取沪市A股共" + aCodes.size() + "只~");
        List<ShStockCorpInfo> aCorpInfos = importer.getShStockCorpInfos(aCodes, ShStockCorpInfo.ASTOCK);
        System.out.print("开始导入A股上市公司信息~");
        b = importer.importData(aCorpInfos);
        if (b == true) {
            System.out.println("||成功！");
        } else {
            System.out.println("||失败！");
        }
        
        //List<String> bCodes = importer.getStockCodes(ShStockCorpInfo.BSTOCK, 53);
        List<String> bCodes = importer.getStockCodes(ShStockCorpInfo.BSTOCK, importer.getCorpNum(ShStockCorpInfo.BSTOCK));
        System.out.println("共获取沪市B股共" + bCodes.size() + "只~");
        List<ShStockCorpInfo> bCorpInfos = importer.getShStockCorpInfos(bCodes, ShStockCorpInfo.BSTOCK);
        System.out.print("开始导入B股上市公司信息~");
        b = importer.importData(bCorpInfos);
        if (b == true) {
            System.out.println("||成功！");
        } else {
            System.out.println("||失败！");
        }
    }
}