package org.yuezhikong.JavaIMAndroid.utils;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FileUtils {
    @NonNull
    public static StringBuilder readTxt(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        File urlFile = new File(path);
        InputStreamReader isr = new InputStreamReader(new FileInputStream(urlFile), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String mimeTypeLine ;
        sb.delete(0,sb.length());
        while ((mimeTypeLine = br.readLine()) != null) {
            sb.append(mimeTypeLine).append("\n");
        }
        br.close();
        isr.close();
        return sb;
    }
    @NonNull
    @Contract(pure = true)
    public static String readTxt(File file,Charset charset) throws IOException {
        return readTxt(new FileInputStream(file), charset);
    }

    public static String readTxt(InputStream is, Charset charset) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(is, charset);
        BufferedReader br = new BufferedReader(isr);
        String mimeTypeLine;
        sb.delete(0,sb.length());
        while ((mimeTypeLine = br.readLine()) != null) {
            sb.append(mimeTypeLine).append("\n");
        }
        br.close();
        isr.close();
        return sb.toString();
    }

    @NonNull
    public static String readTxt(File file) throws IOException {
        return readTxt(file,StandardCharsets.UTF_8);
    }
    public static void writeTxt(String path,String Text) throws IOException {
        writeTxt(new File(path),Text);
    }

    private static void CreateDirectory(File dir)
    {
        if (dir.exists() && dir.isDirectory())
            return;
        if (dir.exists() && dir.isFile())
            dir.delete();
        dir.mkdirs();
    }
    public static void writeTxt(File file, String Text) throws IOException {
        writeTxt(file,Text,StandardCharsets.UTF_8);
    }
    public static void writeTxt(File file, String Text, Charset charset) throws IOException {
        if (!(file.exists() && file.isFile()))
        {
            file.delete();
            if (!(file.getParentFile() != null && file.getParentFile().exists()))
                CreateDirectory(file.getParentFile());
            file.createNewFile();
        }
        writeTxt(new FileOutputStream(file),Text,charset);
    }

    public static void writeTxt(OutputStream is, String Text, Charset charset) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(is,charset));
        writer.write(Text);
        writer.newLine();
        writer.close();
    }
}
