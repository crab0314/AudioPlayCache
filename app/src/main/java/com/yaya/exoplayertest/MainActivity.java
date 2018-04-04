package com.yaya.exoplayertest;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String testUrl = "http://ws.stream.qqmusic.qq.com/M500001VfvsJ21xFqb.mp3?guid=ffffffff82def4af4b12b3cd9337d5e7&uin=346897220&vkey=6292F51E1E384E06DCBDC9AB7C49FD713D632D313AC4858BACB8DDD29067D3C601481D36E62053BF8DFEAF74C0A5CCFADD6471160CAF3E6A&fromtag=46";
    SimpleExoPlayer mPlayer;
    CustomPlaybackControlView playbackControlView;

    MyApplication myApplication;
    HttpProxyCacheServer proxy;
    String proxyUrl;

    CustomToolBar customToolBar;

    CustomRecyclerView recyclerView;
    ScrollView scrollView;
    TextView name;
    int selPosition = -1;
    MusicAdapter musicAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        customToolBar = findViewById(R.id.custom);
        customToolBar.setLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "点击了左键", Toast.LENGTH_SHORT).show();
            }
        });
        customToolBar.setRightClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "点击了右键", Toast.LENGTH_SHORT).show();
            }
        });

        RenderersFactory renderersFactory = new DefaultRenderersFactory(this);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        LoadControl loadControl = new DefaultLoadControl();
        playbackControlView = findViewById(R.id.myView);
        mPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, loadControl);

        playbackControlView.setPlayer(mPlayer);
        playbackControlView.show();
        playbackControlView.setShowTimeoutMs(0);

        myApplication = (MyApplication) getApplication();
        proxy = myApplication.getProxy(MainActivity.this);

        //initAudioCache(testUrl);
        initMusicList();

        Log.i("dir", "" + Utils.getVideoCacheDir(MainActivity.this));
    }

    private void initAudioCache(String musicUrl) {

        Log.d("position","initAudioCache");

        proxyUrl = proxy.getProxyUrl(musicUrl, true);

        if (proxy.isCached(musicUrl)) {
            Toast.makeText(MainActivity.this, "已缓存", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "未缓存", Toast.LENGTH_LONG).show();
        }
        initExoPlayer();
    }

    private void initExoPlayer() {
        Uri mp3Uri = Uri.parse(proxyUrl);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "exoPlayerTest"));
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource mediaSource = new ExtractorMediaSource(
                mp3Uri, dataSourceFactory, extractorsFactory, null, null);
        mPlayer.prepare(mediaSource);
        playbackControlView.startMusic();
        playbackControlView.setPrevClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(MainActivity.this, "点击了上一个", Toast.LENGTH_SHORT).show();
                selPosition -= 1;
                musicAdapter.notifyDataSetChanged();
            }
        });

        playbackControlView.setNextClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(MainActivity.this, "点击了下一个", Toast.LENGTH_SHORT).show();
                selPosition += 1;
                musicAdapter.notifyDataSetChanged();
            }
        });
    }

    private void initMusicList() {
        List<MusicBeans> musicBeansList = new ArrayList<>();
        musicBeansList.add(new MusicBeans(R.drawable.lin, "伟大的渺小", "很好听的", testUrl));
        musicBeansList.add(new MusicBeans(R.mipmap.ic_launcher, "小瓶子", "很好听的~", testUrl));
        musicBeansList.add(new MusicBeans(R.mipmap.ic_launcher, "小瓶子", "很好听的~", "wulala"));
        recyclerView = findViewById(R.id.musicList);
        scrollView = findViewById(R.id.scrollView);
        scrollView.smoothScrollTo(0, 0);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        musicAdapter = new MusicAdapter(musicBeansList);
        recyclerView.setAdapter(musicAdapter);
    }

    @Override
    protected void onDestroy() {
        mPlayer.release();
        super.onDestroy();
    }

    public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

        private List<MusicBeans> musicList;

        public class ViewHolder extends RecyclerView.ViewHolder {
            View musicView;
            ImageView imageView;
            TextView nameView;
            TextView descView;

            public ViewHolder(View view) {
                super(view);
                musicView = view;
                imageView = view.findViewById(R.id.music_picture);
                nameView = view.findViewById(R.id.music_name);
                descView = view.findViewById(R.id.music_desc);
            }
        }

        public MusicAdapter(List<MusicBeans> list) {
            musicList = list;
        }

        @NonNull
        @Override
        public MusicAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_list, parent, false);
            final ViewHolder viewHolder = new ViewHolder(view);

            viewHolder.musicView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = viewHolder.getAdapterPosition();
                    MusicBeans musicBeans = musicList.get(position);
                    playbackControlView.setMusicName(musicBeans.getName());
                    playbackControlView.setDiscMove(musicBeans.getPicture());
                    initAudioCache(musicBeans.getMusicUrl());
                    selPosition = Integer.parseInt(view.getTag().toString());
                    notifyDataSetChanged();

//                    if (selPosition >= 1) playbackControlView.setPrevEnable(true);
//                    else playbackControlView.setPrevEnable(false);
//
//                    if (selPosition <= musicList.size() - 2)
//                        playbackControlView.setNextEnable(true);
//                    else playbackControlView.setNextEnable(false);


                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull MusicAdapter.ViewHolder holder, int position) {
            Log.d("bindView","onBind"+position+"===============");

            MusicBeans musicBeans = musicList.get(position);
            holder.imageView.setImageResource(musicBeans.getPicture());
            holder.nameView.setText(musicBeans.getName());
            holder.descView.setText(musicBeans.getDesc());
            holder.musicView.setTag(position);

            if (selPosition != -1) {
                Log.d("onBind","selPosition="+selPosition+"  position="+position);
                if (selPosition == position) {
                    holder.nameView.setTextColor(getResources().getColor(R.color.main_color));
                    MusicBeans music = musicList.get(position);
                    playbackControlView.setMusicName(music.getName());
                    playbackControlView.setDiscMove(music.getPicture());
                    Log.d("onBind","name"+music.getName());
                    initAudioCache(musicBeans.getMusicUrl());
                } else {
                    holder.nameView.setTextColor(getResources().getColor(R.color.music_name_black));
                }

                if (selPosition >= 1) playbackControlView.setPrevEnable(true);
                else playbackControlView.setPrevEnable(false);

                if (selPosition <= musicList.size() - 2)
                    playbackControlView.setNextEnable(true);
                else playbackControlView.setNextEnable(false);

            }
        }

        @Override
        public int getItemCount() {
            return musicList.size();
        }
    }


}
