package com.yaya.exoplayertest;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.FileNameGenerator;

/**
 * Created by Administrator on 2018/4/1.
 * 邮箱：jiaqipang@bupt.edu.cn
 * package_name com.yaya.exoplayertest
 * project_name ExoplayerTest
 * 功能：
 */
public class MyApplication extends Application {
    private HttpProxyCacheServer proxy;

    public static HttpProxyCacheServer getProxy(Context context) {
        MyApplication myApplication = (MyApplication) context.getApplicationContext();
        return myApplication.proxy == null ? (myApplication.proxy = myApplication.newProxy()) : myApplication.proxy;
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer.Builder(this).cacheDirectory(Utils.getVideoCacheDir(this))
                .fileNameGenerator(new MyFileNameGenerator()).build();
    }

    public class MyFileNameGenerator implements FileNameGenerator {
        // Urls contain mutable parts (parameter 'sessionToken') and stable video's id (parameter 'videoId').
        // e. g. http://example.com?videoId=abcqaz&sessionToken=xyz987
        public String generate(String url) {
            Uri uri = Uri.parse(url);
            String audioId = uri.getQueryParameter("guid");
//            String audioId = url;
//            String[] res = url.split("/");
            return audioId + ".mp3";
        }
    }
}
