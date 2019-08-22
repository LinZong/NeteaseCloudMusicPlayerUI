package nemesiss.com.lyricscroller.LyricParser.Activity;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.support.v7.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import nemesiss.com.lyricscroller.LyricParser.Adapter.MusicDiscPagerAdapter;
import nemesiss.com.lyricscroller.LyricParser.LyricParserImpl;
import nemesiss.com.lyricscroller.LyricParser.Model.BitmapRectCropInfo;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricInfo;
import nemesiss.com.lyricscroller.LyricParser.Model.MusicInfo;
import nemesiss.com.lyricscroller.LyricParser.Utils.DisplayUtil;
import nemesiss.com.lyricscroller.LyricParser.Utils.FastBlurUtil;
import nemesiss.com.lyricscroller.LyricParser.View.DiscView;
import nemesiss.com.lyricscroller.LyricParser.View.MusicDiscPager;
import nemesiss.com.lyricscroller.LyricParser.View.PlayerRootView;
import nemesiss.com.lyricscroller.R;

import java.io.*;
import java.util.ArrayList;
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

    MusicDiscPagerAdapter musicDiscPagerAdapter;

    private static final String DefaultBackgroundImage = "74746927_p0.png";
    private static final String DefaultLyricFile = "ShiawaseShindoromu.lrc";

    private LyricInfo FakeLyricInfo;
    private List<MusicInfo> MusicInfoList;
    private List<DiscView> MusicDiscViews;


    //  Needle动画控制

    private ObjectAnimator NeedleAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);
        NeedleIv.post(this::InitNeedle);

        LoadLyric();
        InitScreenResolution();
        makeStatusBarTransparent();
        InitSeekbar();
        InitMusicPager();
        LoadMusicPages();
//        LoadDiscImage();
//        LoadLyric();

    }

    private void InitNeedleAnimator()
    {


        NeedleAnimator = ObjectAnimator.ofFloat(NeedleIv,View.ROTATION,-25,0);
    }

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
        NeedleIv.setRotation(0);
    }

    private void LoadMusicPages()
    {
        List<DiscView> dvs = new ArrayList<>();

        // 准备一个假的播放列表
        MusicInfoList = new ArrayList<>();
        MusicInfoList.add(new MusicInfo("演员", "薛之谦", DefaultBackgroundImage, FakeLyricInfo));
        MusicInfoList.add(new MusicInfo("社会主义好", "中国共青团", "71947756_p0.png", FakeLyricInfo));

        for (int i = 0; i < MusicInfoList.size(); i++)
        {
            String title = MusicInfoList.get(i).getMusicName();
            String artist = MusicInfoList.get(i).getArtistName();


            DiscView dv = (DiscView) LayoutInflater.from(PlayerActivity.this)
                    .inflate(R.layout.layout_single_discview, MusicPlaylistPager, false);

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

            if(i == 0) OnPlayerBackgroundChanged(MusicInfoList.get(i).getAlbumPhoto());
            dvs.add(dv);
        }
        MusicDiscViews = dvs;

        if (musicDiscPagerAdapter == null)
        {
            musicDiscPagerAdapter = new MusicDiscPagerAdapter(dvs);
            MusicPlaylistPager.setAdapter(musicDiscPagerAdapter);
        } else
        {
            musicDiscPagerAdapter.setMusicDiscList(dvs);
            musicDiscPagerAdapter.notifyDataSetChanged();
        }
    }

    private void SwitchLyricToDisc(View view)
    {
        MusicPlaylistPager.setCanScroll(true);
        AnimateNeedleHideOrShow(true);
    }

    private void SwitchDiscToLyric(View view)
    {
        MusicPlaylistPager.setCanScroll(false);
        AnimateNeedleHideOrShow(false);
    }

    private void InitMusicPager()
    {
        // 属性设置
        MusicPlaylistPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        MusicPlaylistPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            int lastPositionOffsetPixels = 0;
            int currentItem = 0;

            @Override
            public void onPageScrolled(int position,
                                       float positionOffset,
                                       int positionOffsetPixels)
            {
                if (lastPositionOffsetPixels > positionOffsetPixels)
                {
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
                // resetOtherDiscAnimation(position);
                OnPlayerBackgroundChanged(MusicInfoList.get(position).getAlbumPhoto());
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
                    case ViewPager.SCROLL_STATE_SETTLING:
                    {

                        break;
                    }
                    case ViewPager.SCROLL_STATE_DRAGGING:
                    {

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

    private void makeStatusBarTransparent()
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

    private void AnimateNeedleRotation()
    {

    }

    private void AnimateNeedleHideOrShow(boolean IsShows)
    {
        ObjectAnimator oa = ObjectAnimator.ofFloat(NeedleIv, "alpha", IsShows ? new float[]{0f, 1f} : new float[]{1f, 0f});
        oa.setDuration(200);
        oa.start();
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


    private void LoadLyric()
    {
        InputStream lyricFileInputStream;
        try
        {
            lyricFileInputStream = getAssets().open(DefaultLyricFile);
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(lyricFileInputStream)))
            {
                while ((line = br.readLine()) != null)
                {
                    sb.append(line).append("\n");
                }
            }
            FakeLyricInfo = LyricParserImpl.ParseLyric(sb.toString());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
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

}
