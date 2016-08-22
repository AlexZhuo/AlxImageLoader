package vc.zz.qduxsh.alximageloader;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

/**
 * Created by Administrator on 2016/8/22.
 */
public class AlxPermissionHelper {
    private AskPermissionCallBack callBack = null;

    public void checkPermission(final Activity activity, final AskPermissionCallBack callBack){
        this.callBack = callBack;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //没有权限，申请权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //以前被拒绝授予权限，而且不再提示
                Log.i("Alex","以前被拒绝授予权限，而且不再提示");
                new AlertDialog.Builder(activity)
                        .setMessage("app需要开启权限才能使用此功能")
                        .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                                activity.startActivity(intent);
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(callBack != null)callBack.onFailed();
                            }
                        })
                        .create()
                        .show();
            } else {
                //申请权限
                Log.i("Alex","准备首次申请权限");
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2000);
            }
        } else {
            //已经拥有权限
            if(callBack != null)callBack.onSuccess();
        }
    }

    interface AskPermissionCallBack{
        void onSuccess();
        void onFailed();
    }

    /**
     * 在Activity的onRequestPermissionsResult中进行回调
     */
    public void registActivityResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        Log.i("Alex","请求权限完成 requestCode="+ requestCode+"   permissions="+permissions+"    results="+grantResults);
        int i = 0;
        for(String s:permissions){
            Log.i("Alex","权限-"+s+"获取情况::"+grantResults[i]);
            i++;
        }

    }
}
