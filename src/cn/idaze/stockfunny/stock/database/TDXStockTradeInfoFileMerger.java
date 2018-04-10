/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stockfunny.stock.database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 将每只股票的信息合并成一个文件
 *
 * @author HHQ
 */
public class TDXStockTradeInfoFileMerger {

    public static void main(String[] args) {
        String path = "E:/tmp/test";
        String desFile = "E:/tmp/desFile.txt";
        String cycle = "D1";
        String adjtype = "F"; //Forward、Backward、No
        String beginDate = "19880101";
        String endDate = "20160930";

        TDXStockTradeInfoFileMerger merger = new TDXStockTradeInfoFileMerger();
        List<File> files = merger.getFiles(path);
        new TDXStockTradeInfoFileMerger().mergeFiles(files, new File(desFile), beginDate, endDate);
    }

    private void mergeFiles(List<File> files, File desFile, String beginDate, String endDate) {
        //desFile.deleteOnExit();
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(desFile), "GBK"));
            for (File file : files) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
                String newLine;
                String stockId = file.getName().split("\\.")[0].split("#")[1];
                while ((newLine = in.readLine()) != null) {
                    //跳过非数据行
                    if (newLine.trim().isEmpty() || newLine.trim().equals("数据来源:通达信")) {
                        continue;
                    }
                    out.write(stockId+","+newLine.substring(0, 4)+newLine.substring(5,7)+newLine.substring(8));
                    out.newLine();
                }
            }
            out.flush();
            out.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TDXStockTradeInfoFileMerger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TDXStockTradeInfoFileMerger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TDXStockTradeInfoFileMerger.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    //获取文件夹下所有文件
    private List<File> getFiles(String path) {
        File file = new File(path);
        File[] tmpFiles = file.listFiles();
        List<File> fileList = new ArrayList<File>();
        for (File f : tmpFiles) {
            if (f.isFile()) {
                fileList.add(f);
            }
        }

        return fileList;
    }
}
