/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.schedule;

import cn.idaze.stockfunny.database.Connector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.PropertyConfigurator;

/**
 * 调度总控程序
 *
 * @author hhq
 */
public class StockJobScheduler {

    public static void main(String[] args) {
        String dateStr = "20180409";
        boolean initFlag = false; //初始化作业标识
        StockJobScheduler scheduler = new StockJobScheduler();

        scheduler.startScheduling(dateStr, initFlag);

    }

    /**
     * 启动调动程序
     *
     * @param dateStr
     * @param initFlag
     * @return
     */
    public boolean startScheduling(final String dateStr, boolean initFlag) {
        //同时执行的作业个数
        final int maxThreads = 20;

        //1.初始化作业
        if (initFlag == true) {
            boolean b = initJobs(dateStr);
            if (b == true) {
                System.out.println("初始化作业执行成功");
            } else {
                System.out.println("初始化作业执行失败");
                return false;
            }
        }

        //2.设置作业为就绪
        new Thread(new Runnable() { //定期检查可执行的作业
            @Override
            public void run() {
                PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
                org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StockJobScheduler.class);
                String selectSql = "select count(0) cnt from SCH_INFO where dte=? and status <> 'N' ";

                Connector connector = new Connector();
                Connection conn = connector.getConnection();
                PreparedStatement ps = connector.prepareStmt(selectSql);

                while (true) {
                    try {
                        ps.setString(1, dateStr.trim());
                        ResultSet rs = ps.executeQuery();
                        rs.next();
                        if (rs.getInt("cnt") == 0) {
                            logger.debug("该日作业(" + dateStr + ")全部执行成功！");
                            ps.close();
                            conn.close();
                            return;
                        } else {
                            setJobReady(dateStr);
                        }
                    } catch (SQLException ex) {
                        logger.error("查询当天作业是否执行完成失败!" + ex.getMessage());

                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        logger.error(ex.getMessage());
                    }
                }

            }
        }).start();

