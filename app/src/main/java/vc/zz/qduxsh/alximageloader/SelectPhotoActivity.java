package vc.zz.qduxsh.alximageloader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectPhotoActivity extends AppCompatActivity implements SelectPhotoAdapter.CallBackActivity,View.OnClickListener{
    SelectPhotoAdapter allPhotoAdapter =  null;
    TextView tv_done;
    TextView tv_album_name;
    RelativeLayout rl_album;
    ListView lv_albumlist;
    ImageView iv_showalbum;
    AlxPermissionHelper permissionHelper = new AlxPermissionHelper();
    ArrayList<SelectPhotoAdapter.SelectPhotoEntity> selectedPhotoList = null;//用于放置即将要发送的photo
    private AlbumListAdapter albumListAdapter;
    private List<AlbumBean> albumList = new ArrayList<>();//相册列表
    boolean isShowAlbum;
    public static final int SELECT_PHOTO_OK = 20;//选择照片成功的result code
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_photo);
    }

    private void initView(){
        GridView gvPhotoList = (GridView) findViewById(R.id.gv_photo);
        allPhotoAdapter = new SelectPhotoAdapter(this, new ArrayList<SelectPhotoAdapter.SelectPhotoEntity>());
        gvPhotoList.setAdapter(allPhotoAdapter);
        tv_done = (TextView) findViewById(R.id.tv_done);
        tv_album_name = (TextView) findViewById(R.id.tv_album_name);
        findViewById(R.id.rl_center).setOnClickListener(this);
        iv_showalbum = (ImageView) findViewById(R.id.iv_showalbum);
        //选择相册的布局
        rl_album = (RelativeLayout) findViewById(R.id.rl_album);
        rl_album.setOnClickListener(this);
        lv_albumlist = (ListView) findViewById(R.id.lv_albumlist);
        tv_done.setOnClickListener(this);
        findViewById(R.id.rl_album).setOnClickListener(this);
        findViewById(R.id.tv_cancel).setOnClickListener(this);
        //检查权限,获取权限之后将手机所有注册图片搜索出来，并按照相册进行分类
        permissionHelper.checkPermission(this, new AlxPermissionHelper.AskPermissionCallBack() {
            @Override
            public void onSuccess() {
                //扫描手机上500张最近注册过的图片
                SelectPhotoAdapter.get500PhotoFromLocalStorage(SelectPhotoActivity.this, new SelectPhotoAdapter.LookUpPhotosCallback() {
                    @Override
                    public void onSuccess(ArrayList<SelectPhotoAdapter.SelectPhotoEntity> photoArrayList) {
                        if(photoArrayList == null || photoArrayList.size() ==0)return;
                        Log.i("Alex","查找500张图片成功,数量是"+photoArrayList.size());
                        allPhotoAdapter.allPhotoList.clear();
                        allPhotoAdapter.allPhotoList.addAll(photoArrayList);
                        allPhotoAdapter.notifyDataSetChanged();
                        //添加一个默认的相册用来存放这最近的500张图片
                        AlbumBean defaultAlbum = new AlbumBean();
                        defaultAlbum.albumFolder = Environment.getExternalStorageDirectory();
                        Log.i("Alex","folder是"+defaultAlbum.albumFolder.getAbsolutePath());
                        defaultAlbum.topImagePath = photoArrayList.get(0).url;
                        defaultAlbum.imageCounts = photoArrayList.size();
                        defaultAlbum.folderName = "Recently";
                        albumList.add(0,defaultAlbum);
                        if(albumListAdapter != null){//这个回调优先于查找相册回调
                            Log.i("Alex","500张图片落后回调");
                            albumListAdapter.notifyDataSetChanged();
                        }
                    }
                });
                //查找并设置手机上的所有相册
                AlbumBean.getAllAlbumFromLocalStorage(SelectPhotoActivity.this, new AlbumBean.AlbumListCallback() {
                    @Override
                    public void onSuccess(ArrayList<AlbumBean> result) {
                        albumList.addAll(result);
                        albumListAdapter = new AlbumListAdapter(SelectPhotoActivity.this,albumList);
                        lv_albumlist.setAdapter(albumListAdapter);
                    }
                });
                lv_albumlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if(albumList!=null && position < albumList.size() && albumList.get(position) != null){//满足条件才能set
                            tv_album_name.setText(albumList.get(position).folderName);
                        }
                        isShowAlbum=false;
                        hideAlbum();
                        AlbumBean.getAlbumPhotosFromLocalStorage(SelectPhotoActivity.this, albumList.get(position), new AlbumBean.AlbumPhotosCallback() {
                            @Override
                            public void onSuccess(ArrayList<SelectPhotoAdapter.SelectPhotoEntity> photos) {
                                Log.i("Alex","new photo list是"+photos);
                                allPhotoAdapter.allPhotoList.clear();//因为是ArrayAdapter，所以引用不能重置
                                allPhotoAdapter.allPhotoList.addAll(photos);
                                allPhotoAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
            }

            @Override
            public void onFailed() {
                SelectPhotoActivity.this.finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(lv_albumlist == null)initView();
    }



    @Override
    public void updateSelectActivityViewUI() {
        if (allPhotoAdapter.selectedPhotosSet != null && allPhotoAdapter.selectedPhotosSet.size()>0) {
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
                    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + SelectPhotoAdapter.CAMERA_PHOTO_PATH);
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, cameraPhotoUrl);//这个文件指针指向拍好的图片文件
                    final String imageFilePath = file.getAbsolutePath();
                    Log.i("Alex", "拍摄图片暂存到了" + imageFilePath + "  角度是" + AlxImageLoader.readPictureDegree(imageFilePath));
                    //把图片压缩成指定宽高并且保存到本地
                    file = new File(imageFilePath);
                    String albumUrl = null;
                    if (!file.exists()) break;
                    Log.i("Alex", "准备存储到相册");
                    try {
                        ContentResolver cr = SelectPhotoActivity.this.getContentResolver();
                        //在往相册存储的时候返回url是DCIM的url，不是原来的了，而且exif信息也全都没了
                        // /storage/sdcard0/Alex/camera/1461321361499.jpg
                        // /storage/sdcard0/DCIM/Camera/1461321370065.jpg
                        //下面这句是把相机返回的文件拷贝到系统相册里面去,并且生产缩略图存在相册里，然后发送广播更新图片list
                        albumUrl = AlxBitmapUtils.insertImage(this, cr, file, true);//返回值为要发送图片的url
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                for(SelectPhotoAdapter.SelectPhotoEntity p:allPhotoAdapter.selectedPhotosSet)selectedPhotoList.add(p);
                Intent intent = new Intent();
                intent.putExtra("selectPhotos", selectedPhotoList);//把获取到图片交给别的Activity
                intent.putExtra("isFromCamera", false);
                setResult(SELECT_PHOTO_OK,intent);
                finish();
                break;
            case R.id.rl_center:
                if(isShowAlbum)hideAlbum();//现在是展示album的状态
                else showAlbum();//现在是关闭（正常）状态
                break;
            case R.id.rl_album:
                hideAlbum();
                break;
        }
    }

    /**
     * 隐藏相册选择页
     */
    void hideAlbum(){
        if(Build.VERSION.SDK_INT >=11) {
            ObjectAnimator animator1 = ObjectAnimator.ofFloat(rl_album, "alpha", 1.0f, 0.0f);
            ObjectAnimator animator2 = ObjectAnimator.ofFloat(iv_showalbum, "rotationX", 180f, 360f);
            ObjectAnimator animator3 = ObjectAnimator.ofFloat(rl_album, "translationY", -dp2Px(45));
            AnimatorSet set = new AnimatorSet();
            set.setDuration(300).playTogether(animator1, animator2, animator3);
            set.start();
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    rl_album.setVisibility(View.GONE);
                }
            });
        }else {
            rl_album.setVisibility(View.GONE);
        }
        isShowAlbum=false;
    }

    /**
     * 显示相册选择页
     */
    void showAlbum(){
        if(Build.VERSION.SDK_INT>=11) {
            rl_album.setVisibility(View.VISIBLE);//一定要先顯示，才能做動畫操作
            ObjectAnimator animator1 = ObjectAnimator.ofFloat(rl_album, "alpha", 0.0f, 1.0f);
            ObjectAnimator animator2 = ObjectAnimator.ofFloat(iv_showalbum, "rotationX", 0f, 180f);
            ObjectAnimator animator3 = ObjectAnimator.ofFloat(rl_album, "translationY", dp2Px(45));
            AnimatorSet set = new AnimatorSet();
            set.setDuration(300).playTogether(animator1, animator2, animator3);
            set.start();
        }else {
            rl_album.setVisibility(View.VISIBLE);//一定要先顯示，才能做動畫操作
        }
        isShowAlbum=true;
    }

    public int dp2Px(int dp) {
        try {
            DisplayMetrics metric = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metric);
            return (int) (dp * metric.density + 0.5f);
        } catch (Exception e) {
            e.printStackTrace();
            return dp;
        }
    }

}
