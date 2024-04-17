package org.yuezhikong.JavaIMAndroid.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
    public static StringBuilder readTxt(File file,Charset charset) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(new FileInputStream(file), charset);
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
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),charset));
        writer.write(Text);
        writer.newLine();
        writer.close();
    }

    @NonNull
    public static String[] fileListOfServerCACerts(@NotNull Context context)
    {
        File ServerCACertsDirectory = new File (context.getFilesDir().getPath()+"/ServerCACerts/");
        if (!(ServerCACertsDirectory.exists()))
        {
            if (!(ServerCACertsDirectory.mkdir()))
            {
                return new String[0];
            }
            fileListOfServerCACerts(context);
        }
        if (ServerCACertsDirectory.isDirectory())
        {
            List<String> returnFileList = new ArrayList<>();
            String[] FileList = ServerCACertsDirectory.list();
            if (FileList == null)
            {
                return new String[0];
            }
            for (String file : FileList)
            {
                File RequestFile = new File(context.getFilesDir().getPath()+"/ServerCACerts/"+file);
                if (RequestFile.exists() && RequestFile.isFile())
                {
                    returnFileList.add(file);
                }
            }
            return returnFileList.toArray(new String[0]);
        }
        else
        {
            if (!(ServerCACertsDirectory.delete()) || !(ServerCACertsDirectory.mkdir()))
            {
                return new String[0];
            }
            return fileListOfServerCACerts(context);
        }
    }
}