        //3.执行作业：1）检查是否都执行成功；2）检查当前正在执行的作业数；3）执行作业
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StockJobScheduler.class);
        Connector connector = new Connector();
        Connection conn = connector.getConnection();
        PreparedStatement ps;
        while (true) {
            String selectCntSql = "select count(0) cnt from SCH_INFO where dte=? and status <> 'N' ";
            ps = connector.prepareStmt(selectCntSql);
            try {
                ps.setString(1, dateStr.trim());
                ResultSet rs = ps.executeQuery();
                rs.next();
                if (rs.getInt("cnt") == 0) { //无需要执行的作业
                    logger.debug("该日作业(" + dateStr + ")全部执行成功！");
                    ps.close();
                    conn.close();
                    return true;
                } else {
                    String selectProcessingSql = "select count(0) cnt from SCH_INFO where dte=? and status = 'P' ";
                    ps = connector.prepareStmt(selectProcessingSql);
                    ps.setString(1, dateStr.trim());
                    rs = ps.executeQuery();
                    rs.next();
                    int nProcessingJobs = rs.getInt("cnt");
                    if (nProcessingJobs >= maxThreads) { //当前执行的作业数在于预设的最大并发数
                        continue;
                    }

                    String selectReadySql = "select * from SCH_INFO where dte=? and status = 'R' ";
                    ps = connector.prepareStmt(selectReadySql);
                    ps.setString(1, dateStr.trim());
                    rs = ps.executeQuery();
                    while (rs.next() == true && nProcessingJobs < maxThreads) {
                        nProcessingJobs += 1;
                        execJob(dateStr.trim(), rs.getString("jobid"));
                    }
                }

                Thread.sleep(2000);

            } catch (SQLException ex) {
                logger.error("SQL执行失败!" + ex.getMessage());
                return false;

            } catch (InterruptedException ex) {
                logger.error(ex.getMessage());
                return false;
            }
        }
    }

    /**
     * 执行作业，并设置作业执行状态。只有处于就绪状态的作业才能执行。
     *
     * @param dateStr
     * @param jobId
     */
    public void execJob(final String dateStr, final String jobId) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
                org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StockJobScheduler.class);
                String selectSql = "select * from SCH_INFO where dte=? and jobid=? and status='R' order by exectimes ";

                Connector connector = new Connector();
                Connection conn = connector.getConnection();
                PreparedStatement ps = connector.prepareStmt(selectSql);

                try {
                    ps.setString(1, dateStr.trim());
                    ps.setString(2, jobId.trim());
                    ResultSet rs = ps.executeQuery();
                    rs.last();
                    if (rs.getRow() == 0) {
                        logger.debug("该作业(" + dateStr + "," + jobId + ")不存在或不处于就绪状态，执行失败！");
                    } else {
                        rs.first();
                        String className = rs.getString("classname").trim();
                        String classMethod = rs.getString("classmethod").trim();
                        String methodParas = rs.getString("methodparas").trim();
                        logger.info("jobid=" + jobId + ", classname=" + className + ", classmethod=" + classMethod + ", methodparas=" + methodParas);

                        Class<?> clazz = Class.forName(className);
                        Method method = clazz.getMethod(classMethod, Map.class);
                        setJobStatus(dateStr, jobId, "P");
                        Object obj = method.invoke(clazz.newInstance(), replaceMethodParas(methodParas, dateStr, "", "", "", jobId));
                        if ((Boolean) obj.equals(Boolean.TRUE) == true) {
                            setJobStatus(dateStr, jobId, "N");
                            logger.debug("作业(" + dateStr + "," + jobId + ")执行成功！");

                        } else {
                            setJobStatus(dateStr, jobId, "E");
                            logger.error("作业(" + dateStr + "," + jobId + ")执行失败！");
                        }
                    }

                    ps.close();
                    conn.close();
                } catch (SQLException ex) {
                    logger.error("获取作业信息（" + dateStr + "，" + jobId + "）失败！" + ex.getMessage());
                    setJobStatus(dateStr, jobId, "E");
                } catch (ClassNotFoundException cnfe) {
                    logger.error("执行作业失败！作业的执行类不存在（" + dateStr + "，" + jobId + "）失败！" + cnfe.getMessage());
                    setJobStatus(dateStr, jobId, "E");
                } catch (NoSuchMethodException nsme) {
                    logger.error("执行作业失败！作业的执行方法不存在（" + dateStr + "，" + jobId + "）失败！" + nsme.getMessage());
                    setJobStatus(dateStr, jobId, "E");
                } catch (IllegalAccessException iae) {
                    logger.error("执行作业失败！访问作业的执行方法非法（" + dateStr + "，" + jobId + "）失败！" + iae.getMessage());
                    setJobStatus(dateStr, jobId, "E");
                } catch (InvocationTargetException ite) {
                    logger.error("执行作业失败！访问作业的方法执行（" + dateStr + "，" + jobId + "）失败！" + ite.getMessage());
                    setJobStatus(dateStr, jobId, "E");
                } catch (InstantiationException ie) {
                    logger.error("执行作业失败！实例化对象（" + dateStr + "，" + jobId + "）失败！" + ie.getMessage());
                    setJobStatus(dateStr, jobId, "E");
                }
            }
        }).start();

    }

    /**
     * 替换作业参数，将模型中的参数转换为具体的map格式
     *
     * @param methodParas 作业参数字符串
     * @param dateStr 日期
     * @param corpId 公司代码
     * @param stockId 股票代码
     * @param source 来源代码
     * @param jobId 作业代码
     *
     * @return
     */
    public Map<String, String> replaceMethodParas(String methodParas, String dateStr, String corpId, String stockId, String source, String jobId) {
        Map<String, String> resultMap = new HashMap<String, String>();

        String[] paraStrs = methodParas.split(",");
        for (String para : paraStrs) {
            if (para.trim().equals("$DTE") == true) {
                resultMap.put("DTE", dateStr.trim());
            } else if (para.trim().equals("$CORP") == true) {
                resultMap.put("CORP", corpId.trim());
            } else if (para.trim().equals("$STOCK") == true) {
                resultMap.put("STOCK", stockId.trim());
            } else if (para.trim().equals("$SOURCE") == true) {
                resultMap.put("SOURCE", source.trim());
            } else if (para.trim().equals("$JOBID") == true) {
                resultMap.put("$JOBID", jobId.trim());
            } else { //不需要替换的直接写入
                String[] tmpStrs = para.trim().split("=");
                if (tmpStrs.length == 2 && tmpStrs[0].trim().equals("") == false) {
                    resultMap.put(tmpStrs[0].trim(), tmpStrs[1].trim());
                }
            }
        }

        return resultMap;
    }

    /**
     * 将符合条件的作业状态置为就绪R(Ready)。能置为R的作业必须满足以下条件： 1、该作业当前状态为待处理S；
     * 2、该作业父作业状态均为N，或者无父作业。
     *
     * @param dateStr
     * @return
     */
    public boolean setJobReady(String dateStr) {
//查找各个作业的父作业及父作业状态
//select aa.dte, aa.jobid, aa.pjobid, bb.status
//from
//(select b.dte, b.jobid, replace(substring_index(b.pjobids, ',', a.seq), concat(substring_index(b.pjobids, ',', a.seq -1), ','), '') as pjobid
//from HELP_SEQ a
//inner join
//(select dte, jobid, concat(pjobids, ',') as pjobids, length(pjobids)-length(replace(pjobids,',',''))+1 as size
//        from SCH_INFO where dte='20180312' and status='S') b 
//on a.seq <= b.size) aa
//left join (select * from SCH_INFO where dte='20180312') bb
//on aa.pjobid=trim(bb.jobid) //此处有bug，不加trim有部分数据关联不上，实际上没空格

//最终更新语句(当一个作业有三个或更多父作业时，仍有关联不上的情况，且存在截断部分父作业部分字符的情况),改为存储过程
//update SCH_INFO a set status='R'  where dte='20180312' and jobid in
//(select xx.jobid from
//(
//select aa.dte, aa.jobid, count(0) cnt, sum(case when bb.status='N' or aa.pjobid='0' then 1 else 0 end) successno
//from
//(select b.dte, b.jobid, replace(substring_index(b.pjobids, ',', a.seq), concat(substring_index(b.pjobids, ',', a.seq -1), ','), '') as pjobid
//from HELP_SEQ a
//inner join
//(select dte, jobid, concat(pjobids, ',') as pjobids, length(pjobids)-length(replace(pjobids,',',''))+1 as size
//        from SCH_INFO where dte='20180312' and status='S') b 
//on a.seq <= b.size) aa
//left join (select * from SCH_INFO where dte='20180312') bb
//on aa.pjobid=trim(bb.jobid)
//group by aa.dte, aa.jobid) xx
//where xx.cnt=xx.successno);
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StockJobScheduler.class);

