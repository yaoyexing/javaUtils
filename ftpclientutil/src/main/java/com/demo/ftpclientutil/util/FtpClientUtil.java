package com.demo.ftpclientutil.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.Random;

/**
 * ftp帮助类，提供了上传、下载、删除、创建、重命名等功能
 * @author yr
 */
@Slf4j
public class FtpClientUtil {


    private FTPClient ftpc;

    private FTPSClient ftp;

    private static Certificate cer = null;

    public FtpClientUtil(String crtPath){
        try {
            //CA证书
            CertificateFactory cerFactory = CertificateFactory.getInstance("X.509");
            cer = cerFactory.generateCertificate(new FileInputStream(crtPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接FTP服务器：需要验证证书
     * @param serverIP
     * @param user
     * @param password
     * @return boolean
     */
    public boolean connectFtpServer(String serverIP, String user, String password){
        ftp = new FTPSClient("SSL", true);
        try {
            //设置ftp验证服务器证书
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("trust", cer);
            ftp.setTrustManager(TrustManagerUtils.getDefaultTrustManager(ks));
            //连接ftp服务器
            ftp.connect(serverIP, 21);
            ftp.execPBSZ(0);
            ftp.execPROT("P");
            //用户登录
            ftp.login(user, password);
            //设置被动模式
            ftp.pasv();
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                ftp.disconnect();
                log.error("Ftp connect to " + serverIP + "error==> can not connect to :" + serverIP);
                return false;
            }
            if (!ftp.login(user, password)) {
                disconnect();
                log.error("Ftp connect to " + serverIP + "error==> the account:" + user +"/" + password +" is error");
                return false;
            }
            ftp.setConnectTimeout(60000);
        } catch (IOException e) {
            log.error("Ftp connect to " + serverIP + "use account:" + user +"/" + password +"error==>"+e.getMessage(), e);
            return false;
        }catch (GeneralSecurityException e) {
            log.error("Ftp connect to " + serverIP + "use account:" + user +"/" + password +"error crt==>"+e.getMessage(), e);
            return false;
        }catch(Exception e){
            log.error("Ftp connect to " + serverIP + "other error==>"+e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * 连接FTP服务器：不需要验证证书
     * @param serverIP
     * @param user
     * @param password
     * @return boolean
     */
    public boolean connectFtpServer(String serverIP, int port, String user,String password) throws Exception{
        boolean successFlag = true;
        ftpc = new FTPClient();
        try {
            ftp.connect(serverIP, port);
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                ftp.disconnect();
                log.error("Ftp connect to " + serverIP + "error==> can not connert to :" + serverIP);
                return false;
            }
            if (!ftp.login(user, password)) {
                disconnect();
                log.error("Ftp connect to " + serverIP + "error==> the account:" + user +"/" + password +" is error");
                successFlag = false;
                return successFlag;
            }
			ftp.setConnectTimeout(300000);
        } catch (IOException e) {
            log.error("Ftp connect to " + serverIP + "use account:" + user +"/" + password +"error==>"+e.getMessage(), e);
            throw e;
        }
        return successFlag;
    }

    /**
     * 上传文件，文件夹不存在时创建文件夹
     * @param byteContent 二进制的上传内容
     * @param fileDirectory 远程路径
     * @return boolean
     */
    public boolean uploadFile(byte[] byteContent, String fileDirectory) {
        boolean successFlag = true;
        if(ftp == null) {
            log.error("ftp上传文件错误");
            successFlag = false;
            return successFlag;
        }
        InputStream input = null;
        try {
            //设置二进制
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            //编码
            ftp.setControlEncoding("utf-8");
            //请求server开通端口
            ftp.enterLocalPassiveMode();
            //返回到当前用户的根目录
            ftp.cwd("/");
            String[] dirArray = fileDirectory.split("/");
            for (int i = 0; i < dirArray.length - 1; i++) {
                //文件夹不存在时创建文件夹
                if (!ftp.changeWorkingDirectory(dirArray[i])) {
                    if(ftp.makeDirectory(dirArray[i])) {
                        ftp.changeWorkingDirectory(dirArray[i]);
                    }
                }
            }
            input = new ByteArrayInputStream(byteContent);
            //文件名含有中文
            //ftp.storeFile(new String(dirArray[dirArray.length - 1].getBytes("UTF-8"), "iso-8859-1"), input);
            ftp.storeFile(dirArray[dirArray.length - 1], input);
        } catch (IOException e) {
            log.error("ftp上传文件出错", e);
            successFlag = false;
            return successFlag;
        } finally {
            IOUtils.closeQuietly(input);
        }
        return successFlag;
    }

    /**
     * 上传文件，文件夹不存在时创建文件夹
     * @param localPath 上传文件完整路径
     * @param remoteDirectory 远程路径
     * @return boolean
     */
    public boolean uploadFile(String localPath, String remoteDirectory) {
        boolean successFlag = true;
        if(ftp == null) {
            log.error("ftp上传文件错误");
            successFlag = false;
            return successFlag;
        }
        InputStream input = null;
        try {
            //设置二进制
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            //编码
            ftp.setControlEncoding("utf-8");
            //请求server开通端口
            ftp.enterLocalPassiveMode();
            //返回到当前用户的根目录
            ftp.cwd("/");
            String[] dirArray = remoteDirectory.split("/");
            for (int i = 0; i < dirArray.length - 1; i++) {
                //文件夹不存在时创建文件夹
                if (!ftp.changeWorkingDirectory(dirArray[i])) {
                    if(ftp.makeDirectory(dirArray[i])) {
                        ftp.changeWorkingDirectory(dirArray[i]);
                    }
                }
            }
            input = new FileInputStream(localPath);
            //文件路径含有中文
            //ftp.storeFile(new String(dirArray[dirArray.length - 1].getBytes("UTF-8"), "iso-8859-1"), input);
            ftp.storeFile(dirArray[dirArray.length - 1], input);
        } catch (IOException e) {
            log.error("ftp上传文件出错", e);
            successFlag = false;
            return successFlag;
        } finally {
            IOUtils.closeQuietly(input);
        }
        return successFlag;
    }

    /**
     * 下载文件
     * @param local(eg:/ss/22/11.txt)
     * @param remote(eg:/dd/2s/22.txt)
     * @return boolean
     */
    public boolean downloadFile(String local, String remote) {
        boolean successFlag = true;
        OutputStream output = null;
        try {
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
            String[] dirArray = remote.split("/");
            if (dirArray.length > 1) {
                for (int i = 0; i < dirArray.length - 1; i++) {
                    if (dirArray[i] != null) {
                        ftp.changeWorkingDirectory(dirArray[i]);
                    }
                }
            }
            File file = new File(local);
            output = new FileOutputStream(file);
            ftp.retrieveFile(dirArray[dirArray.length - 1], output);
        } catch (IOException e) {
            log.error("ftp downloadFile error==>", e);
            successFlag = false;
            return successFlag;
        } finally {
            IOUtils.closeQuietly(output);
        }
        return successFlag;
    }

    /**
     * 删除文件
     * @param filePath
     * @return boolean
     */
    public boolean deleteFile(String filePath) {
        boolean successFlag = true;
        try {
            String[] dirArray = filePath.split("/");
            if (dirArray.length > 1) {
                for (int i = 0; i < dirArray.length - 1; i++) {
                    if (dirArray[i] != null) {
                        ftp.changeWorkingDirectory(dirArray[i]);
                    }
                }
            }
            ftp.deleteFile(dirArray[dirArray.length - 1]);
        } catch (IOException e) {
            log.error("ftp deleteFile error==>", e);
            successFlag = false;
            return successFlag;
        }
        return successFlag;
    }

    /**
     * 删除文件夹
     * @param path
     * @return boolean
     */
    public boolean removeDirectory(String path) {
        boolean successFlag = true;
        try {
            String[] dirArray = path.split("/");
            for (int i = 0; i < dirArray.length; i++) {
                if (dirArray[i] != null) {
                    ftp.changeWorkingDirectory(dirArray[i]);
                }
            }
            FTPFile[] files = ftp.listFiles();
            for (int j = 0; j < files.length; j++) {
                if (files[j].isDirectory()) { // 递归删除子文件夹
                    removeDirectory(files[j].getName());
                }
                ftp.deleteFile(files[j].getName());
            }
            ftp.changeToParentDirectory();
            ftp.removeDirectory(dirArray[dirArray.length - 1]);
        } catch (IOException e) {
            log.error("ftp removeDirectory error==>",e);
            successFlag = false;
            return successFlag;
        }
        return successFlag;
    }

    /**
     * 重命名文件、文件夹
     * @param from
     * @param to
     * @return boolean
     */
    public boolean rename(String from, String to) {
        boolean successFlag = true;
        try {
            String[] dirArray = from.split("/");
            if (dirArray.length > 1) {
                for (int i = 0; i < dirArray.length - 1; i++) {
                    if (dirArray[i] != null) {
                        ftp.changeWorkingDirectory(dirArray[i]);
                    }
                }
            }
            ftp.rename(dirArray[dirArray.length - 1], to);
        } catch (IOException e) {
            log.error("ftp rename error==>", e);
            successFlag = false;
            return successFlag;
        }
        return successFlag;
    }


    /**
     *  断开连接
     */
    public void disconnect() {
        if (ftp != null && ftp.isConnected()) {
            try {
                ftp.logout();
            } catch (IOException e) {
                log.error("ftp log out error==>", e);
            } finally {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    log.error("ftp disconnect error==>", e);
                }
            }
        }
    }
}
