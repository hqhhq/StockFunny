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
 * 获取上交所公司高管信息
 *
 * @author hhq
 */
public class ShStockCorpSeniorInfoImporter {

    public static void main(String[] args) {

        ShStockCorpSeniorInfoImporter importer = new ShStockCorpSeniorInfoImporter();
        Map<String, String> map = new HashMap<String, String>();
        map.put("DTE", "20180409");
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
        Logger logger = Logger.getLogger(ShStockCorpSeniorInfoImporter.class);

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

        if (importSeniorInfo(dateStr, corpId) == true) {
            logger.debug("导入公司" + corpId + "高管信息成功！");
            return true;
        } else {
            logger.debug("导入公司" + corpId + "高管信息失败！");
            return false;
        }

    }

    /**
     * 导入公司高管信息至数据库
     *
     * @param corpId 公司代码
     * @return
     */
    public boolean importSeniorInfo(String dateStr, String corpId) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(ShStockCorpSeniorInfoImporter.class);

        String baseUrlStr = "http://query.sse.com.cn/commonQuery.do?jsonCallBack=jsonpCallback92935&isPagination=true&sqlId=COMMON_SSE_ZQPZ_GG_GGRYLB_L&productid=";
        String baseRefererStr = "http://www.sse.com.cn/assortment/stock/list/info/executives/index.shtml?COMPANY_CODE=";
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String delSql = "delete from CORP_SENIOR_INFO_SH where dte=? and corpid=? and datasource=?";
        String insertSql = "insert into CORP_SENIOR_INFO_SH (dte, corpid, position, name, "
                + "starttime, seq, datasource, lastupdatetime, remarks) "
                + "values (?, ?, ?, ?, ?, ?, ?, sysdate(6), ?)";
        Connector connector = new Connector();
        Connection conn = connector.getConnection();

        try {

            //针对给定的公司代码，爬取网页详细信息
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

            for (int i = 0; i < jsonarray.length(); i++) {
                //数据日期
                ps.setString(1, dateStr.trim());
                //公司代码
                ps.setString(2, corpId.trim());
                //职位名称
                ps.setString(3, jsonarray.getJSONObject(i).getString("BUSINESS").trim());
                //姓名
                ps.setString(4, jsonarray.getJSONObject(i).getString("NAME").trim());
                //任职开始时间
                ps.setString(5, jsonarray.getJSONObject(i).getString("START_TIME").trim().replace("-", ""));
                //序号
                ps.setInt(6, Integer.valueOf(jsonarray.getJSONObject(i).getString("NUM").trim()));
                //数据来源
                ps.setString(7, "SH");
                //备注
                ps.setString(8, "");

                ps.addBatch();
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
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
