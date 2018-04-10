/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author hhq
 */
public class TestLog4j {

    public static void main(String[] args) {
        PropertyConfigurator.configure(System.getProperty("user.dir") + "/log4j.properties");
        Logger logger = Logger.getLogger(TestLog4j.class);
        //logger.setLevel(Level.INFO); 
        logger.debug("This is a bug~~~好好好");
        logger.error("This ia an error~~~好好好！@@@！");
    }
}
