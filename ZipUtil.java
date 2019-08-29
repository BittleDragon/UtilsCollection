package com.xh.common.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip压缩/解压缩工具类
 * 实现对目标路径及其子路径下的所有文件及空目录的压缩
 */
public class ZipUtil {

    /**
     * 缓冲器大小
     */
    private static final int BUFFER = 2048;

    /**
     * 取的给定源目录下的所有文件及空的子目录
     * 递归实现
     *
     * @param srcFile 源文件
     * @return List<File>
     */
    private static List<File> getAllFiles(File srcFile) {
        List<File> fileList = new ArrayList<File>();
        File[] tmp = srcFile.listFiles();

        for (int i = 0; i < tmp.length; i++) {

            if (tmp[i].isFile()) {
                fileList.add(tmp[i]);
                System.out.println("add file: " + tmp[i].getName());
            }

            if (tmp[i].isDirectory()) {
                if (tmp[i].listFiles().length != 0) {//若不是空目录，则递归添加其下的目录和文件
                    fileList.addAll(getAllFiles(tmp[i]));
                } else {//若是空目录，则添加这个目录到fileList
                    fileList.add(tmp[i]);
                    System.out.println("add empty dir: " + tmp[i].getName());
                }
            }
        }    // end for

        return fileList;
    }

    /**
     * 取相对路径
     * 依据文件名和压缩源路径得到文件在压缩源路径下的相对路径
     *
     * @param dirPath 压缩源路径
     * @param file    file
     * @return 相对路径
     */
    private static String getRelativePath(String dirPath, File file) {
        File dir = new File(dirPath);
        String relativePath = file.getName();

        while (true) {
            file = file.getParentFile();

            if (file == null) {
                break;
            }

            if (file.equals(dir)) {
                break;
            } else {
                relativePath = file.getName() + "/" + relativePath;
            }
        }    // end while

        return relativePath;
    }

    /**
     * 创建文件
     * 根据压缩包内文件名和解压缩目的路径，创建解压缩目标文件，
     * 生成中间目录
     *
     * @param dstPath  解压缩目的路径
     * @param fileName 压缩包内文件名
     * @return 解压缩目标文件
     * @throws IOException
     */
    private static File createFile(String dstPath, String fileName) throws IOException {
        String[] dirs = fileName.split("/");//将文件名的各级目录分解
        File file = new File(dstPath);

        if (dirs.length > 1) {//文件有上级目录
            for (int i = 0; i < dirs.length - 1; i++) {
                file = new File(file, dirs[i]);//依次创建文件对象直到文件的上一级目录
            }

            if (!file.exists()) {
                boolean b = file.mkdirs();//文件对应目录若不存在，则创建
                if (!b) {
                    return null;
                }
                System.out.println("mkdirs: " + file.getCanonicalPath());
            }

            file = new File(file, dirs[dirs.length - 1]);//创建文件

            return file;
        } else {
            if (!file.exists()) {
                boolean b = file.mkdirs();//若目标路径的目录不存在，则创建
                if (!b) {
                    return null;
                }
                System.out.println("mkdirs: " + file.getCanonicalPath());
            }

            file = new File(file, dirs[0]);//创建文件

            return file;
        }
    }

    /**
     * 解压缩方法
     *
     * @param zipFileName 压缩文件名
     * @param dstPath     解压目标路径
     * @param callBack 进度回调
     * @return 是否成功
     */
    public static boolean unzip(String zipFileName, String dstPath, ZipCallBack callBack) {
        System.out.println("zip uncompressing...");

        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFileName));
            ZipEntry zipEntry;
            byte[] buffer = new byte[BUFFER];//缓冲器
            int readLength;//每次读出来的长度
            ZipFile zipFile = new ZipFile(zipFileName);
            int count = 0;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {//若是zip条目目录，则需创建这个目录
                    File dir = new File(dstPath + File.separator + zipEntry.getName());

                    if (!dir.exists()) {
                        boolean b = dir.mkdirs();
                        if (!b) {
                            return false;
                        }
                        System.out.println("mkdirs: " + dir.getCanonicalPath());
                    }

                    if (callBack != null) {
                        callBack.unzipProgress(++count, zipFile.size());
                    }
                    continue;
                }

                File file = createFile(dstPath, zipEntry.getName());//若是文件，则需创建该文件

                if (file == null) {
                    return false;
                }
                System.out.println("file created: " + file.getCanonicalPath());

                OutputStream outputStream = new FileOutputStream(file);

                while ((readLength = zipInputStream.read(buffer, 0, BUFFER)) != -1) {
                    outputStream.write(buffer, 0, readLength);
                }

                outputStream.close();
                System.out.println("file uncompressed: " + file.getCanonicalPath());
                if (callBack != null) {
                    callBack.unzipProgress(++count, zipFile.size());
                }
            }    // end while
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.out.println("unzip fail!");

            return false;
        }

        System.out.println("unzip success!");

        return true;
    }

    /**
     * 压缩方法
     * （可以压缩空的子目录）
     *
     * @param srcPathList 压缩源路径列表
     * @param zipFileName 目标压缩文件
     * @return boolean
     */
    public static boolean zip(List<String> srcPathList, String zipFileName) {
        System.out.println("zip compressing...");
        List<File> fileList = new ArrayList<>();
        File srcFile;
        for (String srcPath : srcPathList) {
            srcFile = new File(srcPath);
            if (!srcFile.exists()) {
                continue;
            }
            fileList.addAll(getAllFiles(srcFile));//所有要压缩的文件
        }
        byte[] buffer = new byte[BUFFER];//缓冲器
        ZipEntry zipEntry = null;
        int readLength = 0;//每次读出来的长度

        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFileName));

            for (File file : fileList) {
                if (file.isFile()) {//若是文件，则压缩这个文件
                    zipEntry = new ZipEntry(file.getName());
                    zipEntry.setSize(file.length());
                    zipEntry.setTime(file.lastModified());
                    zipOutputStream.putNextEntry(zipEntry);

                    InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

                    while ((readLength = inputStream.read(buffer, 0, BUFFER)) != -1) {
                        zipOutputStream.write(buffer, 0, readLength);
                    }

                    inputStream.close();
                    System.out.println("file compressed: " + file.getCanonicalPath());
                } else {//若是目录（即空目录）则将这个目录写入zip条目
                    zipEntry = new ZipEntry(file.getName() + "/");
                    zipOutputStream.putNextEntry(zipEntry);
                    System.out.println("dir compressed: " + file.getCanonicalPath() + "/");
                }

            }    // end for

            zipOutputStream.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.out.println("zip fail!");

            return false;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.out.println("zip fail!");

            return false;
        }

        System.out.println("zip success!");

        return true;
    }

    public interface ZipCallBack {

        /**
         * 解压进度回调
         * @param progress 进度
         * @param total 总数
         */
        void unzipProgress(int progress, int total);
    }
}