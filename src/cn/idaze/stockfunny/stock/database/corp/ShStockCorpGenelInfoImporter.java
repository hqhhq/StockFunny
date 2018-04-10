/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.corp;

import cn.idaze.stockfunny.database.Connector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 导入上交所公司概要信息
 *
 * @author hhq
 */
public class ShStockCorpGenelInfoImporter {

    public static void main(String[] args) {

        ShStockCorpGenelInfoImporter importer = new ShStockCorpGenelInfoImporter();
        Map<String, String> map = new HashMap<String, String>();
        map.put("DTE", "20180315");
        map.put("CORP", "600000");
        importer.execJob(map);
    }

    /**
     * 执行作业
     *
     * @param params 作业执行参数
     * @return
     */
    public boolean execJob(Map<String, String> params) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(ShStockCorpGenelInfoImporter.class);

        String dateStr;
        String corpId;
        if (params.get("DTE") == null || params.get("CORP") == null) {
            logger.error("参数非法，需要传入日期和公司代码!");
            return false;
        } else {
            dateStr = params.get("DTE").trim();
            corpId = params.get("CORP").trim();
            logger.debug("执行参数为：DTE=" + dateStr + ", CORP=" + corpId);
        }

        if (importGenelInfo(dateStr, corpId) == true) {
            //System.out.println("导入公司" + corpId + "概要信息成功！");
            logger.debug("导入公司" + corpId + "概要信息成功！");
            return true;
        } else {
            //System.out.println("导入公司" + corpId + "概要信息失败！");
            logger.debug("导入公司" + corpId + "概要信息失败！");
            return false;
        }

    }

    /**
     * 导入公司概要信息至数据库
     *
     * @param dateStr
     * @param corpId 公司代码
     * @return
     */
    public boolean importGenelInfo(String dateStr, String corpId) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(ShStockCorpGenelInfoImporter.class);

        String baseUrlStr = "http://query.sse.com.cn/commonQuery.do?jsonCallBack=jsonpCallback62821&isPagination=false&sqlId=COMMON_SSE_ZQPZ_GP_GPLB_C&productid=";
        String baseRefererStr = "http://www.sse.com.cn/assortment/stock/list/info/company/index.shtml?COMPANY_CODE=";
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String delSql = "delete from CORP_GENL_INFO_SH where dte=? and corpid=? and datasource=?";
        String insertSql = "insert into CORP_GENL_INFO_SH (dte, corpid, stockid_a, stockid_b, "
                + "chgbondabbr, chgbondid, corpabbr_cn, corpabbr_en, "
                + "corpfull_cn, corpfull_en, regaddr, postaddr, zipcode, legalrepr, email, phone, "
                + "website, csrcdes, csrcgreat, csrcmid, ssedesc, province, state_a, "
                + "state_b, sh180sample, foreignlisting, foreignlistingaddr, datasource, "
                + "lastupdatetime, remarks) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?,  sysdate(6), ?)";
        Connector connector = new Connector();
        Connection conn = connector.getConnection();

        try {

            //针对给定的股票代码，爬取网页详细信息
            HttpGet httpGet = new HttpGet(baseUrlStr + corpId.trim());
            Header header;
            header = new BasicHeader("Referer", baseRefererStr + corpId.trim());
            httpGet.setHeader(header);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String brLine;
            StringBuilder sb = new StringBuilder();
            while ((brLine = br.readLine()) != null) {
                sb.append(brLine);
            }
            String resultStr = sb.toString();
            //System.out.println(resultStr);

            //去除标准JSON前后字符
            int leftBraceIndex = resultStr.indexOf("{");
            String resultJSonStr = resultStr.substring(leftBraceIndex, resultStr.length() - 1);
            //System.out.println(resultJSonStr);
            JSONObject jsonObj = new JSONObject(resultJSonStr);
            JSONArray jsonarray = jsonObj.getJSONArray("result");
            //System.out.println(jsonarray.getJSONObject(0).getString("SECURITY_OF_THE_BOARD_OF_DIRE").trim());

            PreparedStatement ps = connector.prepareStmt(delSql);
            ps.setString(1, dateStr.trim());
            ps.setString(2, corpId.trim());
            ps.setString(3, "SH");
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            //数据日期
            ps.setString(1, dateStr.trim());
            //公司代码
            ps.setString(2, corpId.trim());
            //A股代码
            ps.setString(3, jsonarray.getJSONObject(0).getString("SECURITY_CODE_A").trim());
            //B股代码
            ps.setString(4, jsonarray.getJSONObject(0).getString("SECURITY_CODE_B").trim());
            //可转债简称
            ps.setString(5, jsonarray.getJSONObject(0).getString("CHANGEABLE_BOND_ABBR").trim());
            //可转债代码
            ps.setString(6, jsonarray.getJSONObject(0).getString("CHANGEABLE_BOND_CODE").trim());
            //公司中文简称
            ps.setString(7, jsonarray.getJSONObject(0).getString("COMPANY_ABBR").trim());
            //公司英文简称
            ps.setString(8, jsonarray.getJSONObject(0).getString("ENGLISH_ABBR").trim());
            //公司中文全称
            ps.setString(9, jsonarray.getJSONObject(0).getString("FULLNAME").trim());
            //公司英文全称
            ps.setString(10, jsonarray.getJSONObject(0).getString("FULL_NAME_IN_ENGLISH").trim());
            //注册地址
            ps.setString(11, jsonarray.getJSONObject(0).getString("COMPANY_ADDRESS").trim());
            //通讯地址
            ps.setString(12, jsonarray.getJSONObject(0).getString("OFFICE_ADDRESS").trim());
            //邮编
            ps.setString(13, jsonarray.getJSONObject(0).getString("OFFICE_ZIP").trim());
            //法定代表人
            ps.setString(14, jsonarray.getJSONObject(0).getString("LEGAL_REPRESENTATIVE").trim());
            //Email
            ps.setString(15, jsonarray.getJSONObject(0).getString("E_MAIL_ADDRESS").trim());
            //联系电话
            ps.setString(16, jsonarray.getJSONObject(0).getString("REPR_PHONE").trim());
            //网址
            ps.setString(17, jsonarray.getJSONObject(0).getString("WWW_ADDRESS").trim());
            //CSRC行业门类
            ps.setString(18, jsonarray.getJSONObject(0).getString("CSRC_CODE_DESC").trim());
            //CSRC行业大类
            ps.setString(19, jsonarray.getJSONObject(0).getString("CSRC_GREAT_CODE_DESC").trim());
            //CSRC行业中类
            ps.setString(20, jsonarray.getJSONObject(0).getString("CSRC_MIDDLE_CODE_DESC").trim());
            //SSE行业
            ps.setString(21, jsonarray.getJSONObject(0).getString("SSE_CODE_DESC").trim());
            //所属省/直辖市
            ps.setString(22, jsonarray.getJSONObject(0).getString("AREA_NAME_DESC").trim());
            //A股状态
            ps.setString(23, jsonarray.getJSONObject(0).getString("STATE_CODE_A_DESC").trim());
            //B股状态
            ps.setString(24, jsonarray.getJSONObject(0).getString("STATE_CODE_B_DESC").trim());
            //是否上证180样本股
            ps.setString(25, jsonarray.getJSONObject(0).getString("SECURITY_30_DESC").trim());
            //是否境外上市
            ps.setString(26, jsonarray.getJSONObject(0).getString("FOREIGN_LISTING_DESC").trim());
            //境外上市地
            ps.setString(27, jsonarray.getJSONObject(0).getString("FOREIGN_LISTING_ADDRESS").trim());
            //数据来源
            ps.setString(28, "SH");
            //备注
            ps.setString(29, "");

            ps.addBatch();
            if (++count % batchSize == 0) {
                ps.executeBatch();
            }

            ps.executeBatch(); // insert remaining records
            ps.close();
            conn.close();

        } catch (SQLException sqlex) {
            //System.out.println("SQL执行失败！");
            logger.error("SQL执行失败！" + sqlex.getMessage());
            return false;
        } catch (IOException ioex) {
            //System.out.println("数据读写失败！");
            logger.error("数据读写失败！" + ioex.getMessage());
            return false;
        }
        return true;
    }
}
