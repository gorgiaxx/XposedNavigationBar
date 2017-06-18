/*
 *     Navigation bar function expansion module
 *     Copyright (C) 2017 egguncle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.egguncle.xposednavigationbar.hook;


import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import com.egguncle.xposednavigationbar.FinalStr.FuncName;

import com.egguncle.xposednavigationbar.model.AppInfo;
import com.egguncle.xposednavigationbar.model.ShortCut;
import com.egguncle.xposednavigationbar.model.ShortCutData;
import com.egguncle.xposednavigationbar.ui.adapter.MyViewPagerAdapter;
import com.google.gson.Gson;


/**
 * Created by egguncle on 17-6-1.
 * <p>
 * 一个hook模块，为了在android设备的底部导航栏虚拟按键上实现功能扩展
 */

public class HookUtil implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {

    private final static String TAG = "HookUtil";

    public final static String ACT_BROADCAST = "com.egguncle.xpnavbar.broadcast";

    private static final String SHORT_CUT_DATA = "short_cut_data";

    //用于获取phonestatusbar对象和clearAllNotifications方法
    private static Object phoneStatusBar;
    private static Method clearAllNotificationsMethod;

    //用于加载图片资源
    private Map<Integer, byte[]> mapImgRes = new HashMap<>();
    //用于获取保存的快捷按键设置
    private List<ShortCut> shortCutList;
    private int iconScale;

    //hook获得的mediaplayer的实例
    //   private static MediaSession mediaSession;

    //   private static Object mcb;
    //扩展出来的主界面
    // private ViewPager vpXphook;
    private LinearLayout vpLine;

    private String homePointPosition;

    private BtnFuncFactory btnFuncFactory;

    private void initHook(StartupParam startupParam) throws Throwable {
        //读取sp，查看程序是否被允许激活
        XSharedPreferences pre = new XSharedPreferences("com.egguncle.xposednavigationbar", "XposedNavigationBar");
        boolean activation = pre.getBoolean("activation", false);
        String json = pre.getString(SHORT_CUT_DATA, "");
        if ("".equals(json)) {
            return;
        }
        XposedBridge.log("===" + json);


        //获取主导行栏小点的位置
        homePointPosition = pre.getString(FuncName.HOME_POINT, FuncName.LEFT);
        //获取快捷按钮设置数据
        Gson gson = new Gson();
        shortCutList = gson.fromJson(json, ShortCutData.class).getData();
//        for (ShortCut sc : shortCutList) {
//            XposedBridge.log(sc.getName() + " " + sc.getShortCutName() + " " + sc.getPage() + " " + sc.getPostion());
//        }
        //获取图片缩放大小
        iconScale = pre.getInt(FuncName.ICON_SIZE, 100);

        if (activation) {
            //加载图片资源文件
            Resources res = XModuleResources.createInstance(startupParam.modulePath, null);
            byte[] backImg = XposedHelpers.assetAsByteArray(res, "back.png");
            byte[] clearMenImg = XposedHelpers.assetAsByteArray(res, "clear_mem.png");
            byte[] clearNotificationImg = XposedHelpers.assetAsByteArray(res, "clear_notification.png");
            byte[] downImg = XposedHelpers.assetAsByteArray(res, "down.png");
            byte[] lightImg = XposedHelpers.assetAsByteArray(res, "light.png");
            byte[] quickNoticesImg = XposedHelpers.assetAsByteArray(res, "quick_notices.png");
            byte[] screenOffImg = XposedHelpers.assetAsByteArray(res, "screenoff.png");
            //  byte[] upImg = XposedHelpers.assetAsByteArray(res, "up.png");
            byte[] volume = XposedHelpers.assetAsByteArray(res, "volume.png");
            byte[] smallPonit = XposedHelpers.assetAsByteArray(res, "small_point.png");
            byte[] home = XposedHelpers.assetAsByteArray(res, "ic_home.png");
            byte[] startActs = XposedHelpers.assetAsByteArray(res, "start_acts.png");
            byte[] playMusic = XposedHelpers.assetAsByteArray(res, "ic_music.png");
            byte[] pauseMusic = XposedHelpers.assetAsByteArray(res, "ic_pause.png");
            byte[] previousMusic = XposedHelpers.assetAsByteArray(res, "ic_previous.png");
            byte[] nextMusic = XposedHelpers.assetAsByteArray(res, "ic_next.png");
            byte[] scanWeChat=XposedHelpers.assetAsByteArray(res, "ic_scan.png");
            byte[] screenshot=XposedHelpers.assetAsByteArray(res,"ic_image.png");
            mapImgRes.put(FuncName.FUNC_BACK_CODE, backImg);
            mapImgRes.put(FuncName.FUNC_CLEAR_MEM_CODE, clearMenImg);
            mapImgRes.put(FuncName.FUNC_CLEAR_NOTIFICATION_CODE, clearNotificationImg);
            mapImgRes.put(FuncName.FUNC_DOWN_CODE, downImg);
            mapImgRes.put(FuncName.FUNC_LIGHT_CODE, lightImg);
            mapImgRes.put(FuncName.FUNC_QUICK_NOTICE_CODE, quickNoticesImg);
            mapImgRes.put(FuncName.FUNC_SCREEN_OFF_CODE, screenOffImg);
            //  mapImgRes.put(FuncName.UP, upImg);
            mapImgRes.put(FuncName.FUNC_VOLUME_CODE, volume);
            mapImgRes.put(FuncName.FUNC_SMALL_POINT_CODE, smallPonit);
            mapImgRes.put(FuncName.FUNC_HOME_CODE, home);
            mapImgRes.put(FuncName.FUNC_START_ACTS_CODE, startActs);
            mapImgRes.put(FuncName.FUNC_PLAY_MUSIC_CODE, playMusic);
            mapImgRes.put(FuncName.FUNC_NEXT_PLAY_CODE, nextMusic);
            mapImgRes.put(FuncName.FUN_PREVIOUS_PLAY_CODE, previousMusic);
            mapImgRes.put(FuncName.FUNC_WECHAT_SACNNER_CODE, scanWeChat);
            mapImgRes.put(FuncName.FUNC_ALIPAY_SACNNER_CODE, scanWeChat);
            mapImgRes.put(FuncName.FUNC_SCREEN_SHOT_CODE, screenshot);
        }


    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        initHook(startupParam);
    }

