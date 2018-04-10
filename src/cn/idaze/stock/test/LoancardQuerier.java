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
 * @author Administrator
 */
public class LoancardQuerier {

    public static String baseUrlStr = "http://10.2.60.41:7001/shwebroot/orgBaseInfoQueryOnlineAction.do?typecode=loacard";
    public static String baseRefererStr = "http://10.2.60.41:7001/shwebroot/identity/loancardlist.jsp";

    public static void main(String[] args) throws Exception {
        LoancardQuerier lcq = new LoancardQuerier();
        System.out.println("组织机构代码：77128270-4，中征码：" + lcq.queryLoancard("77128270-4", ""));//3201010007239704
        System.out.println("组织机构代码：75329745-1，中征码：" + lcq.queryLoancard("75329745-1", ""));
        System.out.println("组织机构代码：67769901-X，中征码：" + lcq.queryLoancard("67769901-X", ""));

        System.out.println("统一社会信用代码：913302117532974515，中征码：" + lcq.queryLoancard("", "913302117532974515"));
        System.out.println("统一社会信用代码：913201187712827042，中征码：" + lcq.queryLoancard("", "913201187712827042"));
        System.out.println("统一社会信用代码：9133028257769901X6，中征码：" + lcq.queryLoancard("", "9133028257769901X6"));
        System.out.println("统一社会信用代码：9133028267769901X6，中征码：" + lcq.queryLoancard("", "9133028267769901X6"));//3302820000507793

        System.out.println("组织机构代码：77128270-x，统一社会信用代码：9133028267769901X6，中征码：" + lcq.queryLoancard("77128270-x", "9133028267769901X6")); //3302820000507793

    }

    /**
     * 根据给定的组织机构代码或社会统一信用代码查询中证码（贷款卡号）
     *
     * @param orgCode 组织机构代码，此处只做是否为空判断，不做其它检查
     * @param creditCode 社会统一信用代码，此处只做是否为空判断，不做其它检查
     * @return 查到的中证码,若返回null或空串则未查询到
     */
    public String queryLoancard(String orgCode, String creditCode) {
        if (orgCode != null && orgCode.trim().equals("") == false) { //组织机构代码不为null或空
            String rst = queryLoancardByOrgCode(orgCode.trim());
            if (rst != null && rst.trim().equals("") == false) { //通过组织机构代码查询到中征码
                return rst.trim();
            } else if (creditCode != null && creditCode.trim().equals("") == false) { //未通过组织机构代码查询到中征码，改由通过社会统一信用代码查询
                rst = queryLoancardByCreditCode(creditCode.trim());
                if (rst != null && rst.trim().equals("") == false) { //通过社会统一信用代码查询到中征码
                    return rst.trim();
                } else { //未通过社会统一信用代码查询到中征码
                    return "";
                }
            } else {//未通过组织机构代码查询到中征码，且社会统一信用代码为null或空
                return "";
            }
        } else if (creditCode != null && creditCode.trim().equals("") == false) {//社会统一信用代码不为null或空
            String rst = queryLoancardByCreditCode(creditCode.trim());
            if (rst != null && rst.trim().equals("") == false) { //通过社会统一信用代码查询到中征码
                return rst.trim();
            } else { //未通过社会统一信用代码查询到中征码
                return "";
            }
        } else { //查询条件均为null或空，直接返回
            return "";
        }
    }

    /**
     * 通过组织机构代码查询中征码
     *
     * @param orgCode 组织机构代码
     * @return 查到的中证码,若返回null或空串则未查询到
     */
    private String queryLoancardByOrgCode(String orgCode) {
        return queryLoancardExecutor(orgCode, "1");
    }

    /**
     * 通过社会统一信用代码查询中征码
     *
     * @param creditCode 社会统一信用代码
     * @return 查到的中证码,若返回null或空串则未查询到
     */
    private String queryLoancardByCreditCode(String creditCode) {
        return queryLoancardExecutor(creditCode, "2");
    }

