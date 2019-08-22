package nemesiss.com.lyricscroller.LyricParser.View;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import nemesiss.com.lyricscroller.LyricParser.Model.BitmapRectCropInfo;
import nemesiss.com.lyricscroller.LyricParser.Utils.DisplayUtil;
import nemesiss.com.lyricscroller.R;

public class DiscView extends RelativeLayout
{

    private int mDiscImageWidth;
    private int mDiscImageHeight;

    private int mScreenHeight;
    private int mScreenWidth;
    @BindView(R.id.llNeedle)
    ImageView NeedleIv;

    @BindView(R.id.disc_image)
    ImageView DiscImage;

    private Drawable DiscBackgroundDrawable;


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

        mScreenWidth = DisplayUtil.getScreenWidth(getContext());
        mScreenHeight = DisplayUtil.getScreenHeight(getContext());

        InitDiscViewBackground();
        NeedleIv.post(this::InitNeedle);
    }


    public void SetDiscAlbumPhoto(Bitmap AlbumPhotoBitmap)
    {
        // Calculate crop info

        BitmapRectCropInfo cropInfo = DisplayUtil.GetAlbumPhotoCropPixel(AlbumPhotoBitmap);

        float scaleRatio = (float) mDiscImageWidth / cropInfo.getCroppedEdgeLength();
        Matrix scaleMatrix = new Matrix();

        scaleMatrix.postScale(scaleRatio,scaleRatio);

        Bitmap croppedAlbumBitmap = Bitmap.createBitmap(
                AlbumPhotoBitmap,
                cropInfo.getCropStartX(),
                cropInfo.getCropStartY(),
                cropInfo.getCroppedEdgeLength(),
                cropInfo.getCroppedEdgeLength(),scaleMatrix,false);


        RoundedBitmapDrawable RoundedAlbumBitmap = RoundedBitmapDrawableFactory.create(getResources(),croppedAlbumBitmap);
        RoundedAlbumBitmap.setAntiAlias(true);
        RoundedAlbumBitmap.setCornerRadius(croppedAlbumBitmap.getWidth());

        Drawable[] layers = {RoundedAlbumBitmap,DiscBackgroundDrawable};
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
        layoutParams.setMargins(0, marginTop, 0, 0);

        DiscImage.setLayoutParams(layoutParams);
    }

    private void InitNeedle()
    {
        int needleWidth = NeedleIv.getWidth();
        int needleHeight = NeedleIv.getHeight();

        int marginLeft = (int) (DisplayUtil.SCALE_NEEDLE_MARGIN_LEFT * mScreenWidth);

        RelativeLayout.LayoutParams layoutParams = (LayoutParams) NeedleIv.getLayoutParams();
        layoutParams.setMargins(marginLeft,0 , 0, 0);

        NeedleIv.setLayoutParams(layoutParams);

        float pivotX = DisplayUtil.SCALE_NEEDLE_PIVOT_X * needleWidth;
        float pivotY = DisplayUtil.SCALE_NEEDLE_PIVOT_X * needleHeight;

        NeedleIv.setPivotX(pivotX);
        NeedleIv.setPivotY(pivotY);
        NeedleIv.setRotation(0);
    }

    private int Dp2Px(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
