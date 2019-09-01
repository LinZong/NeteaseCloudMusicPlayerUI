package nemesiss.com.lyricscroller.LyricParser.View;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import nemesiss.com.lyricscroller.LyricParser.Utils.BitmapTransformer.TransformToPlayerBlurBackground;
import nemesiss.com.lyricscroller.LyricParser.Utils.ImageLoader;

public class PlayerRootView extends RelativeLayout
{

    private Drawable DefaultBackgroundDrawable;

    private LayerDrawable BackgroundChangedLayer;

    private ValueAnimator FadePlayerBackgroundAnimator;

    public PlayerRootView(Context context)
    {
        super(context);
    }

    public PlayerRootView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public PlayerRootView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    private Target LastBackgroundImageLoadRequest = null;

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

    public void OnPlayerBackgroundChanged(String NewFileName,float DisplayWHRatio)
    {
        // 解决快速切歌的情况下Background加载错误。
        if(LastBackgroundImageLoadRequest != null) {
            Glide.with(getContext()).pauseRequests();
            Glide.with(getContext()).resumeRequests();
        }

        LastBackgroundImageLoadRequest = ImageLoader.LoadMusicAlbumPhotoForAssets(
                getContext(),
                NewFileName,
                new TransformToPlayerBlurBackground(
                        getResources(),
                        DisplayWHRatio),
                new SimpleTarget<Drawable>()
                {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition)
                    {
                        SetPlayerBackground(resource);
                        LastBackgroundImageLoadRequest = null;
                    }
                });
    }
}
