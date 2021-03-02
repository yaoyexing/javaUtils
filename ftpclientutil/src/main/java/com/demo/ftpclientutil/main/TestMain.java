package com.demo.ftpclientutil.main;

import com.demo.ftpclientutil.util.FtpClientUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestMain {

    // ftp地址
    private static String ftpIp = "ftps.tms.cntv.net";
    // 用户名
    private static String user = "newftp.test.cn";
    // 密码
    private static String password = "J0YErFgeK9smJzEG";
    // 本地文件全路径
    private static String localPath = "E:\\zuzhuang.mp4";
    // 远程文件路径
    private static String remoteDirectory = "/cportal/data";
    // 证书地址
    private static String cerPath = "D:\\tomcat-config\\cntvCA.crt";

    public static void main(String[] args) {
        // 需要验证证书
        if (ftpIp != null && ftpIp.length() > 0) {
            FtpClientUtil ftpClientUtil = new FtpClientUtil(cerPath);
            try{
                ftpClientUtil.connectFtpServer(ftpIp, user, password);
            }catch(Exception e){
                log.error("ftp连接错误", e);
            }
            boolean successFlag = ftpClientUtil.uploadFile(localPath, remoteDirectory);
            ftpClientUtil.disconnect();
            if (!successFlag) {
                log.error("ftp上传失败");
                throw new RuntimeException("ftp falled! for more info, see the log from FtpClientUtil!");
            }
        }
        // 不需要验证证书
        /*if (ftpIp != null && ftpIp.length() > 0) {
            FtpClientUtil ftpClientUtil = new FtpClientUtil();
            try{
                ftpClientUtil.connectFtpServer(ftpIp, 21, user, password);
            }catch(Exception e){
                log.error("ftp连接错误", e);
            }
            boolean successFlag = ftpClientUtil.uploadFile(localPath, remoteDirectory);
            ftpClientUtil.disconnect();
            if (!successFlag) {
                log.error("ftp上传失败");
                throw new RuntimeException("ftp falled! for more info, see the log from FtpClientUtil!");
            }
        }*/
    }
}
