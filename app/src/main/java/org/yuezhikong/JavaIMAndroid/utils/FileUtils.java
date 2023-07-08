package org.yuezhikong.JavaIMAndroid.utils;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
    public static StringBuilder readTxt(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
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
    public static void writeTxt(String path,String Text) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path),StandardCharsets.UTF_8));
        writer.write(Text);
        writer.newLine();
        writer.close();
    }
    public static void writeTxt(File file,String Text) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));
        writer.write(Text);
        writer.newLine();
        writer.close();
    }
}
