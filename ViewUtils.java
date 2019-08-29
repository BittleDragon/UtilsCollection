package com.xh.common.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.support.design.widget.TabLayout;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.xh.common.widget.DelayContinuousTouchListener;
import com.xh.xhcore.common.third.SystemBarTintManager;

import java.lang.reflect.Field;

/**
 * Created by 想法的猫 on 2017/9/5 0005.
 */

public class ViewUtils {

    /**
     * 设置指示器
     * @param context context
     * @param tabs tabs
     * @param leftDip leftDip
     * @param rightDip rightDip
     */
    public static void setIndicator(Context context, TabLayout tabs, int leftDip, int rightDip) {
        Class<?> tabLayout = tabs.getClass();
        Field tabStrip = null;
        try {
            tabStrip = tabLayout.getDeclaredField("mTabStrip");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        tabStrip.setAccessible(true);
        LinearLayout llTab = null;
        try {
            llTab = (LinearLayout) tabStrip.get(tabs);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        int left = dpToPx(leftDip, context.getResources());
        int right = dpToPx(rightDip, context.getResources());
        for (int i = 0; i < llTab.getChildCount(); i++) {
            View child = llTab.getChildAt(i);
            child.setPadding(0, 0, 0, 0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            params.leftMargin = left;
            params.rightMargin = right;
            child.setLayoutParams(params);
            child.invalidate();
        }
    }

    /**
     * Convert Dp to Pixel
     * @param dp dp
     * @param resources 资源
     * @return px
     */
    public static int dpToPx(float dp, Resources resources) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
        return (int) px;
    }

    /**
     * initSystemBar
     * @param activity activity
     * @param statusBarColor 状态栏颜色
     */
    public static void initSystemBar(Activity activity, int statusBarColor) {
        SystemBarTintManager tintManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = activity.getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams
                    .FLAG_TRANSLUCENT_STATUS);
            if (Build.VERSION.SDK_INT >= 21) {
                fixMStatusBar(activity, statusBarColor);
            }
        }
        // 创建状态栏的管理实例
        tintManager = new SystemBarTintManager(activity);
        // 激活状态栏设置
        tintManager.setStatusBarTintEnabled(true);
        // 激活导航栏设置
        tintManager.setNavigationBarTintEnabled(true);
        // 设置一个颜色给系统栏
        tintManager.setTintColor(statusBarColor);
    }

    /**
     * 设置dialog沉浸
     *
     * @param window         dialog的window
     * @param statusBarColor 颜色
     * @param activity       所在activity
     */
    public static void setTranslucentStatus(Window window, int statusBarColor, Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0 全透明实现
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            View decorView = window.getDecorView();
            int startBarHeight = getStatusBarHeight(activity.getWindow());
            decorView.setPadding(decorView.getPaddingLeft(), decorView.getPaddingTop() + startBarHeight, decorView
                    .getPaddingRight(), decorView.getPaddingBottom());
            decorView.setBackgroundColor(statusBarColor);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(statusBarColor);// SDK21
        } else {//4.4 全透明状态栏
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams
                    .FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * fixMStatusBar
     * @param activity activity
     * @param statusBarColor 颜色
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void fixMStatusBar(Activity activity, int statusBarColor) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        activity.getWindow().setStatusBarColor(statusBarColor);// SDK21
    }

    /**
     * 填充布局
     * @param context context
     * @param viewRes 资源id
     * @return view
     */
    public static View inflate(Context context, int viewRes) {
        return LayoutInflater.from(context).inflate(viewRes, null);
    }

    /**
     * 获取状态栏高度
     * @param window window
     * @return 高度
     */
    public static int getStatusBarHeight(Window window) {
        Rect frame = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        DisplayMetrics dm = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return statusBarHeight;
    }

    /**
     * 自适应tabLayout 切换滚动已经铺满模式
     *
     * @param tabLayout tabLayout
     */
    public static void fixTabLayoutOnChange(TabLayout tabLayout) {
        tabLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (oldLeft == left && oldBottom == bottom) {
                return;
            }
            if (tabLayout.getChildCount() > 0) {
                int totalWidth = 0;
                LinearLayout linearLayout = (LinearLayout) tabLayout.getChildAt(0);
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    View tab = linearLayout.getChildAt(i);
                    int tabWidth = tab.getWidth();
                    totalWidth += tabWidth;
                }
                if (totalWidth < linearLayout.getWidth()) {
                    tabLayout.setTabMode(TabLayout.MODE_FIXED);
                } else {
                    tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
                }
            }
        });

    }

    /**
     * 为所有的textView绑定连续点击事件处理
     * @param viewGroup viewGroup
     */
    public static void bindDelayContinuousClick(ViewGroup viewGroup) {
        if (viewGroup == null) {
            return;
        }
        if (viewGroup.isClickable()) {
            bindDelayContinuousClick((View) viewGroup);
        }
        ViewLooper.loopChildren(viewGroup, ViewUtils::bindDelayContinuousClick);
    }

    /**
     * bindDelayContinuousClick
     * @param child child
     */
    public static void bindDelayContinuousClick(View child) {
        if (child.isClickable()) {
            child.setOnTouchListener(new DelayContinuousTouchListener());
        }
    }

    // 两次点击按钮之间的点击间隔不能少于2000毫秒
    private static final int MIN_CLICK_DELAY_TIME = 2000;
    private static long lastClickTime;

    /**
     * 是否快速点击
     * @return boolean
     */
    public static boolean isFastClick() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if (Math.abs(curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            flag = true;
        }
        lastClickTime = curClickTime;
        return flag;
    }
}
