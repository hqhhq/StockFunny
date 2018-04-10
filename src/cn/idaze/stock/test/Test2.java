/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 *
 * @author hhq
 */
public class Test2 {

    public static void main(String[] args) throws IOException {
        System.out.println("1992-02-02".replaceAll("-", ""));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("kkmmss");
        System.out.println(sdf.format(new GregorianCalendar().getTime()));
        System.out.println(sdf2.format(new GregorianCalendar().getTime()));

//        HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream("/home/hhq/桌面/stock/database/stock/上市公司列表_20121222.xls"));
//        HSSFSheet sheet = workbook.getSheetAt(0);
//        
//        System.out.println(sheet.getPhysicalNumberOfRows());
    }
}
