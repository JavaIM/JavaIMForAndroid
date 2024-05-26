package org.yuezhikong.JavaIMAndroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import org.yuezhikong.JavaIMAndroid.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class SettingFragment extends Fragment {
    private final String LogHead = "JavaIM Setting";

    private String SAFWriteData = "";

    private addNewServerDialogContent addNewServerDialogContentInstance = null;

    private static class addNewServerDialogContent extends ConstraintLayout {
        private final EditText ServerName;
        private final EditText IPAddress;
        private final EditText Port;

        public addNewServerDialogContent(Context context, LayoutInflater inflater, ActivityResultLauncher<Intent> CACertSelectSAFResult) {
            super(context);
            inflater.inflate(R.layout.server_create_dialog_content, this, true);
            IPAddress = findViewById(R.id.IPAddress);
            Port = findViewById(R.id.Port);
            ServerName = findViewById(R.id.ServerNameInput);

            findViewById(R.id.ImportCert).setOnClickListener((view) -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/x-x509-ca-cert");
                CACertSelectSAFResult.launch(intent);
            });
        }

        public String getServerName() {
            return ServerName.getText().toString();
        }

        public String getIPAddress() {
            return IPAddress.getText().toString();
        }

        public String getPort() {
            return Port.getText().toString();
        }

        private String CaCert = "";

        public void setCACertData(String caCertData) {
            CaCert = caCertData;
        }

        public String getCaCertData() {
            return CaCert;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    private ActivityResultLauncher<Intent> CACertSelectSAFResult;
    private ActivityResultLauncher<Intent> OnSAFWriteDataPermissionRequestSuccess;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 注册SAF Activity Result Launcher

        // CA证书导入 SAF
        CACertSelectSAFResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK) {
                Log.i(LogHead, "Can not select CA Cert, cancel");
                return;
            }

            Uri FileURI;
            if (result.getData() != null) {
                FileURI = result.getData().getData();
            } else
                return;
            if (FileURI == null)
                return;

            try {
                addNewServerDialogContentInstance.setCACertData(FileUtils.readTxt(requireActivity().getContentResolver().openInputStream(FileURI), StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 写入数据到文件 SAF
        OnSAFWriteDataPermissionRequestSuccess = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK) {
                Log.i(LogHead, "Can not request write permission, cancel");
                return;
            }

            Uri FileURI;
            if (result.getData() != null) {
                FileURI = result.getData().getData();
            } else
                return;
            if (FileURI == null)
                return;

            try {
                FileUtils.writeTxt(requireActivity().getContentResolver().openOutputStream(FileURI), SAFWriteData , StandardCharsets.UTF_8);
                SAFWriteData = "";
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // 刷新Spinner
        RefreshServerSelecter();
        Spinner ActionSelecter = requireActivity().findViewById(R.id.SelectControlMode);
        ActionSelecter.setAdapter(new ArrayAdapter<CharSequence>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, new String[] {
                "删除服务器",
                "导出 CA 根证书",
                "清除 Token",
                "设为使用中的服务器"
        }));

        // 添加新服务器按钮功能注册
        requireActivity().findViewById(R.id.addNewServer).setOnClickListener(v -> {
            if (addNewServerDialogContentInstance != null)
                return;
            addNewServerDialogContentInstance = new addNewServerDialogContent(requireActivity(), getLayoutInflater(), CACertSelectSAFResult);
            new AlertDialog.Builder(requireActivity())
                    .setTitle("添加服务器")
                    .setView(addNewServerDialogContentInstance)
                    .setCancelable(false)
                    .setPositiveButton("确定", (dialog, which) -> {
                        addNewServerDialogContent content = addNewServerDialogContentInstance;
                        String CACertData = content.getCaCertData();
                        String ServerName = content.getServerName();
                        String IPAddress = content.getIPAddress();

                        // 无效情况检测
                        if (content.getPort().isEmpty()) {
                            Toast.makeText(requireActivity(), "一些参数似乎无效，请您检查", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int Port = Integer.parseInt(content.getPort());
                        // 无效情况检测
                        if (CACertData.isEmpty() || ServerName.isEmpty() || IPAddress.isEmpty() || Port <= 0 || Port > 65535) {
                            Toast.makeText(requireActivity(), "一些参数似乎无效，请您检查", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 准备写入
                        File ServerList = new File(requireActivity().getFilesDir(),"servers.json");
                        Gson gson = new Gson();

                        // 准备内容
                        SavedServer.Server newServer = new SavedServer.Server();
                        newServer.setServerName(ServerName);
                        newServer.setX509CertContent(CACertData);
                        newServer.setServerLoginToken("");
                        newServer.setServerAddress(IPAddress);
                        newServer.setServerPort(Port);
                        newServer.setIsUsingServer(true);

                        // 开始写入
                        try {
                            // 如果保存的服务端文件不存在，创建文件后写入
                            if (!ServerList.exists()) {
                                if (ServerList.createNewFile())
                                {
                                    SavedServer savedServer = new SavedServer();
                                    savedServer.setServers(new ArrayList<>());
                                    savedServer.getServers().add(newServer);
                                    FileUtils.writeTxt(ServerList, gson.toJson(savedServer), StandardCharsets.UTF_8);
                                }
                                else
                                    throw new RuntimeException("Create File Failed!");
                            }
                            // 如果已存在，获取文件信息后写入
                            else {
                                String SavedServerContent = FileUtils.readTxt(ServerList, StandardCharsets.UTF_8);
                                SavedServer savedServer = gson.fromJson(SavedServerContent, SavedServer.class);
                                for (SavedServer.Server server : savedServer.getServers())
                                {
                                    if (newServer.getServerName().equals(server.getServerName()))
                                    {
                                        Toast.makeText(requireActivity(), "已经有重名服务器添加过了!", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (server.isIsUsingServer())
                                    {
                                        server.setIsUsingServer(false);
                                    }
                                }
                                savedServer.getServers().add(newServer);
                                FileUtils.writeTxt(ServerList, gson.toJson(savedServer), StandardCharsets.UTF_8);
                            }

                            HomeFragment.UseServer = newServer;
                        } catch (Throwable t) {
                            t.printStackTrace();
                            Toast.makeText(requireActivity(), "写入文件时出错", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(requireActivity(), "导入成功!", Toast.LENGTH_SHORT).show();
                        RefreshServerSelecter();
                    })
                    .setOnDismissListener((dialog) -> addNewServerDialogContentInstance = null)
                    .setNegativeButton("取消添加", (dialog, which) -> dialog.dismiss())
                    .show();
        });
        // 添加执行操作按钮功能注册
        requireActivity().findViewById(R.id.ExecuteServerControl).setOnClickListener(v -> {
            // 读取用户选择
            String SelectAction = (String) ActionSelecter.getSelectedItem();
            String SelectServerName = (String) ((Spinner) requireActivity().findViewById(R.id.SelectServer)).getSelectedItem();

            // 读取文件数据
            SavedServer.Server SelectServer = null;
            SavedServer savedServers;
            Gson gson = new Gson();
            File servers = new File(requireActivity().getFilesDir(),"servers.json");

            try {
                savedServers = gson.fromJson(FileUtils.readTxt(servers, StandardCharsets.UTF_8), SavedServer.class);
                for (SavedServer.Server server : savedServers.getServers())
                {
                    if (server.getServerName().equals(SelectServerName)) {
                        SelectServer = server;
                        break;
                    }
                }
                if (SelectServer == null)
                    return;
            } catch (Throwable t) {
                t.printStackTrace();
                return;
            }

            if (SelectServer.isIsUsingServer())
                HomeFragment.UseServer = SelectServer;

            // 执行操作
            switch (SelectAction)
            {
                case "删除服务器": {
                    savedServers.getServers().remove(SelectServer);
                    if (SelectServer.isIsUsingServer())
                    {
                        HomeFragment.UseServer = null;
                    }
                    break;
                }

                case "导出 CA 根证书": {
                    SAFWriteData = SelectServer.getX509CertContent();

                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/x-x509-ca-cert");
                    intent.putExtra(Intent.EXTRA_TITLE, "CACert.crt");
                    OnSAFWriteDataPermissionRequestSuccess.launch(intent);
                    break;
                }

                case "清除 Token" : {
                    SelectServer.setServerLoginToken("");
                    break;
                }

                case "设为使用中的服务器" : {
                    for (SavedServer.Server server : savedServers.getServers())
                    {
                        server.setIsUsingServer(false);
                    }
                    SelectServer.setIsUsingServer(true);
                    HomeFragment.UseServer = SelectServer;
                    break;
                }

                default: {
                    Toast.makeText(requireActivity(), "未知的操作!", Toast.LENGTH_SHORT).show();
                    break;
                }
            }

            // 写入
            try {
                FileUtils.writeTxt(servers, gson.toJson(savedServers), StandardCharsets.UTF_8);
            } catch (Throwable t)
            {
                t.printStackTrace();
            }
            RefreshServerSelecter();
        });
        // 注册完成
    }

    private void RefreshServerSelecter() {
        if (getActivity() == null)
            return;
        // 获取服务器列表
        List<String> ServerNames = new ArrayList<>();
        try {
            File servers = new File(requireActivity().getFilesDir(),"servers.json");
            if (!servers.exists())
                return;
            if (servers.length() == 0)
            {
                servers.delete();
                return;
            }
            SavedServer savedServers = new Gson().fromJson(FileUtils.readTxt(servers, StandardCharsets.UTF_8), SavedServer.class);
            for (SavedServer.Server server : savedServers.getServers())
            {
                ServerNames.add(server.getServerName());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // 获取服务器选择框
        Spinner servers = requireActivity().findViewById(R.id.SelectServer);
        servers.setAdapter(new ArrayAdapter<CharSequence>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, ServerNames.toArray(new String[0])));
    }
}