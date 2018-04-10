/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import cn.idaze.stockfunny.database.Connector;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author hhq
 */
public class Test_20180325 {

    public static void main(String[] args) throws SQLException {
        String procSql = "{CALL SET_JOB_READY(?)}";
        Connector connector = new Connector();
        Connection conn = connector.getConnection();
        CallableStatement cstm = conn.prepareCall(procSql);
        cstm.setString(1, "20180325"); //存储过程输入参数 
//cstm.setInt(2, 2); // 存储过程输入参数 
//cstm.registerOutParameter(2, Types.INTEGER); // 设置返回值类型 即返回值 
        cstm.execute(); // 执行存储过程 
    }

}
