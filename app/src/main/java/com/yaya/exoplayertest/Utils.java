package com.yaya.exoplayertest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;

/**
 * Created by Administrator on 2018/4/2.
 * 邮箱：jiaqipang@bupt.edu.cn
 * package_name com.yaya.exoplayertest
 * project_name ExoplayerTest
 * 功能：
 */
public class Utils {

    /**
     *
     * @param context
     * @return
     *
     * these are some utils for AndroidVideoCache
     */


    public static File getVideoCacheDir(Context context) {
        return new File(context.getExternalFilesDir("music"), "audio-cache");
    }

    public static void cleanVideoCacheDir(Context context) throws IOException {
        File videoCacheDir = getVideoCacheDir(context);
        cleanDirectory(videoCacheDir);
    }

    private static void cleanDirectory(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        File[] contentFiles = file.listFiles();
        if (contentFiles != null) {
            for (File contentFile : contentFiles) {
                delete(contentFile);
            }
        }
    }

    private static void delete(File file) throws IOException {
        if (file.isFile() && file.exists()) {
            deleteOrThrow(file);
        } else {
            cleanDirectory(file);
            deleteOrThrow(file);
        }
    }

    private static void deleteOrThrow(File file) throws IOException {
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (!isDeleted) {
                throw new IOException(String.format("File %s can't be deleted", file.getAbsolutePath()));
            }
        }
    }


    /**
     * these are some utils for music UI
     */
    public static Drawable getDiscBlackgroundDrawable(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int discSize = (int) (width * 0.5);
        //int discSize = 400;
//        Bitmap bitmapDisc = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R
//                .mipmap.ic_launcher), discSize, discSize, false);
        RoundedBitmapDrawable roundDiscDrawable = RoundedBitmapDrawableFactory.create
                (context.getResources(), BitmapFactory.decodeResource(context.getResources(),R.mipmap.ic_launcher));
        roundDiscDrawable.setCircular(true);

        RoundedBitmapDrawable roundedBitmapDrawable1 = RoundedBitmapDrawableFactory.create(context.getResources(), BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));

        roundedBitmapDrawable1.setCircular(true);

        return roundedBitmapDrawable1;
    }


    public static Drawable getRoundBitmap(Context context,int resId){

        Bitmap src = BitmapFactory.decodeResource(context.getResources(), resId);
        Bitmap dst;
//将长方形图片裁剪成正方形图片
        if (src.getWidth() >= src.getHeight()){
            dst = Bitmap.createBitmap(src, src.getWidth()/2 - src.getHeight()/2, 0, src.getHeight(), src.getHeight()
            );
        }else{
            dst = Bitmap.createBitmap(src, 0, src.getHeight()/2 - src.getWidth()/2, src.getWidth(), src.getWidth()
            );
        }

        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), dst);
        roundedBitmapDrawable.setCornerRadius(dst.getWidth() / 2); //设置圆角半径为正方形边长的一半
        roundedBitmapDrawable.setAntiAlias(true);
        return roundedBitmapDrawable;
    }


}
