/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database.corp;

import cn.idaze.stockfunny.database.Connector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * 根据上交所股票清单生成高管信息导入作业
 * @author hhq
 */
public class ShStockCorpSeniorInfoJobsGenerator {
    public static void main(String[] args) {
        ShStockCorpSeniorInfoJobsGenerator generator = new ShStockCorpSeniorInfoJobsGenerator();
        Map<String, String> map = new HashMap<String, String>();
        map.put("DTE", "20180325");
        map.put("JOBID", "STK_SH_007");
        generator.execJob(map);
    }

    /**
     * 执行作业
     *
     * @param params 作业执行参数
     * @return
     */
    public boolean execJob(Map<String, String> params) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(ShStockCorpSeniorInfoJobsGenerator.class);

        String dateStr;
        String jobId;
        if (params.get("DTE") == null || params.get("JOBID") == null) {
            logger.error("参数非法，需要传入日期和作业代码!");
            return false;
        } else {
            dateStr = params.get("DTE").trim();
            jobId = params.get("JOBID").trim();
            logger.debug("执行参数为：DTE=" + dateStr + ", JOBID=" + jobId);
        }

        String selectSql = "select count(0) cnt from SCH_INFO where dte=? and pjobids like ?";
        String insertCorpGenelInfoJobSql = "insert into SCH_INFO select ?,concat(?,a.corpid), "
                + "concat(?,a.corpid),?,?,concat(?,a.corpid),?,?,?,null, null, sysdate(),sysdate(),? from "
                + "(select corpid from STOCK_LIST_A_SH where dte=? and datasource=? "
                + "union select corpid from STOCK_LIST_B_SH where dte=? and datasource=? "
                + "union select corpid from CORP_END_LIST_SH where dte=? and datasource=?) a";
        Connector connector = new Connector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(selectSql);

        try {
            ps.setString(1, dateStr);
            ps.setString(2, "%" + jobId + "%");
            ResultSet rs = ps.executeQuery();
            rs.next();
            //System.out.println(rs.getInt(1));
            if (rs.getInt(1) > 0) {
                logger.error("已存在 生成上交所公司高管信息获取作业 ，执行失败");
                return false;
            }

        } catch (SQLException sqlex) {
            logger.error("SQL执行失败！无法查询 生成上交所公司概况信息 ！" + sqlex.getMessage());
            return false;
        }

        try {
            ps = connector.prepareStmt(insertCorpGenelInfoJobSql);
            ps.setString(1, dateStr.trim());
            ps.setString(2, jobId.trim());
            ps.setString(3, "上交所公司高管信息获取");
            ps.setString(4, "cn.idaze.stockfunny.stock.database.corp.ShStockCorpSeniorInfoImporter");
            ps.setString(5, "execJob");
            ps.setString(6, "DTE=" + dateStr + ",CORP=");
            ps.setString(7, jobId);
            //设置作业状态
            ps.setString(8, "S");
            //设置执行次数
            ps.setInt(9, 0);
            //设置备注
            ps.setString(10, "");
            ps.setString(11, dateStr.trim());
            ps.setString(12, "SH");
            ps.setString(13, dateStr.trim());
            ps.setString(14, "SH");
            ps.setString(15, dateStr.trim());
            ps.setString(16, "SH");
            ps.execute();

            ps.close();
            conn.close();

        } catch (SQLException sqlex) {
            logger.error("插入作业SQL执行失败！生成上交所公司高管信息获取作业失败！" + sqlex.getMessage());
            return false;
        }

        logger.debug("生成上交所公司高管信息获取作业 成功！");
        return true;
    }
}
