package nemesiss.com.lyricscroller.LyricParser.Utils.BitmapTransformer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import nemesiss.com.lyricscroller.LyricParser.Model.BitmapRectCropInfo;
import nemesiss.com.lyricscroller.LyricParser.Utils.DisplayUtil;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;


public class TransformToPlayerDiscView extends BitmapTransformation
{
    private static final String ID = "nemesiss.com.lyricscroller.LyricParser.Utils.BitmapTransformer.TransformToPlayerDiscView";
    private static byte[] ID_BYTES = null;
    public int RAND_ID = 0;
    private float DiscImageWidth;
    private Resources mResources;
    private Drawable mDiskOriginalBackground;

    static
    {
        try
        {
            ID_BYTES = ID.getBytes(STRING_CHARSET_NAME);

        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    public TransformToPlayerDiscView(float discImageWidth, Resources resources, Drawable diskOriginalBackground)
    {
        DiscImageWidth = discImageWidth;
        mResources = resources;
        mDiskOriginalBackground = diskOriginalBackground;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest)
    {
        messageDigest.update(ID_BYTES);
        byte[] radiusData = ByteBuffer.allocate(8).putInt(RAND_ID).array();
        messageDigest.update(radiusData);
    }

    @Override
    public boolean equals(@Nullable Object obj)
    {
        if (obj instanceof TransformToPlayerDiscView)
        {
            TransformToPlayerDiscView tp = (TransformToPlayerDiscView) obj;
            return RAND_ID == tp.RAND_ID;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return ID.hashCode();
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight)
    {
        BitmapRectCropInfo cropInfo = DisplayUtil.GetAlbumPhotoCropPixel(toTransform);

        Bitmap croppedAlbumBitmap = Bitmap.createBitmap(
                toTransform,
                cropInfo.getCropStartX(),
                cropInfo.getCropStartY(),
                cropInfo.getCroppedEdgeLength(),
                cropInfo.getCroppedEdgeLength());

        RoundedBitmapDrawable RoundedAlbumBitmap = RoundedBitmapDrawableFactory.create(mResources, croppedAlbumBitmap);
        RoundedAlbumBitmap.setAntiAlias(true);
        RoundedAlbumBitmap.setCornerRadius(croppedAlbumBitmap.getWidth());

        Drawable[] layers = {RoundedAlbumBitmap, mDiskOriginalBackground};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        int musicPicMargin = (int) ((DisplayUtil.SCALE_DISC_SIZE - DisplayUtil
                .SCALE_MUSIC_PIC_SIZE) * DiscImageWidth / 2);
        //调整专辑图片的四周边距，让其显示在正中
        layerDrawable.setLayerInset(0, musicPicMargin, musicPicMargin, musicPicMargin,
                musicPicMargin);


        Bitmap bitmap = Bitmap.createBitmap(outWidth, outHeight, toTransform.getConfig());
        Canvas canvas = new Canvas(bitmap);
        layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        layerDrawable.draw(canvas);

        return bitmap;
    }
}
