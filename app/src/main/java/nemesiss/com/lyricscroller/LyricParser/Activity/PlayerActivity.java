package nemesiss.com.lyricscroller.LyricParser.Activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;
import nemesiss.com.lyricscroller.LyricParser.Adapter.MusicDiscPagerAdapter;
import nemesiss.com.lyricscroller.LyricParser.LyricParserImpl;
import nemesiss.com.lyricscroller.LyricParser.Model.*;
import nemesiss.com.lyricscroller.LyricParser.Utils.DisplayUtil;
import nemesiss.com.lyricscroller.LyricParser.Utils.FastBlurUtil;
import nemesiss.com.lyricscroller.LyricParser.View.DiscView;
import nemesiss.com.lyricscroller.LyricParser.View.MusicDiscPager;
import nemesiss.com.lyricscroller.LyricParser.View.MusicStatus;
import nemesiss.com.lyricscroller.LyricParser.View.PlayerRootView;
import nemesiss.com.lyricscroller.R;
import nemesiss.com.lyricscroller.SimpleMusicPlayer;

import java.io.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerActivity extends AppCompatActivity
{

    private int mScreenWidth;
    private int mScreenHeight;
    private float DisplayWHRatio = ((float) 1080 / 1920);

    @BindView(R.id.toolBar)
    Toolbar PlayerToolbar;

    @BindView(R.id.disc_seekbar)
    SeekBar DiscSeekbar;

    @BindView(R.id.llNeedle)
    ImageView NeedleIv;

    @BindView(R.id.player_root)
    PlayerRootView PlayerRootLayout;

    @BindView(R.id.music_list_pager)
    MusicDiscPager MusicPlaylistPager;

    @BindView(R.id.ivPlayOrPause)
    ImageView PlayOrPauseButton;

    @BindView(R.id.seekbar_currTime)
    TextView CurrentTime;

    @BindView(R.id.seekbar_TotalTime)
    TextView TotalTime;

    @BindView(R.id.ivLoop)
    ImageView LoopModeIv;


    MusicDiscPagerAdapter musicDiscPagerAdapter;

    private static final String DefaultBackgroundImage = "74746927_p0.png";
    private static final String ShiawaseShindoromu = "ShiawaseShindoromu.lrc";

    private LyricInfo ShiawaseShindoromuLrcInfo;
    private LyricInfo ImawokakerushoujyoLrcInfo;


    // 歌曲播放控制

    private List<MusicInfo> MusicInfoList;
    private List<DiscView> MusicDiscViews;
    private List<ObjectAnimator> DiscAnimators;
    private DiscView CurrentActiveDiscView;
    private int CurrentPlayMusicDuration = 0;
    private boolean CurrentShowDisc = true;
    private BehaviorSubject<PlayerLoopMode> LoopMode = BehaviorSubject.createDefault(PlayerLoopMode.LOOP_PLAY_LIST);


    //  Needle动画控制

    private ObjectAnimator NeedleAnimator;
    private boolean ContinuePlayNextNeedleAnimation = false;
    private DiscNeedleStatus needleStatus = DiscNeedleStatus.FAR_DISC;
    private boolean IsMovingDisc = false;
    private boolean IsSeeking = false;

    // 音乐播放控制
    private SimpleMusicPlayer MusicPlayer;
    private BehaviorSubject<MusicStatus> MusicPlayStatus;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // 内部需要进行的初始化
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);

        MusicPlayer = new SimpleMusicPlayer(PlayerActivity.this);

        // 播放时间更新
        MusicPlayer.setTimeElapsedListener(new SimpleMusicPlayer.OnPlayerTimeElapsedListener()
        {
            @Override
            public void update(int CurrentTimeStamp)
            {
                if (!IsSeeking)
                {
                    int duration = CurrentPlayMusicDuration == 0 ? 0 : 100 * CurrentTimeStamp / MusicPlayer.getInnerPlayer().getDuration();
                    CurrentTime.setText(Duration2Time(CurrentTimeStamp));
                    DiscSeekbar.setProgress(duration);
                }
                // 歌词跟随
                CurrentActiveDiscView.OnPlayerTimeChanged(CurrentTimeStamp);
            }
        });
        MusicPlayStatus = MusicPlayer.getMusicPlayStatus();

        // 总时长更新
        MusicPlayer.getIsPrepared()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe((status) -> {
                    if (status)
                    {
                        CurrentPlayMusicDuration = MusicPlayer.getInnerPlayer().getDuration();
                        TotalTime.setText(Duration2Time(CurrentPlayMusicDuration));
                    }
                });


        NeedleIv.post(this::InitNeedle);
        InitNeedleAnimator();
        InitScreenResolution();
        MakeStatusBarTransparent();
        InitSeekbar();
        InitMusicPager();

        // 音乐播放状态更新
        MusicPlayStatus.subscribe((status) -> {
            Log.d("PlayerActivity", "当前播放器状态: " + status);
            switch (status)
            {
                case PLAY:
                    PlayOrPauseButton.setImageResource(R.drawable.pause);
                    break;
                case PAUSE:
                case STOP:
                    PlayOrPauseButton.setImageResource(R.drawable.play);
                    break;
            }
        });

        LoopMode.subscribeOn(AndroidSchedulers.mainThread())
                .subscribe((loop) -> {
                   switch (loop) {
                       case LOOP_PLAY_LIST:
                           LoopModeIv.setImageResource(R.drawable.looplist);
                           break;
                       case LOOP_SINGLE:
                           LoopModeIv.setImageResource(R.drawable.loopsingle);
                           break;
                       case RANDOM:
                           LoopModeIv.setImageResource(R.drawable.random);
                           break;
                   }
                });

        // 播完后行为
        MusicPlayer.getInnerPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer)
            {
                DiscSeekbar.setProgress(0);
                ResetDiscRotation(MusicPlaylistPager.getCurrentItem());
                switch (LoopMode.getValue()) {

                    case LOOP_PLAY_LIST:
                        // 判断模式: 现在默认还是直接下一首:
                        Next();
                        break;
                    case LOOP_SINGLE:
                        Play();
                        // 无需处理
                        break;
                    case RANDOM:
                        SecureRandom sr = new SecureRandom();
                        int next = sr.nextInt(MusicInfoList.size());
                        PreparePlayMusic(next);
                        break;
                }
            }
        });


        // 外界传来的信息
        LoadLyric();
        LoadMusicPages();

    }

    // ================= 外来文件加载 ======================

    // 仅作为测试用, 从Asset加载Music
    private void LoadMusicFromAssets(int position)
    {
        try
        {
            AssetFileDescriptor afd = getAssets().openFd(MusicInfoList.get(position).getMusicFileName());
            MusicPlayer.LoadMusic(afd);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void LoadMusicPages()
    {
        List<DiscView> dvs = new ArrayList<>();
        MusicDiscViews = dvs;

        DiscAnimators = new ArrayList<>();

        // 准备一个假的播放列表
        MusicInfoList = new ArrayList<>();
        MusicInfoList.add(new MusicInfo("今をかける少女", "初音ミク", DefaultBackgroundImage, ImawokakerushoujyoLrcInfo, "Imawokakerushoujyo.mp3"));
        MusicInfoList.add(new MusicInfo("シアワセシンドローム", "ナナヲアカリ", "71947756_p0.png", ShiawaseShindoromuLrcInfo, "ShiawaseShindoromu.mp3"));

        for (int i = 0; i < MusicInfoList.size(); i++)
        {
            String title = MusicInfoList.get(i).getMusicName();
            String artist = MusicInfoList.get(i).getArtistName();


            DiscView dv = (DiscView) LayoutInflater.from(PlayerActivity.this)
                    .inflate(R.layout.layout_single_discview, MusicPlaylistPager, false);

            dvs.add(dv);
            ImageView discIv = dv.findViewById(R.id.disc_image);
            DiscAnimators.add(GetDiscAnimator(discIv));

            dv.InitLyrics(MusicInfoList.get(i).getLyrics());

            dv.setDiscViewClickListener(this::SwitchDiscToLyric);

            dv.setLyricViewClickListener(this::SwitchLyricToDisc);

            // 加载图片
            LoadMusicAlbumPhoto(MusicInfoList.get(i).getAlbumPhoto(), new SimpleTarget<Bitmap>()
            {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation)
                {
                    dv.SetDiscAlbumPhoto(resource);
                }
            });
        }

        if (musicDiscPagerAdapter == null)
        {
            musicDiscPagerAdapter = new MusicDiscPagerAdapter(dvs);
            MusicPlaylistPager.setAdapter(musicDiscPagerAdapter);
        } else
        {
            musicDiscPagerAdapter.setMusicDiscList(dvs);
            musicDiscPagerAdapter.notifyDataSetChanged();
        }

        PreparePlayMusic(0);
    }

    private void LoadLyric()
    {
        InputStream lyricFileInputStream;
        try
        {
            lyricFileInputStream = getAssets().open(ShiawaseShindoromu);
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(lyricFileInputStream)))
            {
                while ((line = br.readLine()) != null)
                {
                    sb.append(line).append("\n");
                }
            }
            ShiawaseShindoromuLrcInfo = LyricParserImpl.ParseLyric(sb.toString());
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            lyricFileInputStream = getAssets().open("Imawokakerushoujyo.lrc");
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(lyricFileInputStream)))
            {
                while ((line = br.readLine()) != null)
                {
                    sb.append(line).append("\n");
                }
            }
            ImawokakerushoujyoLrcInfo = LyricParserImpl.ParseLyric(sb.toString());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // ================= 界面控件初始化控制 =================
    private void InitNeedle()
    {
        int needleWidth = NeedleIv.getWidth();
        int needleHeight = NeedleIv.getHeight();

        int marginLeft = (int) (DisplayUtil.SCALE_NEEDLE_MARGIN_LEFT * mScreenWidth);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) NeedleIv.getLayoutParams();
        layoutParams.setMargins(marginLeft, 0, 0, 0);

        NeedleIv.setLayoutParams(layoutParams);

        float pivotX = DisplayUtil.SCALE_NEEDLE_PIVOT_X * needleWidth;
        float pivotY = DisplayUtil.SCALE_NEEDLE_PIVOT_X * needleHeight;

        NeedleIv.setPivotX(pivotX);
        NeedleIv.setPivotY(pivotY);
        NeedleIv.setRotation(-25);
    }

    private void InitMusicPager()
    {

        // 属性设置
        MusicPlaylistPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        MusicPlaylistPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            int lastPositionOffsetPixels = 0;
            int currentItem = 0;
            int direction = 0;

            @Override
            public void onPageScrolled(int position,
                                       float positionOffset,
                                       int positionOffsetPixels)
            {
                // positionOffsetPixels < lastPositionOffsetPixels 认为是上一首
                // positionOffsetPixels > lastPositionOffsetPixels  下一首
                if (lastPositionOffsetPixels > positionOffsetPixels)
                {
                    // 上一首
                    direction = 0;
                    if (positionOffset < 0.5)
                    {
                        OnMusicInfoChanged(MusicInfoList.get(position));
                    } else
                    {
                        OnMusicInfoChanged(MusicInfoList.get(MusicPlaylistPager.getCurrentItem()));
                    }
                }
                //右滑
                else if (lastPositionOffsetPixels < positionOffsetPixels)
                {
                    // 下一首
                    direction = 1;
                    if (positionOffset > 0.5)
                    {
                        OnMusicInfoChanged(MusicInfoList.get(position + 1));
                    } else
                    {
                        OnMusicInfoChanged(MusicInfoList.get(position));
                    }
                }
                lastPositionOffsetPixels = positionOffsetPixels;
            }

            @Override
            public void onPageSelected(int position)
            {
                PreparePlayMusic(position);

                if (position > currentItem)
                {
                    Log.d("PlayerActivity", "NEXT");
                    //notifyMusicStatusChanged(MusicChangedStatus.NEXT);
                } else
                {
                    Log.d("PlayerActivity", "LAST");
                    // notifyMusicStatusChanged(MusicChangedStatus.LAST);
                }
                currentItem = position;
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {
                // 处理Animation
                switch (state)
                {
                    case ViewPager.SCROLL_STATE_IDLE:
                        IsMovingDisc = false;
                        if (MusicPlayStatus.getValue() == MusicStatus.PLAY)
                        {
                            PlayNeedleAnimation();
                        }
                        break;
                    case ViewPager.SCROLL_STATE_SETTLING:
                    {
                        break;
                    }
                    case ViewPager.SCROLL_STATE_DRAGGING:
                    {
                        IsMovingDisc = true;
                        Log.d("DiscView", "Direction :" + direction + "  " + MusicPlaylistPager.getCurrentItem());
                        PauseNeedleAnimation();
                        break;
                    }
                }
            }
        });
    }

    private void InitScreenResolution()
    {
        mScreenWidth = DisplayUtil.getScreenWidth(PlayerActivity.this);
        mScreenHeight = DisplayUtil.getScreenHeight(PlayerActivity.this);
        DisplayWHRatio = (float) mScreenWidth / mScreenHeight;
    }

    private void MakeStatusBarTransparent()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    private void InitSeekbar()
    {
        DiscSeekbar.setProgressTintList(ColorStateList.valueOf(Color.RED));
        DiscSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                // 跟随Seekbar的移动改变当前时间
                if(fromUser) {
                    CurrentTime.setText(Duration2Time(Progress2Duration(progress, CurrentPlayMusicDuration)));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                IsSeeking = true;
                // 停止发送旧消息。
                MusicPlayer.getTimeElapsedHandler().removeMessages(688);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                int progress = seekBar.getProgress();
                MusicPlayer.SeekTo(progress);
                MusicPlayer.BeginDispatchElapsedTimeStamp();
                IsSeeking = false;
            }
        });
    }

    private void InitSeekTimer() {
        // Seekbar以及两边的指示器归位
        CurrentTime.setText(Duration2Time(0));
        TotalTime.setText(Duration2Time(0));
        DiscSeekbar.setProgress(0);
    }


    // ================= Animator控制 =================

    private void InitNeedleAnimator()
    {
        NeedleAnimator = ObjectAnimator.ofFloat(NeedleIv, View.ROTATION, -25, 0);
        NeedleAnimator.setDuration(500);
        NeedleAnimator.setInterpolator(new AccelerateInterpolator());
        NeedleAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if (needleStatus == DiscNeedleStatus.TO_DISC)
                {
                    needleStatus = DiscNeedleStatus.IN_DISC;
                    PlayDiscAnimation(MusicPlaylistPager.getCurrentItem());
                } else if (needleStatus == DiscNeedleStatus.LEAVE_DISC)
                {
                    needleStatus = DiscNeedleStatus.FAR_DISC;
                }

                // 在这里判断是否需要马上返回

                if (ContinuePlayNextNeedleAnimation)
                {
                    ContinuePlayNextNeedleAnimation = false;
                    PlayNeedleAnimation();
                }
            }

            @Override
            public void onAnimationStart(Animator animation)
            {
                // 简单的设置状态
                if (needleStatus == DiscNeedleStatus.FAR_DISC)
                {
                    needleStatus = DiscNeedleStatus.TO_DISC;
                } else if (needleStatus == DiscNeedleStatus.IN_DISC)
                {
                    needleStatus = DiscNeedleStatus.LEAVE_DISC;
                }
            }
        });
    }

    private void PlayDiscAnimation(int position)
    {
        ObjectAnimator oa = DiscAnimators.get(position);
        if (oa.isPaused())
            oa.resume();
        else
            oa.start();

        // 如果音乐没有播放，可以通知播放。
        // MusicStatus = PLAY
    }

    private void PauseDiscAnimation(int position)
    {
        DiscAnimators.get(position).pause();
    }

    private void PlayNeedleAnimation()
    {
        if (needleStatus == DiscNeedleStatus.FAR_DISC)
        {
            NeedleAnimator.start();
            // 它会AnimationEnd的时候自动触发Disc的Animation.
        } else if (needleStatus == DiscNeedleStatus.LEAVE_DISC)
        {
            // 告知它到头之后马上回来
            ContinuePlayNextNeedleAnimation = true;
        }
    }

    private void PauseNeedleAnimation()
    {
        if (needleStatus == DiscNeedleStatus.IN_DISC)
        {
            // 针在碟上，移开针，暂停碟片动画
            int curr = MusicPlaylistPager.getCurrentItem();
            PauseDiscAnimation(curr);
            NeedleAnimator.reverse();
        } else if (needleStatus == DiscNeedleStatus.TO_DISC)
        {
            // 正在前往碟片，马上移开
            NeedleAnimator.reverse();
            /**
             * 若动画在没结束时执行reverse方法，则不会执行监听器的onStart方法，此时需要手动设置
             * */
            needleStatus = DiscNeedleStatus.LEAVE_DISC;
        }
    }


    private void SwitchLyricToDisc(View view)
    {
        CurrentShowDisc = true;
        MusicPlaylistPager.setCanScroll(true);
        AnimateNeedleHideOrShow(true);
    }

    private void SwitchDiscToLyric(View view)
    {
        CurrentShowDisc = false;
        MusicPlaylistPager.setCanScroll(false);
        AnimateNeedleHideOrShow(false);
    }

    private void AnimateNeedleHideOrShow(boolean IsShows)
    {
        ObjectAnimator oa = ObjectAnimator.ofFloat(NeedleIv, "alpha", IsShows ? new float[]{0f, 1f} : new float[]{1f, 0f});
        oa.setDuration(200);
        oa.start();
    }

    private ObjectAnimator GetDiscAnimator(ImageView dv)
    {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(dv, View.ROTATION, 0, 360);
        objectAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objectAnimator.setDuration(20 * 1000);
        objectAnimator.setInterpolator(new LinearInterpolator());
        return objectAnimator;
    }


    // ================= 播放相关控制 ===================

    private void ResetAllDiscRotation(int skip)
    {
        for (int i = 0; i < MusicInfoList.size(); i++)
        {
            if (i == skip) continue;
            ResetDiscRotation(i);
        }
    }

    private void ResetDiscRotation(int position)
    {
        ObjectAnimator oa = DiscAnimators.get(position);
        oa.pause();
        oa.end();


        DiscView dv = MusicDiscViews.get(position);
        dv.MeasureFirstLyricPaddingTop();
        ImageView discImage = dv.findViewById(R.id.disc_image);
        discImage.setRotation(0);
    }

    private void LoadMusicAlbumPhoto(String fileName, SimpleTarget<Bitmap> handler)
    {

        try
        {
            BufferedInputStream bis = new BufferedInputStream(getAssets().open(fileName));
            int total = bis.available();
            byte[] buffer = new byte[total];
            bis.read(buffer);
            bis.close();
            Glide.with(PlayerActivity.this)
                    .load(buffer)
                    .asBitmap()
                    .into(handler);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    private Drawable LoadPlayerBackgroundImage(Bitmap OriginalAlbumPhoto)
    {
        BitmapRectCropInfo cropInfo = DisplayUtil.GetPlayerBackgroundCropPixel(OriginalAlbumPhoto, DisplayWHRatio);

        Bitmap croppedAlbumPhoto = Bitmap.createBitmap(OriginalAlbumPhoto,
                cropInfo.getCropStartX(),
                cropInfo.getCropStartY(),
                cropInfo.getCropEndX() - cropInfo.getCropStartX(),
                cropInfo.getCropEndY() - cropInfo.getCropStartY());

        Bitmap blurBackground = FastBlurUtil.doBlur(croppedAlbumPhoto, 128, true);

        Drawable backgroundDrawable = new BitmapDrawable(getResources(), blurBackground);
        backgroundDrawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        return backgroundDrawable;
    }

    private void PreparePlayMusic(int position)
    {
        InitSeekTimer();
        CurrentActiveDiscView = MusicDiscViews.get(position);

        // 记住当前Disc/Lyric翻转状态
        if(CurrentShowDisc) {
            CurrentActiveDiscView.DiscNeedleContainer.setVisibility(View.VISIBLE);
            CurrentActiveDiscView.DiscNeedleContainer.setAlpha(1f);
            CurrentActiveDiscView.LyricContainer.setVisibility(View.INVISIBLE);
            CurrentActiveDiscView.LyricContainer.setAlpha(0f);
        }
        else {
            CurrentActiveDiscView.DiscNeedleContainer.setVisibility(View.GONE);
            CurrentActiveDiscView.DiscNeedleContainer.setAlpha(0f);
            CurrentActiveDiscView.LyricContainer.setVisibility(View.VISIBLE);
            CurrentActiveDiscView.LyricContainer.setAlpha(1f);
        }

        OnMusicInfoChanged(MusicInfoList.get(position));


        LoadMusicFromAssets(position);

        ResetAllDiscRotation(position);
        OnPlayerBackgroundChanged(MusicInfoList.get(position).getAlbumPhoto());

    }

    // ================= 控制音乐信息更新 ====================

    private void OnMusicInfoChanged(MusicInfo NewMusicInfo)
    {
        String title = NewMusicInfo.getMusicName();
        String subTitle = NewMusicInfo.getArtistName();

        PlayerToolbar.setTitle(title);
        PlayerToolbar.setSubtitle(subTitle);
    }

    private void OnPlayerBackgroundChanged(String NewFileName)
    {
        LoadMusicAlbumPhoto(NewFileName, new SimpleTarget<Bitmap>()
        {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation)
            {
                new Thread(() -> {
                    Drawable drawable = LoadPlayerBackgroundImage(resource);
                    runOnUiThread(() -> PlayerRootLayout.SetPlayerBackground(drawable));
                }).start();
            }
        });
    }

    // =============== 绑定按钮点击事件 ==================


    public void Play()
    {
        MusicPlayer.Play(false);
        PlayNeedleAnimation();
    }

    public void Pause()
    {
        MusicPlayer.Pause();
        PauseNeedleAnimation();
    }

    @OnClick(R.id.ivLast)
    public void Last()
    {
        SelectMusic(-1);
    }

    @OnClick(R.id.ivNext)
    public void Next()
    {
        SelectMusic(1);
    }

    private void SelectMusic(int offset)
    {
        int last = MusicInfoList.size() - 1;
        int curr = MusicPlaylistPager.getCurrentItem();

        if (0 <= curr && curr <= last || LoopMode.getValue() == PlayerLoopMode.LOOP_PLAY_LIST)
        {
            // 会触发OnPageSelected，在那里加载新的歌曲
            MusicPlaylistPager.setCurrentItem((curr + offset + last + 1) % (last + 1), true);

            PauseNeedleAnimation();
            if(MusicPlayStatus.getValue() == MusicStatus.PLAY) PlayNeedleAnimation();
        }
        else MusicPlayStatus.onNext(MusicStatus.STOP);
    }

    @OnClick(R.id.ivPlayOrPause)
    void TogglePlayOrPause()
    {
        MusicStatus currStatus = MusicPlayStatus.getValue();
        switch (currStatus)
        {

            case PLAY:
                Pause();
                break;
            case PAUSE:
            case STOP:
                Play();
                break;
        }
    }

    @OnClick(R.id.ivList)
    void TogglePlayList()
    {
        // 可以通过修改这里的逻辑展示播放列表
    }

    @OnClick(R.id.ivLoop)
    void SwitchLoopMode()
    {
        PlayerLoopMode loopMode = LoopMode.getValue();
        PlayerLoopMode[] AllMode = PlayerLoopMode.values();
        LoopMode.onNext(AllMode[((Arrays.asList(AllMode).indexOf(loopMode) + 1) % AllMode.length)]);
    }

    // ================ 帮助类 ===================
    private String Duration2Time(int duration)
    {
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;

        return (min < 10 ? "0" + min : min + "") + ":" + (sec < 10 ? "0" + sec : sec + "");
    }

    private int Progress2Duration(int progress, int totalDuration)
    {
        return (int) (((float) progress / 100) * totalDuration);
    }
}