    /**
     * 中征码查询执行方法
     *
     * @param queryParm 查询参数
     * @param queryType 参数类别，"1"-组织机构代码；"2"-社会统一信用代码
     * @return
     */
    private String queryLoancardExecutor(String queryParm, String queryType) {
        String responseStr = "";
        String loancardNo = "";
        BasicCookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();

        this.getCookie(httpClient, cookieStore);
//        this.printCookies(cookieStore.getCookies());

//        System.out.println("开始模拟登录...");
        login(httpClient, cookieStore);

//        System.out.println("开始模拟查询...");
        HttpPost httpPost = new HttpPost(baseUrlStr);
        Header[] headers = new Header[9];
        headers[0] = new BasicHeader("Host", "10.2.60.41:7001");
        headers[1] = new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:53.0) Gecko/20100101 Firefox/53.0");
        headers[2] = new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers[3] = new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        headers[4] = new BasicHeader("Accept-Encoding", "gzip, deflate");
        headers[5] = new BasicHeader("Content-Type", "application/x-www-form-urlencoded");
        headers[6] = new BasicHeader("Referer", baseRefererStr);
        headers[7] = new BasicHeader("Connection", "keep-alive");
        headers[8] = new BasicHeader("Cache-Control", "no-cache");
        httpPost.setHeaders(headers);

        //创建参数列表
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        list.add(new BasicNameValuePair("attribute", "0"));
        list.add(new BasicNameValuePair("creditcode", ""));
        list.add(new BasicNameValuePair("loancardno", ""));
        list.add(new BasicNameValuePair("queryreason", "1"));
        if (queryType.equals("2") == true) { //"2"-社会统一信用代码
            list.add(new BasicNameValuePair("registercode", queryParm));
            list.add(new BasicNameValuePair("registertype", "07"));
        } else {
            list.add(new BasicNameValuePair("registercode", ""));
            list.add(new BasicNameValuePair("registertype", ""));
        }
        list.add(new BasicNameValuePair("sdeplandtaxcode", ""));
        list.add(new BasicNameValuePair("sdepnationaltaxcode", ""));
        if (queryType.equals("1") == true) { //"1"-组织机构代码
            list.add(new BasicNameValuePair("sdeporgcode", queryParm));
        } else {
            list.add(new BasicNameValuePair("sdeporgcode", ""));
        }
        list.add(new BasicNameValuePair("searchType", "1"));
        list.add(new BasicNameValuePair("type", ""));
        list.add(new BasicNameValuePair("typecode", "loacard"));

        //url格式编码
        UrlEncodedFormEntity uefEntity;
        try {
            uefEntity = new UrlEncodedFormEntity(list, "GBK");

            httpPost.setEntity(uefEntity);

            HttpResponse httpResponse = httpClient.execute(httpPost); //throws IOException, ClientProtocolException
//            printCookies(cookieStore.getCookies());

            HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
            String brLine;
            StringBuilder sb = new StringBuilder();
            while ((brLine = br.readLine()) != null) {
                sb.append(brLine);
            }
            //返回内容
            responseStr = sb.toString().trim();
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "发生编码不支持异常", ex);
        } catch (IOException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "IO异常", ex);
        }

        TagNode tagNode = new HtmlCleaner().clean(responseStr);
        org.w3c.dom.Document doc;
        try {
            doc = new DomSerializer(new CleanerProperties()).createDOM(tagNode);

            XPath xpath = XPathFactory.newInstance().newXPath();
            loancardNo = (String) xpath.evaluate("//form[@name='mainForm']/table/tbody/tr[2]/td[1]/div[1]/table/tbody/tr[1]/td[1]/table/tbody/tr[2]/td[4]", doc, XPathConstants.STRING);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "转换配置异常", ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "XPath表达式异常", ex);
        }
        return loancardNo;
    }

    /**
     * 模拟登录
     *
     * @param httpClient
     * @param cookieStore
     */
    private void login(CloseableHttpClient httpClient, BasicCookieStore cookieStore) {
        HttpPost httpPost = new HttpPost("http://10.2.60.41:7001/shwebroot/logon.do");
        Header[] headers = new Header[9];
        headers[0] = new BasicHeader("Host", "10.2.60.41:7001");
        headers[1] = new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:53.0) Gecko/20100101 Firefox/53.0");
        headers[2] = new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers[3] = new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        headers[4] = new BasicHeader("Accept-Encoding", "gzip, deflate");
        headers[5] = new BasicHeader("Content-Type", "application/x-www-form-urlencoded");
        headers[6] = new BasicHeader("Referer", "http://10.2.60.41:7001/shwebroot/index.jsp");
        headers[7] = new BasicHeader("Connection", "keep-alive");
        headers[8] = new BasicHeader("Upgrade-Insecure-Requests", "1");
        httpPost.setHeaders(headers);

        //创建参数列表
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        Map<String, String> userInfo = getUserInfo();
        list.add(new BasicNameValuePair("orgCode", userInfo.get("org")));
        list.add(new BasicNameValuePair("userid", userInfo.get("acct")));
        list.add(new BasicNameValuePair("password", userInfo.get("pwd")));

        StringBuilder sb = new StringBuilder();
        try {
            //url格式编码
            UrlEncodedFormEntity uefEntity = new UrlEncodedFormEntity(list, "gb2312");
            httpPost.setEntity(uefEntity);

            HttpResponse httpResponse = httpClient.execute(httpPost); //throws IOException, ClientProtocolException
//            printCookies(cookieStore.getCookies());

            HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "gb2312"));
            String brLine;

            while ((brLine = br.readLine()) != null) {
                sb.append(brLine);
            }

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "登录时发生字符编码格式异常", ex);
        } catch (IOException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "登录时IO异常", ex);
        } catch (UnsupportedOperationException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "登录时出现不支持操作异常", ex);
        }
        //返回内容
