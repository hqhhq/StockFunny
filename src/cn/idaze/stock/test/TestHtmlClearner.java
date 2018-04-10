/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

/**
 *
 * @author hhq
 */
public class TestHtmlClearner {

    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("/home/hhq/Desktop/result.txt"), "UTF8"));
        String newLine;
        StringBuilder sb = new StringBuilder();
        while ((newLine = in.readLine()) != null) {
            sb.append(newLine);
        }
        TagNode tagNode = new HtmlCleaner().clean(sb.toString());
        org.w3c.dom.Document doc;
        try {
            doc = new DomSerializer(new CleanerProperties()).createDOM(tagNode);

            XPath xpath = XPathFactory.newInstance().newXPath();
            //String result = (String) xpath.evaluate("//form[@name='mainForm']/table/tbody/tr[2]/td[1]/div[1]/table/tbody/tr[1]/td[1]/table/tbody/tr[2]/td[4]", doc, XPathConstants.STRING);
            String result = (String) xpath.evaluate("//table[@class='hqdataContainer']/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[1]/td[1]", doc, XPathConstants.STRING);
            System.out.println(result);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "转换配置异常", ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "XPath表达式异常", ex);
        }
    }

}
