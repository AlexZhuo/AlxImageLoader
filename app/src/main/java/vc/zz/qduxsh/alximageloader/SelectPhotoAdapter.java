package vc.zz.qduxsh.alximageloader;

/**
 * Created by Alex on 2016/8/22.
 */

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public class SelectPhotoAdapter extends ArrayAdapter<SelectPhotoAdapter.SelectPhotoEntity> implements OnClickListener{
    public static final String QRAVED_CAMERA_PATH = "/Alex/camera";
    public String cameraPhotoUrl;
    private Activity mActivity;
    public ArrayList<SelectPhotoEntity> allPhotoList;
    int maxSelectedPhotoCount = 9;

    public static final int REQ_CAMARA = 1000;
    private File mfile1;
    private AlxImageLoader alxImageLoader;
    private int destWidth, destHeight;
    int screenWidth;
    public SelectPhotoAdapter(Activity activity, ArrayList<SelectPhotoEntity> array,File file) {
        super(activity, R.layout.adapter_select_photo, array);
        this.mActivity = activity;
        this.allPhotoList = array;
        this.mfile1 = file;
        this.alxImageLoader = new AlxImageLoader(activity);
        screenWidth = getScreenWidth(activity);
        this.destWidth = (screenWidth - 20) / 3;
        this.destHeight = (screenWidth - 20) / 3;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();

            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_select_photo, parent, false);
            viewHolder.rlPhoto = (RelativeLayout) convertView.findViewById(R.id.rlPhoto);
            viewHolder.iv_photo = (ImageView) convertView.findViewById(R.id.iv_photo);
            viewHolder.iv_select = (ImageView) convertView.findViewById(R.id.iv_select);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (viewHolder.iv_photo.getLayoutParams() != null) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) viewHolder.iv_photo.getLayoutParams();
            lp.width = destWidth;
            lp.height = destHeight;
            viewHolder.iv_photo.setLayoutParams(lp);
        }
        viewHolder.iv_photo.setImageDrawable(null);
        viewHolder.iv_photo.setTag("no_image.jpg");

        viewHolder.iv_select.setVisibility(View.VISIBLE);
        viewHolder.iv_select.setImageDrawable(getDrawable(mActivity, R.drawable.unchoose));
        viewHolder.rlPhoto.setOnClickListener(null);

        if (position == 0) {//第一个位置显示一个小相机
            viewHolder.iv_photo.setImageDrawable(getDrawable(mActivity,R.drawable.cameraadd));
            alxImageLoader.setAsyncBitmapFromSD(null,viewHolder.iv_photo,0,false,false,false);//防止回调覆盖了imageView原来的bitmap
            viewHolder.iv_select.setVisibility(View.GONE);
            viewHolder.rlPhoto.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    cameraPhotoUrl = System.currentTimeMillis() + "Alex.jpg";//相机拍完之后的命名
                    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + QRAVED_CAMERA_PATH);//设置相机拍摄完毕后放置的目录
                    if (!dir.exists()) dir.mkdirs();
                    try {
                        SharedPreferences camerasp = mActivity.getSharedPreferences("Camera", Context.MODE_PRIVATE);
                        if (camerasp != null) {
                            Editor editor = camerasp.edit();
                            editor.putString("photoUrl", cameraPhotoUrl);
                            editor.apply();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    mfile1 = new File(dir, cameraPhotoUrl);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mfile1));//在这里告诉相机你应该把拍好的照片放在什么位置
                    mActivity.startActivityForResult(intent, REQ_CAMARA);
                }
            });
        } else {
            if ((allPhotoList != null) && (position >= 0) && (allPhotoList.size() > position) && (allPhotoList.get(position-1) != null)) {
                final SelectPhotoEntity photoEntity = allPhotoList.get(position-1);
                final String filePath = photoEntity.url;

                viewHolder.iv_select.setVisibility(View.VISIBLE);
                if (checkIsExistedInSelectedPhotoArrayList(position)) {
                    viewHolder.iv_select.setImageDrawable(getDrawable(mActivity, R.drawable.choose));
                } else {
                    viewHolder.iv_select.setImageDrawable(getDrawable(mActivity, R.drawable.unchoose));
                }

                alxImageLoader.setAsyncBitmapFromSD(filePath,viewHolder.iv_photo,screenWidth/3,false,true,true);
                viewHolder.rlPhoto.setTag(R.id.rlPhoto,position);
                viewHolder.rlPhoto.setTag(R.id.tv_restaurantName,photoEntity);
                viewHolder.rlPhoto.setOnClickListener(this);
            }
        }
        return convertView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.rlPhoto:
                Log.i("Alex","点击了rl photo");
                int position = (int)v.getTag(R.id.rlPhoto);
                SelectPhotoEntity photoEntity = (SelectPhotoEntity) v.getTag(R.id.tv_restaurantName);
                ImageView ivSelect = (ImageView) v.findViewById(R.id.iv_select);
                if (mActivity == null) return;
                if (checkIsExistedInSelectedPhotoArrayList(position)) {
                    ivSelect.setImageDrawable(getDrawable(mActivity, R.drawable.unchoose));
                    removeSelectedPhoto(position);
                } else if (!isFullInSelectedPhotoArrayList()){
                    ivSelect.setImageDrawable(getDrawable(mActivity, R.drawable.choose));
                    addSelectedPhoto(position);
                } else {
                    return;
                }
                if (mActivity instanceof CallBackActivity)((CallBackActivity)mActivity).updateSelectActivityViewUI();
                break;
        }
    }


    interface CallBackActivity{
        void updateSelectActivityViewUI();

    }

    class ViewHolder {
        public RelativeLayout rlPhoto;
        public ImageView iv_photo;
        public ImageView iv_select;
    }

    public static class SelectPhotoEntity implements Serializable, Parcelable {
        public String url;
        public int isSelect;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.url);
            dest.writeInt(this.isSelect);
        }

        public SelectPhotoEntity() {
        }

        protected SelectPhotoEntity(Parcel in) {
            this.url = in.readString();
            this.isSelect = in.readInt();
        }

        public static final Parcelable.Creator<SelectPhotoEntity> CREATOR = new Parcelable.Creator<SelectPhotoEntity>() {
            @Override
            public SelectPhotoEntity createFromParcel(Parcel source) {
                return new SelectPhotoEntity(source);
            }

            @Override
            public SelectPhotoEntity[] newArray(int size) {
                return new SelectPhotoEntity[size];
            }
        };

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("SelectPhotoEntity{");
            sb.append("url='").append(url).append('\'');
            sb.append(", isSelect=").append(isSelect);
            sb.append('}');
            return sb.toString();
        }
    }

    public static int getScreenWidth(Activity activity) {
        DisplayMetrics metric = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels;
    }

    public static Drawable getDrawable(Context context, int id) {
        if ((context == null) || (id < 0)) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(id, null);
        } else {
            return context.getResources().getDrawable(id);
        }
    }

    /**
     * 从系统相册里面取出图片的uri
     */
    public void getAllPhotoFromLocalStorage(final Context context) {
        new AlxMultiTask<Void,Void,ArrayList<SelectPhotoEntity>>(){

            @Override
            protected ArrayList<SelectPhotoEntity> doInBackground(Void... params) {
                ArrayList<SelectPhotoEntity> allPhotoArrayList = new ArrayList<>();

                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver mContentResolver = context.getContentResolver();//得到内容处理者实例

                String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " desc";//设置拍摄日期为倒序
                Log.i("Alex","准备查找图片");
                // 只查询jpeg和png的图片
                Cursor mCursor = mContentResolver.query(mImageUri, null, MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?", new String[]{"image/jpeg", "image/png"}, sortOrder);

                if (mCursor != null) {
                    int size = mCursor.getCount();
                    if (size > 0) {
                        for (int i = 0; i < size; i++) {//遍历全部图片
                            mCursor.moveToPosition(i);
                            String path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA));// 获取图片的路径
                            SelectPhotoEntity entity = new SelectPhotoEntity();
                            entity.url = path;//将图片的uri放到对象里去
                            allPhotoArrayList.add(entity);
                        }
                    }
                    mCursor.close();
                }
                return allPhotoArrayList;
            }

            @Override
            protected void onPostExecute(ArrayList<SelectPhotoEntity> photoArrayList) {
                super.onPostExecute(photoArrayList);
                if(photoArrayList == null)return;

                allPhotoList.addAll(photoArrayList);
                notifyDataSetChanged();
            }
        }.executeDependSDK();
    }

    /**
     * 判断某张照片是否已经被选择过
     * @param entity
     * @return
     */
    HashSet<Integer> selectedPhotosPosition = new HashSet<>(9);
    public boolean checkIsExistedInSelectedPhotoArrayList(int position) {
        if (selectedPhotosPosition == null || selectedPhotosPosition.size() == 0) return false;
        if(selectedPhotosPosition.contains(position))return true;
        return false;
    }

    public void removeSelectedPhoto(int position) {
        selectedPhotosPosition.remove(position);
    }
    public boolean isFullInSelectedPhotoArrayList() {
        if (maxSelectedPhotoCount > 0 && selectedPhotosPosition.size() < maxSelectedPhotoCount) return false;
        return true;
    }
    public void addSelectedPhoto(int position) {
        selectedPhotosPosition.add(position);
    }
}