//        String updateSql = "update SCH_INFO a set status='R', lastupdatetime=sysdate(6) where dte=? and jobid in "
//                + "(select xx.jobid from "
//                + "(select aa.dte, aa.jobid, count(0) cnt, sum(case when bb.status='N' or aa.pjobid='0' then 1 else 0 end) successno "
//                + "from "
//                + "(select b.dte, b.jobid, replace(substring_index(b.pjobids, ',', a.seq), "
//                + "concat(substring_index(b.pjobids, ',', a.seq -1), ','), '') as pjobid "
//                + "from HELP_SEQ a "
//                + "inner join "
//                + "(select dte, jobid, concat(pjobids, ',') as pjobids, length(pjobids)-length(replace(pjobids,',',''))+1 as size "
//                + "      from SCH_INFO where dte=? and status='S') b "
//                + "on a.seq <= b.size) aa "
//                + "left join (select * from SCH_INFO where dte=?) bb "
//                + "on aa.pjobid=trim(bb.jobid) "
//                + "group by aa.dte, aa.jobid) xx "
//                + "where xx.cnt=xx.successno)";
//
//        Connector connector = new Connector();
//        Connection conn = connector.getConnection();
//        PreparedStatement ps = connector.prepareStmt(updateSql);
//        int cnt = 0;
//        try {
//            ps.setString(1, dateStr.trim());
//            ps.setString(2, dateStr.trim());
//            ps.setString(3, dateStr.trim());
//            cnt = ps.executeUpdate();
//
//            ps.close();
//            conn.close();
//        } catch (SQLException ex) {
//            logger.error("设置作业状态为就绪（" + dateStr + "）失败！" + ex.getMessage());
//            return false;
//        }
        String procSql = "{CALL SET_JOB_READY(?)}";
        Connector connector = new Connector();
        Connection conn = connector.getConnection();
        try {
            CallableStatement cstm = conn.prepareCall(procSql);
            cstm.setString(1, dateStr); //存储过程输入参数 
            cstm.execute();
        } catch (SQLException ex) {
            logger.error("设置作业状态为就绪（" + dateStr + "）失败！" + ex.getMessage());
            return false;
        }