//        String responseStr = sb.toString();
//        System.out.println(responseStr);
//
//        System.out.println("登录完成...");
    }

    /**
     * 获取cookie
     *
     * @param httpClient
     * @param cookieStore
     */
    private void getCookie(CloseableHttpClient httpClient, BasicCookieStore cookieStore) {
        HttpGet httpGet = new HttpGet("http://10.2.60.41:7001/shwebroot/index.jsp");

        Header[] headers = new Header[7];
        headers[0] = new BasicHeader("Host", "10.2.60.41:7001");
        headers[1] = new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:53.0) Gecko/20100101 Firefox/53.0");
        headers[2] = new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers[3] = new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        headers[4] = new BasicHeader("Accept-Encoding", "gzip, deflate");
        headers[5] = new BasicHeader("Connection", "keep-alive");
        headers[6] = new BasicHeader("Upgrade-Insecure-Requests", "1");
        httpGet.setHeaders(headers);

        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
//            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.INFO, "Login form get: " + response.getStatusLine());
            EntityUtils.consume(entity);

//            List<Cookie> cookies = cookieStore.getCookies();
//            printCookies(cookies);
            response.close();
        } catch (IOException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "获取cookie发生IO异常", ex);
        }
    }

    /**
     * 输出cookie明细信息
     *
     * @param cookies
     */
    private void printCookies(List<Cookie> cookies) {
        if (cookies.isEmpty()) {
            System.out.println("No Cookie~");
        } else {
            for (int i = 0; i < cookies.size(); i++) {
                System.out.println("- " + cookies.get(i).toString());
            }
        }
    }

    /**
     * 获取登录所需用户密码信息
     *
     * @return 返回登录时所需的用户密码信息
     */
    private Map<String, String> getUserInfo() {
        Map<String, String> userInfo = new HashMap<String, String>();
        userInfo.put("org", "");
        userInfo.put("acct", "");
        userInfo.put("pwd", "");

        File directory = new File("");
        String cfgFile = directory.getAbsolutePath() + File.separator + "resources" + File.separator + "conf" + File.separator + "loancardUser.cfg";
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(cfgFile), "UTF-8"));
            String brLine;
            while ((brLine = in.readLine()) != null) {
                String[] strs = brLine.split(":");
                if (strs.length == 2) {
                    userInfo.put(strs[0].trim(), strs[1].trim());
                }
            }
            in.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "找不到登录所用的用户密码信息", ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "编码格式不支持", ex);
        } catch (IOException ex) {
            Logger.getLogger(LoancardQuerier.class.getName()).log(Level.SEVERE, "读取登录所需用户密码信息异常", ex);
        }

        return userInfo;
    }
}