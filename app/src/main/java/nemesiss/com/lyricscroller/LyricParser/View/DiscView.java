package nemesiss.com.lyricscroller.LyricParser.View;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import lombok.Getter;
import lombok.Setter;
import nemesiss.com.lyricscroller.LyricParser.Adapter.LyricRecycleAdapter;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricInfo;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricSentence;
import nemesiss.com.lyricscroller.LyricParser.Utils.BitmapTransformer.TransformToPlayerDiscView;
import nemesiss.com.lyricscroller.LyricParser.Utils.DisplayUtil;
import nemesiss.com.lyricscroller.LyricParser.Utils.ImageLoader;
import nemesiss.com.lyricscroller.R;

import java.util.Arrays;

public class DiscView extends RelativeLayout
{

    private static final String TAG = "DiscView";

    // 屏幕尺寸相关数据

    private int mDiscImageWidth = 804;
    private int mDiscImageHeight = 804;
    private int mScreenHeight;
    private int mScreenWidth;

    // 视图组件对象绑定

    @BindView(R.id.disc_image)
    public ImageView DiscImage;

    @BindView(R.id.disc_lyric_scroller)
    public LyricScrollerView LyricScroller;

    @BindView(R.id.disc_lyric)
    public LyricWaterfall LyricRecyclerView;

    @BindView(R.id.disc_needle_container)
    public RelativeLayout DiscNeedleContainer;

    @BindView(R.id.disc_lyric_container)
    public RelativeLayout LyricContainer;


    @Getter
    private Drawable DiscDefaultBackgroundDrawable;

    // 歌词显示布局控制

    private ValueAnimator SwitchDiscAndLyricAnimator;
    private LinearLayoutManager LyricLayoutManager;
    private LyricRecycleAdapter LyricAdapter;

    // 歌词逐句显示控制

    private int CurrentLyricSentenceIndex = 0;
    private LyricInfo lyricInfo;

    @Getter
    @Setter
    private OnClickListener DiscViewClickListener;

    @Getter
    @Setter
    private OnClickListener LyricViewClickListener;

    public DiscView(Context context)
    {
        super(context);
    }

