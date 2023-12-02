package org.yuezhikong.utils;

import android.os.Build;

import org.yuezhikong.JavaIMAndroid.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 跨平台文件io封装
 * Android版依赖于此文件修改io调用方式，请勿删除此文件!
 */
public class FileIO {
    /**
     * 删除文件或文件夹
     * @param file 文件
     * @throws IOException 发生IO错误
     */
    public static void DeleteFile(File file) throws IOException {
        if (Build.VERSION.SDK_INT >= 26) {//大于Android 8.0时，采用Files
            Files.delete(file.toPath());
        } else {
            if (!file.delete())
            {
                throw new IOException("Delete File Failed");
            }
        }
    }

    /**
     * 创建文件
     * @param file 文件
     * @throws IOException 发生IO错误
     */
    public static void CreateFile(File file) throws IOException {
        if (Build.VERSION.SDK_INT >= 26) {//大于Android 8.0时，采用Files
            Files.createFile(file.toPath());
        } else {
            if (!file.mkdir())
            {
                throw new IOException("Create File Failed");
            }
        }
    }

    /**
     * 创建文件夹
     * @param file 文件
     * @throws IOException 发生IO错误
     */
    public static void CreateDirectory(File file) throws IOException {
        if (Build.VERSION.SDK_INT >= 26) {//大于Android 8.0时，采用Files
            Files.createDirectories(file.toPath());
        } else {
            if (!file.mkdir())
            {
                throw new IOException("Create Directory Failed");
            }
        }
    }

    /**
     * 从文件读取信息
     * @param file 文件
     * @return 读取到的信息
     * @throws IOException 发生IO错误
     */
    public static String readFileToString(File file) throws IOException {
        return readFileToString(file, StandardCharsets.UTF_8);
    }

    /**
     * 从文件读取信息
     * @param file 文件
     * @param charset 使用的字符集
     * @return 读取到的信息
     * @throws IOException 发生IO错误
     */
    public static String readFileToString(File file, Charset charset) throws IOException {
        return FileUtils.readTxt(file,charset).toString();
    }

    /**
     * 将数据写入到文件
     * @param file 文件
     * @param data 数据
     * @throws IOException 发生IO错误
     */
    public static void writeStringToFile(File file, String data) throws IOException
    {
        writeStringToFile(file,data,StandardCharsets.UTF_8);
    }

    /**
     * 将数据写入到文件
     * @param file 文件
     * @param data 数据
     * @param charset 使用的字符集
     * @throws IOException 发生IO错误
     */
    public static void writeStringToFile(File file, String data, Charset charset) throws IOException
    {
        FileUtils.writeTxt(file, data, charset);
    }
    private FileIO() {}
}
