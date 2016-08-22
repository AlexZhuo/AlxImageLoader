package vc.zz.qduxsh.alximageloader;

/**
 * Created by Administrator on 2016/8/22.
 */

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Created by Administrator on 2016/4/6.
 */
public class AlxBitmapUtils {

    /**
     * 传入一个bitmap，根据传入比例进行大小缩放
     * @param bitmap
     * @param widthRatio 宽度比例，缩小就比1小，放大就比1大
     * @param heightRatio
     * @return
     */
    public static Bitmap scaleBitmap(Bitmap bitmap, float widthRatio, float heightRatio) {
        Matrix matrix = new Matrix();
        matrix.postScale(widthRatio,heightRatio);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
    }

    /**
     * 传入图片路径，根据图片进行压缩，仅压缩大小，不压缩质量,
     * @param oriFile 源文件
     * @param targetFile 这个和 stream传一个就行，只有穿file的时候才会保留exif信息，但是都会根据旋转角度进行旋转
     * @param ifDel 是否需要在压缩完毕后删除原图
     */
    public static void compressImage(File oriFile, File targetFile, OutputStream stream,boolean ifDel,int rotateDegree,boolean keepExif) {
        if(oriFile ==null || !oriFile.isFile()|| !oriFile.exists())return;
//        Log.i("Alex","源图片为"+oriFile);
//        Log.i("Alex","目标地址为"+targetFile);
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true; // 不读取像素数组到内存中，仅读取图片的信息，非常重要
            BitmapFactory.decodeFile(oriFile.getAbsolutePath(), opts);//读取文件信息，存放到Options对象中
            // 从Options中获取图片的分辨率
            int imageHeight = opts.outHeight;
            int imageWidth = opts.outWidth;
            int longEdge = Math.max(imageHeight,imageWidth);//取出宽高中的长边
            int pixelCount = (imageWidth*imageHeight)>>20;//看看这张照片有多少百万像素
//            Log.i("Alex","图片宽为"+imageWidth+"图片高为"+imageHeight+"图片像素数为"+pixelCount+"百万像素");

            long size = oriFile.length();
            Log.i("Alex","f.length 图片大小为"+(size>>10)+" KB");
            //走到这一步的时候，内存里还没有bitmap
            Bitmap bitmap = null;
            if(pixelCount >= 3){//如果超过了3百万像素，那么就首先对大小进行压缩
                float compressRatio = longEdge /1280f;
                int compressRatioInt = Math.round(compressRatio);
                if(compressRatioInt%2!=0 && compressRatioInt!=1)compressRatioInt++;//如果是奇数的话，就给弄成偶数
//                Log.i("Alex","长宽压缩比是"+compressRatio+" 偶数化后"+compressRatioInt);
                //尺寸压缩
                BitmapFactory.Options options = new BitmapFactory.Options();
                //目标出来的大小1024*1024 1百万像素，100k
                options.inSampleSize = Math.round(compressRatioInt);//注意，此处必须是偶数，根据计算好的比例进行压缩,如果长边没有超过1280*1.5，就不去压缩,否则就压缩成原来的一半
                options.inJustDecodeBounds = false;//在decode file的时候，不仅读取图片的信息，还把像素数组到内存
                options.inPreferredConfig = Bitmap.Config.RGB_565;//每个像素占四位，即R=5，G=6，B=5，没有透明度，那么一个像素点占5+6+5=16位
                //现在开始将bitmap放入内存
                bitmap = BitmapFactory.decodeFile(oriFile.getAbsolutePath(), options);//根据压缩比取出大小已经压缩好的bitmap
                //此处会打印出存入内存的bitmap大小
            }else if(imageHeight*imageWidth>2073600){//如果图片大于1920*1080，为了减少内存开销，使用RGB——565,但不进行大小压缩
                BitmapFactory.Options options = new BitmapFactory.Options();
                //目标出来的大小1024*1024 1百万像素，100k
                options.inSampleSize = 1;//注意，此处必须是偶数，根据计算好的比例进行压缩,如果长边没有超过1280*1.5，就不去压缩,否则就压缩成原来的一半
                options.inJustDecodeBounds = false;//在decode file的时候，不仅读取图片的信息，还把像素数组到内存
                options.inPreferredConfig = Bitmap.Config.RGB_565;//每个像素占四位，即R=5，G=6，B=5，没有透明度，那么一个像素点占5+6+5=16位
                //现在开始将bitmap放入内存
                bitmap = BitmapFactory.decodeFile(oriFile.getAbsolutePath(), options);//根据压缩比取出大小已经压缩好的bitmap
                //此处会打印出存入内存的bitmap大小
            }else {//如果是长图或者长边短于1920的图，那么只进行质量压缩
                // 现在开始将bitmap放入内存
                bitmap = BitmapFactory.decodeFile(oriFile.getAbsolutePath());
                //此处会打印出bitmap在内存中占得大小
            }
            if(rotateDegree!=0)bitmap = AlxImageLoader.rotateBitmap(bitmap,rotateDegree,true);//如果设定了旋转角度,那么就旋转这个bitmap
            ExifInterface exif = null;
            if(keepExif) exif = new ExifInterface(oriFile.getAbsolutePath());
            if(targetFile!=null)compressMethodAndSave(bitmap, targetFile,exif);
            if(stream!=null)compressBitmapToStream(bitmap,stream);
            if(ifDel) oriFile.delete();//是否要删除源文件
            System.gc();
        }catch (Exception e){
            Log.d("Alex",""+e.getMessage().toString());
        }catch (OutOfMemoryError e) {
            Log.i("Alex","压缩bitmap OOM了",e);
        }
    }

    /**
     * 获取一个bitmap在内存中所占的大小
     * @param image
     * @return
     */
    public static int getSize(Bitmap image){
        int size=0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {    //API 19
            size = image.getAllocationByteCount();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {//API 12
            size = image.getByteCount();
        } else {
            size = image.getRowBytes() * image.getHeight();
        }
        return size;
    }

    /**
     * 根据传来的bitmap的大小计算一个质量压缩率，并且保存到指定路径中去，只压缩质量，不压缩大小
     * @param image
     * @param targetFile
     */
    public static void compressMethodAndSave(Bitmap image, File targetFile, ExifInterface exif){
        try {
            OutputStream stream = new FileOutputStream(targetFile);
            int size = compressBitmapToStream(image,stream);
            if(size==0)return;
            if(exif!=null) copyExifTags(targetFile,exif);//如果传来的exif，就给压缩完的图片设置这个exif
            long afterSize = targetFile.length();
            Log.i("Alex","压缩完后图片大小"+(afterSize>>10)+"KB 压缩率:::"+afterSize*100/size+"%");
        }catch (Exception e){
            Log.i("Alex","压缩图片出现异常",e);
        }
    }

    public static int compressBitmapToStream(Bitmap image,OutputStream stream){
        if(image==null || stream==null)return 0;
        try {
            Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
            int size = getSize(image);
            Log.i("Alex","存入内寸的bitmap大小是"+(size>>10)+" KB 宽度是"+image.getWidth()+" 高度是"+image.getHeight());
            int quality = getQuality(size);//根据图像的大小得到合适的有损压缩质量
            Log.i("Alex","目前适用的有损压缩率是"+quality);
            long startTime = System.currentTimeMillis();
            image.compress(format, quality, stream);//压缩文件并且输出
            image.recycle();//此处把bitmap从内存中移除
            Log.i("Alex","压缩图片并且存储的耗时"+(System.currentTimeMillis()-startTime));
            return size;
        }catch (Exception e){
            Log.i("Alex","压缩图片出现异常",e);
        }
        return 0;
    }

    /**
     * 复制一个图片的exif信息到另一个图片
     * @param targetFile
     * @return
     */
    public static boolean copyExifTags(File targetFile,ExifInterface sourceExif){
        if(sourceExif == null || targetFile==null || !targetFile.isFile())return false;
        try {
            ExifInterface targetExif = new ExifInterface(targetFile.getAbsolutePath());
            Log.i("Alex","源图片有缩略图？"+sourceExif.hasThumbnail());
            targetExif.setAttribute(ExifInterface.TAG_APERTURE,sourceExif.getAttribute(ExifInterface.TAG_APERTURE));
            targetExif.setAttribute(ExifInterface.TAG_DATETIME,sourceExif.getAttribute(ExifInterface.TAG_DATETIME));
            targetExif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME,sourceExif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME));
            targetExif.setAttribute(ExifInterface.TAG_FLASH,sourceExif.getAttribute(ExifInterface.TAG_FLASH));
            targetExif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH,sourceExif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH));
            targetExif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE,sourceExif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));
            targetExif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF,sourceExif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF));
            targetExif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP,sourceExif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP));
            targetExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,sourceExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
            targetExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,sourceExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
            targetExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,sourceExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
            targetExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,sourceExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
            targetExif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD,sourceExif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD));
            targetExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH,sourceExif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
            targetExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH,sourceExif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
            targetExif.setAttribute(ExifInterface.TAG_ISO,sourceExif.getAttribute(ExifInterface.TAG_ISO));
            targetExif.setAttribute(ExifInterface.TAG_MAKE,sourceExif.getAttribute(ExifInterface.TAG_MAKE));
            targetExif.setAttribute(ExifInterface.TAG_MODEL,sourceExif.getAttribute(ExifInterface.TAG_MODEL));
            targetExif.setAttribute(ExifInterface.TAG_ORIENTATION,sourceExif.getAttribute(ExifInterface.TAG_ORIENTATION));
            targetExif.setAttribute(ExifInterface.TAG_WHITE_BALANCE,sourceExif.getAttribute(ExifInterface.TAG_WHITE_BALANCE));
            try {targetExif.saveAttributes();}catch (Exception e){Log.i("Alex","存储exif信息出现异常,这是安卓的bug",e);}
            return true;
        } catch (IOException e) {
            Log.i("Alex","复制图片的exif出现异常",e);
            return false;
        }
    }

    /**
     * 根据图像的大小得到合适的有损压缩质量，因为此时传入的bitmap大小已经比较合适了，靠近1000*1000，所以根据其内存大小进行质量压缩
     * @param size
     * @return
     */
    private static int getQuality(int size){
        int mb=size>>20;//除以100万，也就是m
        int kb = size>>10;
        Log.i("Alex","准备按照图像大小计算压缩质量，大小是"+kb+"KB,兆数是"+mb);
        if(mb>70){
            return 17;
        }else if(mb>50){
            return 20;
        }else if(mb>40){
            return 25;
        }else if(mb>20){
            return 40;
        }else if(mb>10){
            return 50;
        }else if(mb>3){//目标压缩大小 100k，这里可根据实际情况来判断
            return 60;
        }else if(mb>=2){
            return 70;
        }else if(kb > 1800) {
            return 75;
        }else if(kb > 1500){
            return 80;
        }else if(kb > 1000){
            return 85;
        }else if(kb>500){
            return 90;
        }else if(kb>100){
            return 95;
        }
        else{
            return 100;
        }
    }




    /**
     * 向系统相册插入一张图片并且创建一个缩略图供系统相册显示,返回值存放图片的地址(如果成功放到相册里就返回相册的地址，否则就是源文件的地址)
     * 如果害怕源文件插入到相册之后源文件还存在浪费空间并且重复显示，就deleteSourceFile 传true来删除源文件
     * Insert an image and create a thumbnail for it.
     *
     * @param contentResolver The content resolver to use
     * @return The URL to the newly created image, or <code>null</code> if the image failed to be stored
     * for any reason.
     */
    public static String insertImage(Context context, ContentResolver contentResolver, File sourceImage,boolean deleteSourceFile) {
        if(sourceImage==null || !sourceImage.isFile() || !sourceImage.exists())return null;
        ContentValues values = new ContentValues();
//        values.put(MediaStore.Images.Media.TITLE, title);
//        values.put(MediaStore.Images.Media.DESCRIPTION, description);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
//        values.put(MediaStore.Images.Media.DATE_ADDED, JTimeUtils.getCurrentTimeLong());
//        values.put(MediaStore.Images.Media.DATE_MODIFIED, JTimeUtils.getCurrentTimeLong());
//        values.put(MediaStore.Images.Media.DATE_TAKEN, JTimeUtils.getCurrentTimeLong());
//        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
//        values.put(MediaStore.Images.Media.LATITUDE, QravedApplication.getPhoneConfiguration().getLocation().getLatitude());
//        values.put(MediaStore.Images.Media.LONGITUDE, QravedApplication.getPhoneConfiguration().getLocation().getLongitude());
//        values.put(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, "6666");

        Uri url = null;
        String stringUrl = null;    /* value to be returned */

        try {
            url = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);//用contentResolver注册一个路径，这里的路径大约是/storage/sdcard0/DCIM/Camera/1461321370065.jpg，文件名很有可能是title
            //现在需要把文件拷贝到这个url所指向的位置
            Log.i("Alex","contentResolver分配的图片位置是"+url);
            if (url != null) {
                OutputStream imageOut = contentResolver.openOutputStream(url);//获得一个输出流，以便把图片拷贝到系统目录下
                FileInputStream in = null;
                int byteread; // 读取的字节数
                try {//把拍摄后保存的图片复制到相册文件夹里去，练exif信息也会一起复制
                    in = new FileInputStream(sourceImage);
                    byte[] buffer = new byte[1024];
                    while ((byteread = in.read(buffer)) != -1) {
                        imageOut.write(buffer, 0, byteread);
                    }
                } catch (Exception e){
                    Log.i("Alex","将图片插入系统相册异常",e);
                } finally {
                    if(in!=null)in.close();
                    if (imageOut != null) imageOut.close();
                }
                long id = ContentUris.parseId(url);
                Log.i("Alex","媒体id是"+id);
                // Wait until MINI_KIND thumbnail is generated.
                Bitmap miniThumb = MediaStore.Images.Thumbnails.getThumbnail(contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null);
                // This is for backward compatibility.
                Bitmap microThumb = StoreThumbnail(contentResolver, miniThumb, id, 50F, 50F, MediaStore.Images.Thumbnails.MICRO_KIND);
            } else {
                Log.i("Alex", "Failed to create thumbnail, removing original");
                if (url != null) {
                    contentResolver.delete(url, null, null);
                }
                url = null;
            }
        } catch (Exception e) {
            Log.i("Alex", "Failed to insert image", e);
            if (url != null) {
                contentResolver.delete(url, null, null);
                url = null;
            }
        }

        if (url != null) {
            // can post image
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(url,
                    proj, // Which columns to return
                    null, // WHERE clause; which rows to return (all rows)
                    null, // WHERE clause selection arguments (none)
                    null); // Order-by clause (ascending by name)
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                stringUrl = cursor.getString(column_index);
            }
        }
        //对某些不更新相册的应用程序强制刷新

        Intent intent2 = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File newPhotoFile = new File(stringUrl);
        Uri uri = Uri.fromFile(newPhotoFile);
        if(stringUrl!=null && stringUrl.length()>0 && newPhotoFile.isFile() && newPhotoFile.exists() && newPhotoFile.length()>1000 && newPhotoFile.canRead()){//如果确实存入相册了，就不需要原来的文件了，就可以删掉
            intent2.setData(uri);
            context.sendBroadcast(intent2);
            if(deleteSourceFile)sourceImage.delete();//防止图片重复，删掉相机生成的图片
        }else {
            Log.i("Alex","插入相册失败");
            return sourceImage.getAbsolutePath();
        }
        Log.i("Alex","发送广播的url是"+stringUrl);
        return stringUrl;
    }

    private static Bitmap StoreThumbnail(
            ContentResolver cr,
            Bitmap source,
            long id,
            float width, float height,
            int kind) {
        // create the matrix to scale it
        Matrix matrix = new Matrix();

        float scaleX = width / source.getWidth();
        float scaleY = height / source.getHeight();

        matrix.setScale(scaleX, scaleY);

        Bitmap thumb = Bitmap.createBitmap(source, 0, 0,
                source.getWidth(),
                source.getHeight(), matrix,
                true);

        ContentValues values = new ContentValues(4);
        values.put(MediaStore.Images.Thumbnails.KIND, kind);
        values.put(MediaStore.Images.Thumbnails.IMAGE_ID, (int) id);
        values.put(MediaStore.Images.Thumbnails.HEIGHT, thumb.getHeight());
        values.put(MediaStore.Images.Thumbnails.WIDTH, thumb.getWidth());

        Uri url = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream thumbOut = cr.openOutputStream(url);

            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            thumbOut.close();
            return thumb;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }


}