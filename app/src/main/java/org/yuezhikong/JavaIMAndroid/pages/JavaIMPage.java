package org.yuezhikong.JavaIMAndroid.pages;

import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * 本类设计为用于定义JavaIM主页页面的共享函数，如加载是否完毕
 */
public class JavaIMPage extends Fragment {
    private boolean Started = false;

    public boolean isStarted() {
        return Started;
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        Started = true;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Started = false;
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Started = false;
        super.onDetach();
    }
}
