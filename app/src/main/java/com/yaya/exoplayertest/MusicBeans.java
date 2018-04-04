package com.yaya.exoplayertest;

/**
 * Created by Administrator on 2018/4/3.
 * 邮箱：jiaqipang@bupt.edu.cn
 * package_name com.yaya.exoplayertest
 * project_name ExoplayerTest
 * 功能：
 */
public class MusicBeans {
    private int picture;
    private String name;
    private String desc;
    private String musicUrl;

    public MusicBeans(int picture, String name, String desc, String musicUrl) {
        this.picture = picture;
        this.name = name;
        this.desc = desc;
        this.musicUrl = musicUrl;
    }

    public int getPicture() {
        return picture;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getMusicUrl() {
        return musicUrl;
    }
}
