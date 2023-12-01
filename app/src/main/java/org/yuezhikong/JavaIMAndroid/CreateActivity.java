package org.yuezhikong.JavaIMAndroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import androidx.appcompat.widget.Toolbar;

import org.yuezhikong.JavaIMAndroid.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.UUID;

public class CreateActivity extends AppCompatActivity {
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
            String[] FileNames = new String[0];
            if (bundle != null) {
                FileNames = bundle.getStringArray("FileNames");
            }
            final Spinner FileNameSpinner = findViewById(R.id.spinner);
            if (FileNames != null) {
                FileNameSpinner.setAdapter(new ArrayAdapter<CharSequence>(this,
                        android.R.layout.simple_spinner_dropdown_item, FileNames));
            }
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
            final Spinner FileControlModeSpinner = findViewById(R.id.spinner2);
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
            final Button ApplyChange = findViewById(R.id.button7);
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
                Toast.makeText(CreateActivity.File_Control_Activity.this,"文件名不能为空",Toast.LENGTH_LONG).show();
                return;
            }
            File file = new File(getFilesDir().getPath()+"/ServerPublicKey/"+SelectedFileName);
            if (MainActivity.UsedKey != null && file.getName().equals(MainActivity.UsedKey.getName()) && MainActivity.isSession())
            {
                Toast.makeText(CreateActivity.File_Control_Activity.this,"此文件正在使用中",Toast.LENGTH_LONG).show();
                return;
            }
            if (getResources().getString(R.string.RenameText).equals(FileControlMode))
            {
                if ("".equals(((EditText)findViewById(R.id.RenameFileText)).getText().toString()))
                {
                    Toast.makeText(CreateActivity.File_Control_Activity.this,"文件名不能为空",Toast.LENGTH_LONG).show();
                    return;
                }
                for (String Filename : FileUtils.fileListOfServerPublicKey(this))
                {
                    //判断新名称是否与现有名称重复
                    if (new File(getFilesDir().getPath()+"/ServerPublicKey/"+((EditText)findViewById(R.id.RenameFileText)).getText().toString())
                            .getName().equals(new File(getFilesDir().getPath()+"/ServerPublicKey/"+Filename).getName()))
                    {
                        Toast.makeText(CreateActivity.File_Control_Activity.this,"文件名重复",Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                if (!file.renameTo(new File(getFilesDir().getPath()+"/ServerPublicKey/"
                        +((EditText)findViewById(R.id.RenameFileText)).getText().toString())))
                {
                    Toast.makeText(CreateActivity.File_Control_Activity.this,"文件重命名失败",Toast.LENGTH_LONG).show();
                }
                else if (file.getPath().equals(MainActivity.UsedKey.getPath()))
                {
                    MainActivity.UsedKey = new File(getFilesDir().getPath()+"/ServerPublicKey/"
                            +((EditText)findViewById(R.id.RenameFileText)).getText().toString());
                }
            }
            else if (getResources().getString(R.string.DeleteFileText).equals(FileControlMode))
            {
                if (!file.delete())
                {
                    Toast.makeText(CreateActivity.File_Control_Activity.this,"文件删除失败",Toast.LENGTH_LONG).show();
                }
                //如果删除成功且删除的公钥是正在使用的公钥
                else if (file.getPath().equals(MainActivity.UsedKey.getPath()))
                {
                    //剩余公钥数量大于0
                    if (FileUtils.fileListOfServerPublicKey(this).length > 0)
                    {
                        //随机一个新公钥
                        MainActivity.UsedKey = new File(getFilesDir().getPath()+"/ServerPublicKey/"+(FileUtils.fileListOfServerPublicKey(this))[0]);
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
    public void OnImportPublicKey(View v)
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        StorageAccessFrameworkResultLauncher.launch(intent);
    }
    public void OnSaveChange() {
        //开始获取新ServerAddr和新ServerPort
        final EditText AddrEdit = findViewById(R.id.SettingIPAddress);
        final EditText PortEdit = findViewById(R.id.SettingIPPort);
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
    private ActivityResultLauncher<Intent> StorageAccessFrameworkResultLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_activity);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(this::OnImportPublicKey);

        ((EditText) findViewById(R.id.SettingIPAddress)).setText(MainActivity.ServerAddr);
        if (MainActivity.ServerPort == 0){
            ((EditText) findViewById(R.id.SettingIPPort)).setText("");
        }
        else {
            ((EditText) findViewById(R.id.SettingIPPort)).setText(String.valueOf(MainActivity.ServerPort));
        }

        StorageAccessFrameworkResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK) {
                return;
            }
            Uri FileURI = null;
            if (result.getData() != null) {
                FileURI = result.getData().getData();
            }
            if (FileURI == null)
                return;
            Log.d(LogHead, "获取到的URI是：" + FileURI);
            String DisplayName = GetURIDisplayName(FileURI);
            if (DisplayName == null) {
                DisplayName = "RandomKeyName" + UUID.randomUUID() + UUID.randomUUID() + ".txt";
            }
            final Uri finalFileURI = FileURI;
            Toast.makeText(CreateActivity.this, "文件名为：" + DisplayName, Toast.LENGTH_LONG).show();
            if (!(new File(getFilesDir().getPath()+"/ServerPublicKey").exists()))
            {
                if (!(new File(getFilesDir().getPath()+"/ServerPublicKey").mkdir()))
                {
                    Toast.makeText(this,"无法成功创建文件夹",Toast.LENGTH_LONG).show();
                    return;
                }
            }
            File ServerPublicKey = new File(getFilesDir().getPath()+"/ServerPublicKey/"+DisplayName);
            try {
                if (!(ServerPublicKey.createNewFile()))
                {
                    Toast.makeText(this,"无法成功创建文件",Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this,"无法成功创建文件",Toast.LENGTH_LONG).show();
                return;
            }
            Application.getInstance().getIOThreadPool().execute(() -> {
                try (BufferedReader FileInput = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(finalFileURI))); BufferedWriter FileOutput = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ServerPublicKey)))) {
                    String line;
                    while ((line = FileInput.readLine()) != null) {
                        FileOutput.write(line);
                        FileOutput.newLine();//line是纯文本，没有回车，需要补上
                        FileOutput.flush();
                    }
                    //写入完毕，将此文件设为使用
                    MainActivity.UsedKey = ServerPublicKey;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        else if (id == R.id.action_save)
            OnSaveChange();
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_main, menu);
        return true;
    }
}