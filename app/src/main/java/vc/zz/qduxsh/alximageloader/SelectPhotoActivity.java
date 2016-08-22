package vc.zz.qduxsh.alximageloader;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class SelectPhotoActivity extends AppCompatActivity implements SelectPhotoAdapter.CallBackActivity,View.OnClickListener{
    SelectPhotoAdapter allPhotoAdapter =  null;
    TextView tv_done = null;
    AlxPermissionHelper permissionHelper = new AlxPermissionHelper();
    ArrayList<SelectPhotoAdapter.SelectPhotoEntity> selectedPhotoList = null;
    public static final int SELECT_PHOTO_OK = 20;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File file1 = null;//拍照得到的file
        setContentView(R.layout.activity_select_photo);
        GridView gvPhotoList = (GridView) findViewById(R.id.gv_photo);
        ArrayList<SelectPhotoAdapter.SelectPhotoEntity> allPhotoList = new ArrayList<>();
        allPhotoAdapter = new SelectPhotoAdapter(this, allPhotoList,file1);
        gvPhotoList.setAdapter(allPhotoAdapter);
        tv_done = (TextView) findViewById(R.id.tv_done);
        tv_done.setOnClickListener(this);
        //检查权限
        permissionHelper.checkPermission(this, new AlxPermissionHelper.AskPermissionCallBack() {
            @Override
            public void onSuccess() {
                allPhotoAdapter.getAllPhotoFromLocalStorage(SelectPhotoActivity.this);
            }

            @Override
            public void onFailed() {
                SelectPhotoActivity.this.finish();
            }
        });
        findViewById(R.id.tv_cancel).setOnClickListener(this);
    }


    @Override
    public void updateSelectActivityViewUI() {
        if (allPhotoAdapter.selectedPhotosPosition != null && allPhotoAdapter.selectedPhotosPosition.size()>0) {
            tv_done.setTextColor(Color.BLACK);
            tv_done.setClickable(true);
            tv_done.setEnabled(true);
        } else {
            tv_done.setTextColor(Color.GRAY);
            tv_done.setClickable(false);
            tv_done.setEnabled(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.registActivityResult(requestCode,permissions,grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        switch (requestCode) {
            case SelectPhotoAdapter.REQ_CAMARA: {//1000如果是相机发来的
                Log.i("Alex", "现在是相机拍照完毕");
                if (RESULT_OK == resultCode) {//系统默认值
                    String cameraPhotoUrl = "file.jpg";
                    try {
                        SharedPreferences camerasp = getSharedPreferences("Camera", Context.MODE_PRIVATE);
                        if (camerasp != null) {
                            cameraPhotoUrl = camerasp.getString("photoUrl", "file.jpg");//得到之前在adapter里存好的文件名
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + SelectPhotoAdapter.QRAVED_CAMERA_PATH);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    File file = new File(dir, cameraPhotoUrl);//这个文件指针指向拍好的图片文件

                    final String imageFilePath = file.getAbsolutePath();
                    Log.i("Alex", "拍摄图片暂存到了" + imageFilePath + "  角度是" + AlxImageLoader.readPictureDegree(imageFilePath));

                    //把图片压缩成指定宽高并且保存到本地
                    //bug修复，没什么好压缩的，不压了
                    file = new File(imageFilePath);
                    String albumUrl = null;
                    if (file.exists()) {
                        //bug修复，2015、12/15 Alex将拍摄的照片存到系统相册里去
                        Log.i("Alex", "准备存储到相册");
                        try {
                            ContentResolver cr = SelectPhotoActivity.this.getContentResolver();
                            //在往相册存储的时候返回url是DCIM的url，不是原来的了，而且exif信息也全都没了
                            // /storage/sdcard0/qraved/camera/1461321361499.jpg
                            // /storage/sdcard0/DCIM/Camera/1461321370065.jpg
                            //下面这句是把相机返回的文件拷贝到系统相册里面去,并且生产缩略图存在相册里，然后发送广播更新图片list
                            albumUrl = AlxBitmapUtils.insertImage(this, cr, file, true);//返回值为要发送图片的url
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //bug修复完毕

                        // 提交数据，和选择图片用的同一个ArrayList

                        SelectPhotoAdapter.SelectPhotoEntity photoEntity = new SelectPhotoAdapter.SelectPhotoEntity();
                        //因为存储到相册之后exif全都没了，所以应该传源文件的路径
                        photoEntity.url = albumUrl;
                        if(selectedPhotoList == null)selectedPhotoList = new ArrayList<>(1);
                        selectedPhotoList.add(photoEntity);
                        Intent intent = new Intent();
                        intent.putExtra("selectPhotos", selectedPhotoList);//把获取到图片交给别的Activity
                        intent.putExtra("isFromCamera", true);
                        setResult(SELECT_PHOTO_OK, intent);
                        finish();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_cancel:
                finish();
                break;
            case R.id.tv_done:
                if(selectedPhotoList == null)selectedPhotoList = new ArrayList<>(9);
                for(int i:allPhotoAdapter.selectedPhotosPosition)selectedPhotoList.add(allPhotoAdapter.allPhotoList.get(i-1));
                Intent intent = new Intent();
                intent.putExtra("selectPhotos", selectedPhotoList);//把获取到图片交给别的Activity
                intent.putExtra("isFromCamera", false);
                setResult(SELECT_PHOTO_OK,intent);
                finish();
                break;
        }
    }


}