    private Bitmap byte2Bitmap(byte[] imgBytes) {
        return BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        XSharedPreferences pre = new XSharedPreferences("com.egguncle.xposednavigationbar", "XposedNavigationBar");
        boolean activation = pre.getBoolean("activation", false);
        if (!activation) {
            return;
        }


        //过滤包名
        if (!resparam.packageName.equals("com.android.systemui"))
            return;

        XposedBridge.log("hook resource ");



        resparam.res.hookLayout(resparam.packageName, "layout", "navigation_bar", new XC_LayoutInflated() {


            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                XposedBridge.log("hook layout");

                LinearLayout rootView = (LinearLayout) liparam.view;
                //  FrameLayout navBarBg = new FrameLayout(rootView.getContext());
                LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                //    rootView.addView(navBarBg,params1);

                //垂直状态下的导航栏整体布局
                XposedBridge.log("hook navBarBg success");
                FrameLayout navBarBg = (FrameLayout) rootView.findViewById(liparam.res.getIdentifier("rot0", "id", "com.android.systemui"));
                //垂直状态下的导航栏三大按钮布局
                final LinearLayout lineBtn = (LinearLayout) rootView.findViewById(liparam.res.getIdentifier("nav_buttons", "id", "com.android.systemui"));

                final Context context = navBarBg.getContext();
                LinearLayout parentView = new LinearLayout(context);
                //加入一个viewpager，第一页为空，是导航栏本身的功能
                final ViewPager vpXphook = new ViewPager(context);

                parentView.addView(vpXphook);
                //   TextView textView1 = new TextView(context);
                //第一个界面，与原本的导航栏重合，实际在导航栏的下层
                LinearLayout linepage1 = new LinearLayout(context);
                if (!homePointPosition.equals(FuncName.DISMISS)) {
                    //用于呼出整个扩展导航栏的一个小点
                    ImageButton btnCall = new ImageButton(context);
                    btnCall.setImageBitmap(byte2Bitmap(mapImgRes.get(FuncName.FUNC_SMALL_POINT_CODE)));
                    btnCall.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    btnCall.setBackgroundColor(Color.alpha(255));
                    LinearLayout.LayoutParams line1Params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    if (homePointPosition.equals(FuncName.LEFT)) {
                        //line1Params.gravity = Gravity.LEFT;
                        linepage1.setGravity(Gravity.LEFT);
                    } else if (homePointPosition.equals(FuncName.RIGHT)) {
                        //  line1Params.gravity = Gravity.RIGHT;
                        linepage1.setGravity(Gravity.RIGHT);
                    }
                    linepage1.addView(btnCall, line1Params);
                    //点击这个按钮，跳转到扩展部分
                    btnCall.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            vpXphook.setCurrentItem(1);
                        }
                    });
                }

                //初始化广播接收器
                initBroadcast(context);


