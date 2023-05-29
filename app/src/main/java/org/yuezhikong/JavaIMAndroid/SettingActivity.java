package org.yuezhikong.JavaIMAndroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.UUID;


public class SettingActivity extends AppCompatActivity {
    private final String LogHead = "JavaIM";
    public static class File_Control_Activity extends AppCompatActivity {
        private String SelectedFileName = "";
        private String FileControlMode = "";
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            //Super类调用
            super.onCreate(savedInstanceState);
            //设置显示界面
            setContentView(R.layout.control_file_activity);
            //开始读取bundle，执行填充
            Bundle bundle = this.getIntent().getExtras();
            String[] FileNames = bundle.getStringArray("FileNames");
            Spinner FileNameSpinner = findViewById(R.id.spinner);
            FileNameSpinner.setAdapter(new ArrayAdapter<CharSequence>(this,
                    android.R.layout.simple_spinner_dropdown_item, FileNames));
            FileNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    SelectedFileName = adapterView.getItemAtPosition(i).toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            //FileNameSpinner注册完成
            Spinner FileControlModeSpinner = findViewById(R.id.spinner2);
            FileControlModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    FileControlMode = adapterView.getItemAtPosition(i).toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            FileControlMode = (getResources().getStringArray(R.array.control_mode_array))[0];
            //FileControlModeSpinner注册完成
            Button ApplyChange = findViewById(R.id.button7);
            ApplyChange.setOnClickListener(v -> {
                onApplyChange();
                this.finish();
            });
            //ApplyChangeButton注册完成
        }
        private void onApplyChange()
        {
            if ("".equals(SelectedFileName))
            {
                Toast.makeText(File_Control_Activity.this,"文件名不能为空",Toast.LENGTH_LONG).show();
                return;
            }
            File file = new File(getFilesDir().getPath()+"/"+SelectedFileName);
            if (MainActivity.UsedKey != null && file.getName().equals(MainActivity.UsedKey.getName()) && MainActivity.isSession())
            {
                Toast.makeText(File_Control_Activity.this,"此文件正在使用中",Toast.LENGTH_LONG).show();
                return;
            }
            if (getResources().getString(R.string.RenameText).equals(FileControlMode))
            {
                for (String Filename : fileList())
                {
                    if (new File(getFilesDir().getPath()+"/"+((EditText)findViewById(R.id.RenameFileText)).getText().toString()).getName().equals(new File(getFilesDir().getPath()+"/"+Filename).getName()))
                    {
                        Toast.makeText(File_Control_Activity.this,"文件名重复",Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                if (!file.renameTo(new File(getFilesDir().getPath()+"/"+((EditText)findViewById(R.id.RenameFileText)).getText().toString())))
                {
                    Toast.makeText(File_Control_Activity.this,"文件重命名失败",Toast.LENGTH_LONG).show();
                }
                else if (file.getPath().equals(MainActivity.UsedKey.getPath()))
                {
                    MainActivity.UsedKey = new File(getFilesDir().getPath()+"/"+((EditText)findViewById(R.id.RenameFileText)).getText().toString());
                }
            }
            else if (getResources().getString(R.string.DeleteFileText).equals(FileControlMode))
            {
                if (!file.delete())
                {
                    Toast.makeText(File_Control_Activity.this,"文件删除失败",Toast.LENGTH_LONG).show();
                }
                else if (file.getPath().equals(MainActivity.UsedKey.getPath()))
                {
                    if (fileList().length > 0)
                    {
                        MainActivity.UsedKey = new File(getFilesDir().getPath()+"/"+(fileList())[0]);
                    }
                    else
                    {
                        MainActivity.UsedKey = null;
                    }
                }
            }
            else if (getResources().getString(R.string.SetUsedKeyText).equals(FileControlMode))
            {
                MainActivity.UsedKey = file;
            }
        }
    }
    private ActivityResultLauncher<Intent> StorageAccessFrameworkResultLauncher;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //Super类调用
        super.onCreate(savedInstanceState);
        //设置显示界面
        setContentView(R.layout.setting_activity);
        //开始读取bundle，进行第一次填充
        Bundle bundle = this.getIntent().getExtras();
        int ServerPort = bundle.getInt("ServerPort");
        String ServerAddr = bundle.getString("ServerAddr");
        //进行第一次填充
        EditText AddrEdit = findViewById(R.id.SettingIPAddress);
        EditText PortEdit = findViewById(R.id.SettingIPPort);
        AddrEdit.setText(ServerAddr);
        if (ServerPort != 0) {
            PortEdit.setText(String.format(Locale.getDefault(),"%d", ServerPort));
        }
        //填充完成
        //正在处理注册
        Button button = findViewById(R.id.button5);
        button.setOnClickListener(this::OnSaveChange);
        //完成1/4（保存与退出注册）
        button = findViewById(R.id.button9);
        button.setOnClickListener(this::OnImportPublicKey);
        //完成2/4（导入服务端公钥注册）
        button = findViewById(R.id.button10);
        button.setOnClickListener(this::OnManagePublicKey);
        //完成3/4（服务端公钥管理器注册）
        StorageAccessFrameworkResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK) {
                return;
            }
            {
                Uri FileURI = null;
                if (result.getData() != null) {
                    FileURI = result.getData().getData();
                }
                Log.d(LogHead, "获取到的URI是：" + FileURI);
                String DisplayName = GetURIDisplayName(FileURI);
                if (DisplayName == null) {
                    DisplayName = "RandomKeyName" + UUID.randomUUID() + UUID.randomUUID() + ".txt";
                }
                Uri finalFileURI = FileURI;
                String finalDisplayName = DisplayName;
                Toast.makeText(SettingActivity.this, "文件名为：" + DisplayName, Toast.LENGTH_LONG).show();
                new Thread() {
                    @Override
                    public void run() {
                        this.setName("I/O Thread");
                        try (BufferedReader FileInput = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(finalFileURI))); BufferedWriter FileOutput = new BufferedWriter(new OutputStreamWriter(openFileOutput(finalDisplayName, MODE_PRIVATE)))) {
                            String line;
                            while ((line = FileInput.readLine()) != null) {
                                FileOutput.write(line);
                                FileOutput.newLine();//line是纯文本，没有回车，需要补上
                            }
                            //写入完毕，将此文件设为使用
                            MainActivity.UsedKey = new File(getFilesDir().getPath() + "/" + finalDisplayName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
        // 开始注册StorageAccessFrameworkResultLauncher
        ///注册完成
    }
    public void OnSaveChange(View view) {
        //开始获取新ServerAddr和新ServerPort
        EditText AddrEdit = findViewById(R.id.SettingIPAddress);
        EditText PortEdit = findViewById(R.id.SettingIPPort);
        //开始向bundle写入用户的新ServerAddr和新ServerPort
        MainActivity.ServerAddr = AddrEdit.getText().toString();
        try {
            MainActivity.ServerPort = Integer.parseInt(PortEdit.getText().toString());
        } catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
        //退出此Activity
        this.finish();
    }
    public void OnImportPublicKey(View v)
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        StorageAccessFrameworkResultLauncher.launch(intent);
    }
    private String GetURIDisplayName(Uri uri)
    {
        try (Cursor cursor = getContentResolver()
                .query(uri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {

                try {
                    String displayName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    Log.i(LogHead, "Display Name: " + displayName);
                    return displayName;
                } catch (IllegalArgumentException e) {
                    new AlertDialog.Builder(this)
                            .setTitle("由于操作系统错误")
                            .setIcon(R.mipmap.ic_launcher_round)
                            .setMessage("无法成功获取到文件名，正在使用随机文件名")
                            .setPositiveButton("我知道了", (dialog, which) -> Log.d(LogHead, "已完成对用户的提示"))
                            .create()
                            .show();
                    return null;
                }
            }
        }
        return null;
    }

    public void OnManagePublicKey(View v)
    {
        //开始创建新Activity过程
        Intent intent=new Intent();
        intent.setClass(SettingActivity.this, File_Control_Activity.class);
        //开始向新Activity发送文件列表，以便填充到编辑框
        Bundle bundle = new Bundle();
        bundle.putStringArray("FileNames",fileList());
        //从Bundle put到intent
        intent.putExtras(bundle);
        //设置 如果这个activity已经启动了，就不产生新的activity，而只是把这个activity实例加到栈顶
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //启动Activity
        startActivity(intent);
    }
}