//        logger.debug("设置作业状态为就绪（" + dateStr + "," + cnt + "）成功！");
        logger.debug("设置作业状态为就绪（" + dateStr + "）成功！");
        return true;
    }

    /**
     * 更新作业状态，同时更新“最后更新时间、开始执行时间、结束执行时间”字段，如果将作业状态更新为处理中P，则执行次数+1
     *
     * @param dateStr 作业日期
     * @param jobId 作业代码
     * @param status 作业状态
     * @return
     */
    public synchronized boolean setJobStatus(String dateStr, String jobId, String status) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StockJobScheduler.class);

        //如果将作业状态置为P，则处理次数+1
        String updateSql = "update SCH_INFO set status=?, lastupdatetime=sysdate(6), "
                + "exectimes=exectimes + (case when upper(?)='P' then 1 else 0 end), "
                + "begtime=(case when upper(?)='P' then sysdate(6) else begtime end), "
                + "endtime=(case when upper(?)='N' or upper(?)='E' then sysdate(6) else endtime end) where dte=? and jobid=?";
        Connector connector = new Connector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(updateSql);
        try {
            ps.setString(1, status.trim());
            ps.setString(2, status.trim());
            ps.setString(3, status.trim());
            ps.setString(4, status.trim());
            ps.setString(5, status.trim());
            ps.setString(6, dateStr.trim());
            ps.setString(7, jobId);
            ps.execute();

            ps.close();
            conn.close();
        } catch (SQLException ex) {
            logger.error("更新作业状态（" + dateStr + "," + jobId + "," + status + "）失败！" + ex.getMessage());
            return false;
        }

        logger.debug("更新作业状态（" + dateStr + "," + jobId + "," + status + "）成功！");
        return true;
    }

    /**
     * 初始化作业信息，同时将作业参数$DTE替换为具体日期
     *
     * @param dateStr
     * @return
     */
    private boolean initJobs(String dateStr) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StockJobScheduler.class);

        //读取作业信息，暂时只处理按日处理的作业，即频度为D的作业
        String selectSchSql = "select count(0) from SCH_INFO where dte=?";
        String selectJobSql = "select distinct jobid, jobname from JOB_INFO where freq='D' and status='N' and ? between begdate and enddate";
        Connector connector = new Connector();
        Connection conn = connector.getConnection();
        PreparedStatement ps = connector.prepareStmt(selectSchSql);

        try {
            ps.setString(1, dateStr);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                logger.error(dateStr + "日作业信息已存在，执行失败！如需重新执行，请手动删除该日作业信息！");
                return false;
            }

            ps = connector.prepareStmt(selectJobSql);
            ps.setString(1, dateStr);
            rs = ps.executeQuery();
            rs.last();
            if (rs.getRow() == 0) {
                logger.debug("无需要初始化的作业（" + dateStr + "），直接返回成功！");
                return true;
            } else {
                logger.debug("需要初始化的作业数为：" + rs.getRow());

                String insertSql = "insert into SCH_INFO select ?, a.jobid, a.jobname, "
                        + "a.classname, a.classmethod, replace(a.methodparas, '$DTE', ?), a.pjobs, 'S', "
                        + "0, null, null, sysdate(6), sysdate(6), '' "
                        + "from (select jobid, jobname, classname, classmethod, methodparas, "
                        + "group_concat(pjobid order by pjobid asc separator ',') pjobs "
                        + "from JOB_INFO where freq='D' and status='N' and ? between begdate and enddate "
                        + "group by jobid, jobname, classname, classmethod, methodparas) a";
                ps = connector.prepareStmt(insertSql);
                ps.setString(1, dateStr);
                ps.setString(2, "DTE=" + dateStr);
                ps.setString(3, dateStr);
                ps.execute();

                rs.close();
                ps.close();
                conn.close();
            }
        } catch (SQLException ex) {
            logger.error("初始化作业（" + dateStr + "）失败！" + ex.getMessage());
            return false;
        }

        logger.debug("初始化作业（" + dateStr + "）成功！");
        return true;
    }

}
