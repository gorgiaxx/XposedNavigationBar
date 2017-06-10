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

package com.egguncle.xposednavigationbar.hook.btnFunc;

import android.content.Context;
import android.view.View;

import com.egguncle.xposednavigationbar.hook.HookUtil;
import com.egguncle.xposednavigationbar.hook.hookFunc.ClearAllNotifications;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by egguncle on 17-6-10.
 */

public class BtnClearAllNotifications implements ClearAllNotifications, View.OnClickListener {
    //用于获取phonestatusbar对象和clearAllNotifications方法
//    private Object mPhoneStatusBar;
//    private Method mClearAllNotificationsMethod;

    private BtnStatusBarController btnStatusBarController;

    //    public BtnClearAllNotifications(Object phoneStatusBar, Method clearAllNotificationsMethod) {
//        mPhoneStatusBar = phoneStatusBar;
//        mClearAllNotificationsMethod = clearAllNotificationsMethod;
//        btnStatusBarController = new BtnStatusBarController();
//    }
    public BtnClearAllNotifications() {
        btnStatusBarController = new BtnStatusBarController();
    }

    @Override
    public void onClick(View view) {
        clearAllNotifications(view.getContext());
    }

    @Override
    public void clearAllNotifications(Context context) {
        Object mPhoneStatusBar = HookUtil.getPhoneStatusBar();
        Method mClearAllNotificationsMethod = HookUtil.getClearAllNotificationsMethod();
        if (mClearAllNotificationsMethod == null || mPhoneStatusBar == null) {
            XposedBridge.log("method or object is null");
            return;
        }
        try {
            //反射取到这个清除所有通知的方法
            mClearAllNotificationsMethod.invoke(mPhoneStatusBar);
            //方法执行后，不会马上清除所有的消息，而是在通知栏下拉，通知内容变得可见后才清除。
            //所以在这里调用一次下拉通知栏的方法
            btnStatusBarController.expandStatusBar(context);
            //再收起通知栏
            btnStatusBarController.collapseStatusBar(context);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
