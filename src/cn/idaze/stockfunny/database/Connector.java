/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.database;

import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.Properties;

/**
 *
 * @author hhq
 */
public class Connector {

    private String driverClass;
    private String url;//此为NO-DSN方式
    private String user;
    private String pwd;
    private Connection conn = null;
    private Statement stmt = null;

    /** Creates a new instance of Connector */
    public Connector() {
        Properties prop = new Properties();
        try {
            FileReader in = new FileReader("cfg.properties");
            prop.load(in);
            driverClass = prop.getProperty("db.driverClass", "com.mysql.jdbc.Driver");
            //注意加上后面的参数，否则插入数据库乱码。且有可能因为缓存原因不能及时生效，需要清理再构建
            url = prop.getProperty("db.url", "jdbc:mysql://localhost:3306/stock?useUnicode=true&characterEncoding=UTF-8");
            user = prop.getProperty("db.user", "root");
            pwd = prop.getProperty("db.pwd", "root");
            in.close();
        } catch (Exception e) {
            System.out.println("读取数据库属性文件失败!");
            System.out.print(e.getMessage());
        }
    }

    public Connector(String url, String user, String pwd) {
        this.url = url;
        this.user = user;
        this.pwd = pwd;
    }

    public Connection getConnection() {
        try {
            Class.forName(driverClass);
            conn = DriverManager.getConnection(url, user, pwd);
        } catch (ClassNotFoundException ex) { //发生异常
            System.out.println("无法装载驱动程序!");
            System.exit(1);
        } catch (SQLException ex) {
            System.out.println("================连接数据库失败!数据库可能未启动================");
            ex.printStackTrace();
            System.exit(1);
        }
        return conn;
    }

    public Statement getStatement() {
        try {
            stmt = getConnection().createStatement();
        } catch (SQLException e) {//发生异常,退出
            e.printStackTrace();
            System.exit(1);
        }

        return stmt;
    }

    public ResultSet executeQuery(String sql) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }

    public int executeUpdate(String sql) {
        Statement stmt = null;
        int ret = 0;
        try {
            stmt = conn.createStatement();
            ret = stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public PreparedStatement prepareStmt(String sql) {
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pstmt;
    }

    public void close() {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            stmt = null;
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            conn = null;
        }
    }

    public static void main(String[] args) throws Exception {
//        System.out.println(
//                Thread.currentThread().getContextClassLoader().getResource(""));
//        System.out.println(Connector.class.getClassLoader().getResource(""));
//        System.out.println(ClassLoader.getSystemResource(""));
//        System.out.println(Connector.class.getResource(""));
//        System.out.println(Connector.class.getResource("/"));
//        System.out.println(new File("").getAbsolutePath());
//        System.out.println(System.getProperty("user.dir"));

        Connector c = new Connector();
        Connection conn = c.getConnection();
        Statement stmt = c.getStatement();
        c.close();
    }
}
