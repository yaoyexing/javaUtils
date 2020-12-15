package com.demo.m3u8download.utils;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class UrlHandleUtils {
    /**
     * 根据指定url，通过get方式获取返回结果
     * @param url
     * @return
     * @throws Exception
     */
    public static String getResultByGet(String url) throws Exception{
        String result = "";
        GetMethod get = null;
        try {
            HttpClient httpClient = new HttpClient();
            get = new GetMethod(url);
            int httpStatus = httpClient.executeMethod(get);
            if (HttpStatus.SC_OK == httpStatus) {
                result = get.getResponseBodyAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            get.releaseConnection();
        }
        return result;
    }

    /**
     * 根据指定url，通过post方式获取返回结果
     * @param url
     * @return
     * @throws Exception
     */
    public static String getResultByPost(String url) throws Exception{
        String result = "";
        PostMethod post = null;
        try {
            HttpClient httpClient = new HttpClient();
            post = new PostMethod(url);
            int httpStatus = httpClient.executeMethod(post);
            if (HttpStatus.SC_OK == httpStatus) {
                result = post.getResponseBodyAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            post.releaseConnection();
        }
        return result;
    }

}
