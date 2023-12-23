package org.yuezhikong.JavaIMAndroid.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.yuezhikong.JavaIMAndroid.R;

public class CardServer extends Fragment {
    private String ServerName;
    private String ServerAddr;
    private int port;
    private int top = 0;
    public void setTop(int i) {
        for (;i > 0; i--) {
            top += 300 * i;
        }
    }
    public void setServerName(String ServerName) {this.ServerName = ServerName;}
    public void setServerPort(int ServerPort) {
        this.port = ServerPort;
    }
    public void setServerAddr(String ServerAddr) {this.ServerAddr = ServerAddr;}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.card_server, container, false);
        TextView textview = view.findViewById(R.id.ServerName);
        textview.setText(ServerName);
        textview = view.findViewById(R.id.Address);
        textview.setText(ServerAddr + ":" + port);
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        marginLayoutParams.setMargins(marginLayoutParams.leftMargin, top, marginLayoutParams.rightMargin, marginLayoutParams.bottomMargin);
        view.requestLayout();
        return view;
    }
}
