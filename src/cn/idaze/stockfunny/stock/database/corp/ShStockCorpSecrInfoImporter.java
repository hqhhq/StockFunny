/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.corp;

import cn.idaze.stockfunny.database.Connector;
import java.io.BufferedReader;
import java.io.FileInputStream;
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
 * 导入上交所公司董事会秘书信息
 *
 * @author hhq
 */
public class ShStockCorpSecrInfoImporter {

    public static void main(String[] args) {

        ShStockCorpSecrInfoImporter importer = new ShStockCorpSecrInfoImporter();
        Map<String, String> map = new HashMap<String, String>();
        map.put("DTE", "20180318");
        map.put("CORP", "900957");
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
        Logger logger = Logger.getLogger(ShStockCorpSecrInfoImporter.class);
        
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

        if (importSecretaryInfo(dateStr, corpId) == true) {
            logger.debug("导入公司" + corpId + "董秘信息成功！");
            return true;
        } else {
            logger.debug("导入公司" + corpId + "董秘信息失败！");
            return false;
        }

    }

    /**
     * 导入董事会秘书信息至数据库
     *
     * @param corpId 公司代码
     * @return
     */
    public boolean importSecretaryInfo(String dateStr, String corpId) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(ShStockCorpSecrInfoImporter.class);
        
        String baseUrlStr = "http://query.sse.com.cn/commonQuery.do?jsonCallBack=jsonpCallback90996&isPagination=false&sqlId=COMMON_SSE_ZQPZ_GP_GPLB_MSXX_C&productid=";
        String baseRefererStr = "http://www.sse.com.cn/assortment/stock/list/info/company/index.shtml?COMPANY_CODE=";
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String delSql = "delete from CORP_GENL_SECR_SH where dte=? and corpid=?";
        String insertSql = "insert into CORP_GENL_SECR_SH (dte, corpid, secrname, datasource, lastupdatetime, remarks) "
                + " values (?, ?, ?, ?, sysdate(6), ?)";
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
            ps.execute();

            ps = connector.prepareStmt(insertSql);

            final int batchSize = 1000;
            int count = 0;

            //数据日期
            ps.setString(1, dateStr.trim());
            //公司代码
            ps.setString(2, corpId.trim());
            //董秘名称
            ps.setString(3, jsonarray.getJSONObject(0).getString("SECURITY_OF_THE_BOARD_OF_DIRE").trim());
            //数据来源
            ps.setString(4, "SH");
            //备注
            ps.setString(5, "");

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
