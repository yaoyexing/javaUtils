package com.demo.m3u8download.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MergeTsMain {

    // 源文件夹
    private static File srcFolder;
    // 目的文件夹
    private static File destFolder;
    // 存放所有待命名的文件
    private static List<File> srcFiles;
    // 存放已经重命名好的ts文件
    private static List<File> destFiles;
    // 生成的MP4文件名称
    private static String fileName;

    static {
        srcFolder = new File("D:\\workApplication\\TSMergeTool\\ts");
        destFolder = new File("D:\\workApplication\\TSMergeTool\\tsNew");
        srcFiles = new ArrayList<File>();
        destFiles = new ArrayList<File>();
    }

    private static void checkFolder() throws FileNotFoundException {
        if (!MergeTsMain.srcFolder.exists()) {
            throw new FileNotFoundException("指定的源文件夹不存在！");
        }
        if (!MergeTsMain.destFolder.exists()) {
            throw new FileNotFoundException("指定的目标文件夹不存在！");
        }
    }

    private static void getSrcFilesList(File srcFile) {
        // 如果是文件夹,就继续深入遍历
        if (srcFile.isDirectory()) {
            File[] files = srcFile.listFiles();
            for (File each : files) {
                getSrcFilesList(each);
            }
        } else if (srcFile.getAbsolutePath().endsWith("_ts")) {
            // 不是文件夹而且文件格式以_ts结尾，就将该文件添加到待命名文件的list集合中
            MergeTsMain.srcFiles.add(srcFile);
        }
    }

    private static void renameSrcFiles() {
        String tsName = null;
        String tempStr = null;
        StringBuilder strBuilder = new StringBuilder();
        File tempFile = null;
        String sequenceNumber = null;
        String detailName = null;
        // 遍历list集合,逐个进行重命名
        for (File each : MergeTsMain.srcFiles) {
            // 获取文件名称(除去后缀名"_ts")
            tsName = each.getName().substring(0, each.getName().length() - 3);
            strBuilder.append(tsName + ".ts");
            // 新文件的path
            tempFile = new File(MergeTsMain.destFolder, strBuilder.toString());
            //核心代码(实现重命名和移动)
            each.renameTo(tempFile);
            // 切记将strBuilder进行清空
            strBuilder.delete(0, strBuilder.length());
        }
    }

    private static void getDestFilesList(File destFile) {
        // 如果是文件夹,就继续深入遍历
        if (destFile.isDirectory()) {
            File[] files = destFile.listFiles();
            for (File each : files) {
                getDestFilesList(each);
            }
        } else if (destFile.getAbsolutePath().endsWith(".ts")) {
            // 不是文件夹而且文件格式以.ts结尾，就将该文件添加到待合并ts文件的list集合中
            MergeTsMain.destFiles.add(destFile);
        }
    }

    /**
     * 合并重命名好的ts片段
     */
    private static void mergeTs() {
        try {
            File file = new File(destFolder + "\\" + fileName + ".mp4");
            System.out.println(destFolder + "\\" + fileName + ".mp4");
            System.gc();
            if (file.exists()) {
                file.delete();
            }else{
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] b = new byte[4096];
            for (File f : destFiles) {
                FileInputStream fileInputStream = new FileInputStream(f);
                int len;
                while ((len = fileInputStream.read(b)) != -1) {
                    fileOutputStream.write(b, 0, len);
                }
                fileInputStream.close();
                fileOutputStream.flush();
            }
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除重命名的ts片段
     */
    private static void deleteDestFiles() {
        for (File f : MergeTsMain.destFiles) {
            if (f.getName().endsWith(".ts")){
                f.delete();
            }
        }
    }


    public static void main(String[] args) {
        fileName = "test";
        // 对文件夹的合法性(是否存在)进行校验
        try {
            checkFolder();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        // 遍历源文件夹把要修改的文件放到集合中
        getSrcFilesList(MergeTsMain.srcFolder);
        // 对集合中的元素进行重命名(并移动到目标文件夹)
        renameSrcFiles();
        // 把ts文件放进集合中
        getDestFilesList(MergeTsMain.destFolder);
        // 合并重命名好的ts片段
        mergeTs();
        // 删除重命名的ts片段
        deleteDestFiles();
    }
}
