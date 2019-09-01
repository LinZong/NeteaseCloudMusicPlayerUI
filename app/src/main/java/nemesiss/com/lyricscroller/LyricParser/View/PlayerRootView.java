package nemesiss.com.lyricscroller.LyricParser.View;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import nemesiss.com.lyricscroller.LyricParser.Utils.BitmapTransformer.TransformToPlayerBlurBackground;
import nemesiss.com.lyricscroller.LyricParser.Utils.DisplayUtil;
import nemesiss.com.lyricscroller.LyricParser.Utils.ImageLoader;

public class PlayerRootView extends RelativeLayout
{

    private float DisplayWHRatio;

    private Drawable DefaultBackgroundDrawable;

    private LayerDrawable BackgroundChangedLayer;

    private ValueAnimator FadePlayerBackgroundAnimator;

    public PlayerRootView(Context context)
    {
        super(context);
        DisplayWHRatio = DisplayUtil.getScreenWHRatio(context);
    }

    public PlayerRootView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        DisplayWHRatio = DisplayUtil.getScreenWHRatio(context);
    }

    public PlayerRootView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        DisplayWHRatio = DisplayUtil.getScreenWHRatio(context);
    }

    private long LastRequestLoadBackgroundTimeStamp = 0;
    private boolean BackgroundLoaded = false;
    private String ShouldLoadBackgroundFilePath = null;
    private Handler BackgroundLoadRequestHandler = new android.os.Handler(this::HandleBgLoadRequest);

    private boolean HandleBgLoadRequest(Message message)
    {
        if (message.what == 9987)
        {
            ImageLoader.LoadMusicAlbumPhotoForAssets(
                    getContext(),
                    ShouldLoadBackgroundFilePath,
                    new TransformToPlayerBlurBackground(
                            getResources(),
                            DisplayWHRatio),
                    new SimpleTarget<Drawable>()
                    {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition)
                        {
                            SetPlayerBackground(resource);
                            BackgroundLoaded = true;
                        }
                    });
        }
        return true;
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        DefaultBackgroundDrawable = getBackground();
        BackgroundChangedLayer = new LayerDrawable(new Drawable[]{DefaultBackgroundDrawable, DefaultBackgroundDrawable});
        BackgroundChangedLayer.setId(0,0);
        BackgroundChangedLayer.setId(1,1);
    }

    private void SetPlayerBackground(Drawable NewPlayerBackground)
    {

        Drawable NewPlayerBackgroundCopy = NewPlayerBackground.mutate().getConstantState().newDrawable();

        if(FadePlayerBackgroundAnimator == null) {
            FadePlayerBackgroundAnimator = ValueAnimator.ofInt(0,255);
            FadePlayerBackgroundAnimator.setDuration(500);
        }
        else {
            FadePlayerBackgroundAnimator.end();
            FadePlayerBackgroundAnimator.removeAllListeners();
            FadePlayerBackgroundAnimator.removeAllUpdateListeners();
        }

        FadePlayerBackgroundAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                int alpha = (Integer) valueAnimator.getAnimatedValue();
                BackgroundChangedLayer.getDrawable(1).setAlpha(alpha);
                setBackground(BackgroundChangedLayer);
            }
        });

        FadePlayerBackgroundAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                BackgroundChangedLayer.setDrawableByLayerId(0,NewPlayerBackground);
            }

            @Override
            public void onAnimationStart(Animator animation)
            {
                super.onAnimationStart(animation);
                BackgroundChangedLayer.setDrawableByLayerId(1,NewPlayerBackgroundCopy);
                BackgroundChangedLayer.getDrawable(1).setAlpha(0);
            }
        });
        FadePlayerBackgroundAnimator.start();
    }

    public void OnPlayerBackgroundChanged(String NewFileName)
    {
        long CurrentTimeStamp = System.currentTimeMillis();
        if(CurrentTimeStamp - LastRequestLoadBackgroundTimeStamp < 350) {
            Log.d("PlayerRootView","取消当前更新Bg请求");
            BackgroundLoaded = false;
            BackgroundLoadRequestHandler.removeMessages(9987);
        }
        // 解决快速切歌的情况下Background加载错误。
        ShouldLoadBackgroundFilePath = NewFileName;
        BackgroundLoadRequestHandler.sendEmptyMessage(9987);
        LastRequestLoadBackgroundTimeStamp = CurrentTimeStamp;
    }
}
