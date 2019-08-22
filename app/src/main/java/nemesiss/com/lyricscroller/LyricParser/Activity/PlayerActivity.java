package nemesiss.com.lyricscroller.LyricParser.Activity;

import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import nemesiss.com.lyricscroller.LyricParser.LyricParserImpl;
import nemesiss.com.lyricscroller.LyricParser.Model.BitmapRectCropInfo;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricInfo;
import nemesiss.com.lyricscroller.LyricParser.Utils.DisplayUtil;
import nemesiss.com.lyricscroller.LyricParser.Utils.FastBlurUtil;
import nemesiss.com.lyricscroller.LyricParser.View.DiscView;
import nemesiss.com.lyricscroller.R;

import java.io.*;

public class PlayerActivity extends AppCompatActivity
{

    private int mScreenWidth;
    private int mScreenHeight;
    private float DisplayWHRatio = ((float) 1080 / 1920);

    @BindView(R.id.disc_seekbar)
    SeekBar DiscSeekbar;

    @BindView(R.id.disc_view)
    DiscView discView;

    @BindView(R.id.player_root)
    RelativeLayout PlayerRootLayout;

    private static final String DefaultBackgroundImage = "74746927_p0.png";
    private static final String DefaultLyricFile = "ShiawaseShindoromu.lrc";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);

        InitScreenResolution();
        makeStatusBarTransparent();
        InitSeekbar();
        LoadDiscImage();
        LoadLyric();
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

    private void LoadPlayerBackgroundImage(Bitmap OriginalAlbumPhoto)
    {
        new Thread(() -> {
            BitmapRectCropInfo cropInfo = DisplayUtil.GetPlayerBackgroundCropPixel(OriginalAlbumPhoto, DisplayWHRatio);

            Bitmap croppedAlbumPhoto = Bitmap.createBitmap(OriginalAlbumPhoto,
                    cropInfo.getCropStartX(),
                    cropInfo.getCropStartY(),
                    cropInfo.getCropEndX() - cropInfo.getCropStartX(),
                    cropInfo.getCropEndY() - cropInfo.getCropStartY());

            Bitmap blurBackground = FastBlurUtil.doBlur(croppedAlbumPhoto, 128, true);

            Drawable backgroundDrawable = new BitmapDrawable(getResources(), blurBackground);
            backgroundDrawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            runOnUiThread(() -> PlayerRootLayout.setBackground(backgroundDrawable));
        }).start();
    }


    private void LoadDiscImage()
    {
        try
        {
            BufferedInputStream bis = new BufferedInputStream(getAssets().open(DefaultBackgroundImage));
            int total = bis.available();
            byte[] buffer = new byte[total];
            bis.read(buffer);
            bis.close();
            Glide.with(PlayerActivity.this)
                    .load(buffer)
                    .asBitmap()
                    .into(new SimpleTarget<Bitmap>()
                    {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation)
                        {
                            discView.SetDiscAlbumPhoto(resource);
                            LoadPlayerBackgroundImage(resource);
                        }
                    });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void LoadLyric()
    {
        new Thread(() -> {
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
                LyricInfo li = LyricParserImpl.ParseLyric(sb.toString());
                runOnUiThread(() -> discView.InitLyrics(li));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }).start();
    }
}
