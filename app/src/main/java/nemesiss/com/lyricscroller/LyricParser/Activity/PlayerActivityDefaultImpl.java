package nemesiss.com.lyricscroller.LyricParser.Activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.BitmapFactory;
import nemesiss.com.lyricscroller.LyricParser.Model.MusicInfo;
import nemesiss.com.lyricscroller.R;

public class PlayerActivityDefaultImpl extends PlayerActivity
{
    private ValueAnimator LikeScaleAnimator;
    protected void PlayLikeMusicAnimation(boolean IsLike)
    {
        if (LikeScaleAnimator == null)
        {

            LikeScaleAnimator = ValueAnimator.ofFloat(1f, 1.2f,1f);
            LikeScaleAnimator.setDuration(250);
            LikeScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator)
                {
                    float scale = (float) valueAnimator.getAnimatedValue();
                    LikeMusicIv.setScaleX(scale);
                    LikeMusicIv.setScaleY(scale);
                }
            });
        }

        LikeScaleAnimator.end();
        LikeScaleAnimator.removeAllListeners();

        LikeScaleAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationStart(Animator animation)
            {
                super.onAnimationStart(animation);
                if (IsLike)
                {
                    LikeMusicIv.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.like_red));

                } else
                {
                    LikeMusicIv.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.like));

                }
            }
        });
        LikeScaleAnimator.start();
    }

    @Override
    void HandleClickLikeMusic(MusicInfo mi)
    {
        boolean like = mi.isLikeMusic();
        PlayLikeMusicAnimation(!like);
        mi.setLikeMusic(!like);
    }

    @Override
    void HandleClickDownload(MusicInfo mi)
    {

    }

    @Override
    void HandleClickShare(MusicInfo mi)
    {

    }

    @Override
    void HandleClickComments(MusicInfo mi)
    {

    }

    @Override
    void HandleClickMusicProperty(MusicInfo mi)
    {

    }
}