                //viewpage的第二页
                //整个页面的基础
                final FrameLayout framePage2 = new FrameLayout(context);
                framePage2.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        return true;
                    }
                });
                btnFuncFactory = new BtnFuncFactory(iconScale, framePage2, vpXphook, mapImgRes);

                //   LinearLayout
                vpLine = new LinearLayout(context);
                // vpLine.setPadding(0, 0, 0, 0);
                framePage2.addView(vpLine);
                vpLine.setOrientation(LinearLayout.HORIZONTAL);
                vpLine.setGravity(Gravity.CENTER_VERTICAL);

                for (ShortCut sc : shortCutList) {
                    // createBtnAndSetFunc(context, framePage2, vpLine, sc.getShortCutName());
                    btnFuncFactory.createBtnAndSetFunc(context, vpLine, sc.getCode());
                }
                //将这些布局都添加到viewpageadapter中
                List<View> list1 = new ArrayList<View>();
                list1.add(linepage1);
                list1.add(framePage2);

                MyViewPagerAdapter pagerAdapter = new MyViewPagerAdapter(list1);

                vpXphook.setAdapter(pagerAdapter);
                vpXphook.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {


                    }

                    @Override
                    public void onPageSelected(int position) {
                        if (position == 0) {
                            //当移动到第一页的时候，显示出导航栏，上升动画
                            XposedBridge.log("apper NavigationBar");
                            lineBtn.setVisibility(View.VISIBLE);
//                            int navBarHeight = lineBtn.getHeight();
//                            TranslateAnimation animaUp = new TranslateAnimation(0, 0, navBarHeight, 0);
//                            animaUp.setDuration(300);
//                            animaUp.setFillAfter(true);
//                            lineBtn.startAnimation(animaUp);
                        } else {
                            //当移动到非第一页的时候，隐藏导航栏本身的功能，来实现自己的一些功能。
                            XposedBridge.log("hide NavigationBar");
                            lineBtn.setVisibility(View.GONE);
//                            int navBarHeight = lineBtn.getHeight();
//                            TranslateAnimation animDown = new TranslateAnimation(0, 0, 0, navBarHeight);
//                            animDown.setDuration(300);
//                            animDown.setFillAfter(true);
//                            lineBtn.startAnimation(animDown);
//                            new Handler().postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                        lineBtn.setVisibility(View.GONE);
//                                }
//                            },300);
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });

                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                        ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.MATCH_PARENT);
                navBarBg.addView(parentView, 0, params);

            }


        });

    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XSharedPreferences pre = new XSharedPreferences("com.egguncle.xposednavigationbar", "XposedNavigationBar");
        boolean activation = pre.getBoolean("activation", false);
        // XposedBridge.log(lpparam.packageName);
        if (!activation) {
            return;
        }


        //过滤包名
        if (lpparam.packageName.equals("com.android.systemui")) {
            XposedBridge.log("filter package");
            //获取清除通知的方法
            Class<?> phoneStatusBarClass =
                    lpparam.classLoader.loadClass("com.android.systemui.statusbar.phone.PhoneStatusBar");
            Method method1 = phoneStatusBarClass.getDeclaredMethod("clearAllNotifications");
            method1.setAccessible(true);

            //获取到clearAllNotifications方法
            clearAllNotificationsMethod = method1;
            //   XposedBridge.log("====hook PhoneStatusBar success====");
            //       phoneStatusBar=XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBar",lpparam.classLoader);
            XposedHelpers.findAndHookMethod(phoneStatusBarClass,
                    "start", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            //在这里获取到PhoneStatusBar对象
                            phoneStatusBar = param.thisObject;
                            //     XposedBridge.log("====hook clear notifications success====");
                        }
                    });

//            Class<?> navigationbarClass=lpparam.classLoader.loadClass("com.android.systemui.statusbar.phone.NavigationBarView");
//            XposedHelpers.findAndHookConstructor(navigationbarClass, Context.class, AttributeSet.class, new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    super.afterHookedMethod(param);
//                    XposedBridge.log("hook navigationbarClass success");
//                }
//            });

        } else if (lpparam.packageName.equals("android.hardware.input")) {
//            XposedBridge.log("hook WindowManagerService success");
//            Class<?> phoneWindowManagerClass = lpparam.classLoader.loadClass("android.hardware.input.InputManager");
//            XposedHelpers.findAndHookConstructor(phoneWindowManagerClass, new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    super.afterHookedMethod(param);
//                    XposedBridge.log("hook WindowManagerService constructor success");
//
//                }
//            });

        }

    }


    public static Object getPhoneStatusBar() {
        return phoneStatusBar;
    }

    public static Method getClearAllNotificationsMethod() {
        return clearAllNotificationsMethod;
    }

    /**
     * 初始化广播，用于进程间通信
     *
     * @param context
     */
    private void initBroadcast(Context context) {
        ;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACT_BROADCAST);
        MyReceiver myReceiver = new HookUtil.MyReceiver();
        context.registerReceiver(myReceiver, intentFilter);
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Integer> scCodes = intent.getIntegerArrayListExtra("data");
            btnFuncFactory.clearAllBtn(vpLine);
            if (scCodes != null && scCodes.size() != 0) {
                for (int code : scCodes) {
                    btnFuncFactory.createBtnAndSetFunc(context, vpLine, code);
                }
            }


        }
    }

}

