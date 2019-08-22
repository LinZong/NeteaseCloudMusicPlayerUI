package nemesiss.com.lyricscroller.LyricParser.Activity;

import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import nemesiss.com.lyricscroller.LyricParser.View.DiscView;
import nemesiss.com.lyricscroller.R;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PlayerActivity extends AppCompatActivity
{

    @BindView(R.id.disc_seekbar)
    SeekBar DiscSeekbar;

    @BindView(R.id.disc_view)
    DiscView discView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);
        makeStatusBarTransparent();
        InitSeekbar();
        LoadDiscImage();
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

    private void LoadDiscImage()
    {
        try
        {
            BufferedInputStream bis = new BufferedInputStream(getAssets().open("74746927_p0.png"));
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
                        }
                    });
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }
}
