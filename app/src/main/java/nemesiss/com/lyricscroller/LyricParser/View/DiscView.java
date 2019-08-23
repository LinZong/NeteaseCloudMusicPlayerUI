package nemesiss.com.lyricscroller.LyricParser.View;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lombok.Getter;
import lombok.Setter;
import nemesiss.com.lyricscroller.LyricParser.Adapter.LyricRecycleAdapter;
import nemesiss.com.lyricscroller.LyricParser.Model.BitmapRectCropInfo;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricInfo;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricSentence;
import nemesiss.com.lyricscroller.LyricParser.Utils.DisplayUtil;
import nemesiss.com.lyricscroller.R;

import java.util.Arrays;
import java.util.List;

public class DiscView extends RelativeLayout
{

    private static final String TAG = "DiscView";

    // 屏幕尺寸相关数据

    private int mDiscImageWidth;
    private int mDiscImageHeight;
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


    private Drawable DiscBackgroundDrawable;

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

        InitDiscViewBackground();

    }


    // =========================  Control Disc View ======================================

    public void SetDiscAlbumPhoto(Bitmap AlbumPhotoBitmap)
    {
        // Calculate crop info

        BitmapRectCropInfo cropInfo = DisplayUtil.GetAlbumPhotoCropPixel(AlbumPhotoBitmap);

        float scaleRatio = (float) mDiscImageWidth / cropInfo.getCroppedEdgeLength();
        Matrix scaleMatrix = new Matrix();

        scaleMatrix.postScale(scaleRatio, scaleRatio);

        Bitmap croppedAlbumBitmap = Bitmap.createBitmap(
                AlbumPhotoBitmap,
                cropInfo.getCropStartX(),
                cropInfo.getCropStartY(),
                cropInfo.getCroppedEdgeLength(),
                cropInfo.getCroppedEdgeLength(), scaleMatrix, false);


        RoundedBitmapDrawable RoundedAlbumBitmap = RoundedBitmapDrawableFactory.create(getResources(), croppedAlbumBitmap);
        RoundedAlbumBitmap.setAntiAlias(true);
        RoundedAlbumBitmap.setCornerRadius(croppedAlbumBitmap.getWidth());

        Drawable[] layers = {RoundedAlbumBitmap, DiscBackgroundDrawable};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        int musicPicMargin = (int) ((DisplayUtil.SCALE_DISC_SIZE - DisplayUtil
                .SCALE_MUSIC_PIC_SIZE) * mDiscImageWidth / 2);
        //调整专辑图片的四周边距，让其显示在正中
        layerDrawable.setLayerInset(0, musicPicMargin, musicPicMargin, musicPicMargin,
                musicPicMargin);

        DiscImage.setImageDrawable(layerDrawable);
    }

    private void InitDiscViewBackground()
    {
        DiscBackgroundDrawable = DiscImage.getDrawable();

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


    // =========================  Control Lyric View ======================================

    private void ResetLyricView()
    {
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

    public void InitLyrics(LyricInfo lyricInfo)
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
//            ClearAllHighlight();

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