    public DiscView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public DiscView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }


    // Lazy init status

    private boolean MusicAlbumLoaded = false;

    private boolean LyricLoaded = false;

    @Getter
    @Setter
    private String MusicAlbumPhotoPath = null;


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);

        mDiscImageWidth = DiscImage.getWidth();
        mDiscImageHeight = DiscImage.getHeight();
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        ButterKnife.bind(this);
        InitLyrics(null);

        // 屏蔽RecyclerView自带滚动，全程使用NestedScrollView托管。
        ViewCompat.setNestedScrollingEnabled(LyricRecyclerView, false);
        LyricRecyclerView.setNestedScrollingEnabled(false);

        mScreenWidth = DisplayUtil.getScreenWidth(getContext());
        mScreenHeight = DisplayUtil.getScreenHeight(getContext());

        InitDefaultDiscViewBackground();
    }


    public void EnsureLazyInitFinished()
    {
        LoadMusicAlbumPhoto(MusicAlbumPhotoPath);
    }



    // =========================  Control Disc View ======================================

    private void InitDefaultDiscViewBackground()
    {
        DiscDefaultBackgroundDrawable = DiscImage.getDrawable();

        int marginTop = (int) (DisplayUtil.SCALE_DISC_MARGIN_TOP * mScreenHeight);
        RelativeLayout.LayoutParams layoutParams = (LayoutParams) DiscImage
                .getLayoutParams();

        // 定死唱盘图片的宽高, 不受专辑图叠加后的影响。
        int imageWH = (int) (mScreenWidth * ((float) (804) / (1080)));

        layoutParams.setMargins(0, marginTop, 0, 0);
        layoutParams.width = imageWH;
        layoutParams.height = imageWH;
        DiscImage.setLayoutParams(layoutParams);
    }

    private void LoadMusicAlbumPhoto(String fileNameInAssets)
    {
        // This method is working for load music album photo from assets.
        // For resources from network or local absolute path plz use method below.

        ImageLoader.LoadMusicAlbumPhotoForAssets(
                getContext(),
                fileNameInAssets,
                new TransformToPlayerDiscView(mDiscImageWidth,getResources(), DiscDefaultBackgroundDrawable),
                new DrawableImageViewTarget(DiscImage) {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition)
                    {
                        super.onResourceReady(resource, transition);
                        MusicAlbumLoaded = true;
                    }
                });
    }

    // =========================  Control Lyric View ======================================

    private void ResetLyricView()
    {
        CurrentLyricSentenceIndex = 0;
        // 滚回到第一句
        int recycleViewHeight = LyricScroller.getHeight();
        LyricRecyclerView.setPadding(0, recycleViewHeight / 2, 0, recycleViewHeight / 2);
        LyricRecyclerView.scrollToPosition(0);
        ChangeSentenceColor(0, true);
    }

    public void MeasureFirstLyricPaddingTop()
    {
        LyricScroller.post(this::ResetLyricView);
    }

    private void ChangeSentenceColor(int position, boolean IsHighlight)
    {
        RecyclerView.ViewHolder holder = LyricRecyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null)
        {
            TextView sentence = holder.itemView.findViewById(R.id.LyricRecycle_Sentence);
            sentence.setTextColor(IsHighlight ? Color.WHITE : Color.rgb(162, 162, 162));

            if (IsHighlight)
                ScrollToCenter(position, holder.itemView);
        }
    }

    private void ScrollToCenter(int position, View view)
    {
        int height = view.getHeight();
        int totalHeight = height * position;
        int begin = LyricScroller.getScrollY();
        BeginSmoothScroll(begin, totalHeight);
    }

    private void BeginSmoothScroll(int begin, int end)
    {
        ValueAnimator va = ValueAnimator.ofInt(begin, end);
        va.setDuration(300);
        va.addUpdateListener(valueAnimator -> {
            int curr = (int) valueAnimator.getAnimatedValue();
            LyricScroller.scrollTo(0, curr);
        });
        va.start();
    }

    public synchronized void InitLyrics(LyricInfo lyricInfo)
    {
        CurrentLyricSentenceIndex = 0;

        Log.d(TAG, "InitLyrics's thread " + Thread.currentThread().getId());
        if (lyricInfo != null)
        {
            this.lyricInfo = lyricInfo;
        } else
        {
            LyricSentence ls = new LyricSentence(0, "暂无歌词");
            this.lyricInfo = new LyricInfo(null, Arrays.asList(ls));
        }

        if (LyricLayoutManager == null)
        {
            LyricLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            LyricRecyclerView.setLayoutManager(LyricLayoutManager);
        }

        if (LyricAdapter == null)
        {
            LyricAdapter = new LyricRecycleAdapter(this.lyricInfo);
            LyricRecyclerView.setAdapter(LyricAdapter);
        } else
        {
            LyricAdapter.set_lyricInfo(this.lyricInfo);
            LyricAdapter.notifyDataSetChanged();
        }

        // Let first sentence position to center.
        LyricLoaded = true;
        MeasureFirstLyricPaddingTop();
    }

    public void OnPlayerTimeChanged(int CurrentTimeStamp)
    {
        int curr = CurrentTimeStamp;
        int shouldRender = GetCurrentLyricPosition(curr, CurrentTimeStamp, lyricInfo.getSentences().size());
        if (shouldRender == -2)
        {
            return;
        } else if (shouldRender == -1)
        {
            shouldRender = GetCurrentLyricPosition(curr, 0, lyricInfo.getSentences().size());

            int fsr = shouldRender;
            if (CurrentLyricSentenceIndex != fsr)
            {

                ClearAllHighlight();
                ChangeSentenceColor(fsr, true);
            }

            CurrentLyricSentenceIndex = shouldRender;
        } else
        {
            //render normally
            int fsr1 = shouldRender;
            if (CurrentLyricSentenceIndex != fsr1)
            {

                ClearAllHighlight();
                ChangeSentenceColor(fsr1, true);
            }
            CurrentLyricSentenceIndex = shouldRender;
        }
    }

    private int GetCurrentLyricPosition(int CurrentTime, int Begin, int SentenceCount)
    {
        // 特判时间超了
        LyricSentence end = lyricInfo.getSentences().get(SentenceCount - 1);
        LyricSentence begin = lyricInfo.getSentences().get(0);
        if (CurrentTime < begin.getMills()) return -2;// 告知此时仍然不需要渲染歌词
        if (CurrentTime >= end.getMills()) return SentenceCount - 1; // 渲染最后一个

        for (int i = Begin; i < SentenceCount - 1; i++)
        {
            LyricSentence lsCurr = lyricInfo.getSentences().get(i);
            LyricSentence lsNext = lyricInfo.getSentences().get(i + 1);
            if (lsCurr.getMills() <= CurrentTime && CurrentTime < lsNext.getMills())
                return i;
        }
        return -1;// 给定Begin找不到结果
    }

    private void ClearAllHighlight()
    {
        for (int i = 0; i < lyricInfo.getSentences().size(); i++)
        {
            ChangeSentenceColor(i, false);
        }
    }

    // =========================  Control Disc and Lyric switch ======================================

    @OnClick({R.id.disc_needle_container})
    void SwitchToLyricView()
    {
        if (DiscViewClickListener != null) DiscViewClickListener.onClick(this);
        AnimateDiscAndLyricSwitch(DiscNeedleContainer, LyricContainer);
    }

    @OnClick({R.id.disc_lyric_container, R.id.disc_lyric_scroller, R.id.disc_lyric})
    void SwitchToDiscView()
    {
        if (LyricViewClickListener != null) LyricViewClickListener.onClick(this);
        AnimateDiscAndLyricSwitch(LyricContainer, DiscNeedleContainer);
    }


    private void AnimateDiscAndLyricSwitch(View dim, View show)
    {

        if (SwitchDiscAndLyricAnimator != null)
        {
            SwitchDiscAndLyricAnimator.end();
        }
        SwitchDiscAndLyricAnimator = ValueAnimator.ofFloat(0, 1);
        SwitchDiscAndLyricAnimator.setDuration(200);
        SwitchDiscAndLyricAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                float curr = (float) valueAnimator.getAnimatedValue();
                dim.setAlpha(1 - curr);
                show.setAlpha(curr);
            }
        });
        SwitchDiscAndLyricAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                dim.setVisibility(GONE);
            }

            @Override
            public void onAnimationStart(Animator animation)
            {
                super.onAnimationStart(animation);
                dim.setVisibility(VISIBLE);
                show.setVisibility(VISIBLE);

                dim.setAlpha(1f);
                show.setAlpha(0);
            }
        });
        SwitchDiscAndLyricAnimator.start();
    }
    private int Dp2Px(int dp)
    {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
