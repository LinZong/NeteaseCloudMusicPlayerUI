package nemesiss.com.lyricscroller.LyricParser.Utils.BitmapTransformer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import nemesiss.com.lyricscroller.LyricParser.Model.BitmapRectCropInfo;
import nemesiss.com.lyricscroller.LyricParser.Utils.DisplayUtil;
import nemesiss.com.lyricscroller.LyricParser.Utils.FastBlurUtil;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class TransformToPlayerBlurBackground extends BitmapTransformation
{

    private static final String ID = "nemesiss.com.lyricscroller.LyricParser.Utils.BitmapTransformer.TransformToPlayerBlurBackground";
    private static byte[] ID_BYTES = null;
    public int RAND_ID = 0;
    public float DisplayWHRatio;
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

    public TransformToPlayerBlurBackground(float displayWHRatio)
    {
        RAND_ID = new SecureRandom().nextInt();
        DisplayWHRatio = displayWHRatio;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight)
    {
        BitmapRectCropInfo cropInfo = DisplayUtil.GetPlayerBackgroundCropPixel(toTransform, DisplayWHRatio);

        Bitmap croppedAlbumPhoto = Bitmap.createBitmap(toTransform,
                cropInfo.getCropStartX(),
                cropInfo.getCropStartY(),
                cropInfo.getCropEndX() - cropInfo.getCropStartX(),
                cropInfo.getCropEndY() - cropInfo.getCropStartY());

        return FastBlurUtil.doBlur(croppedAlbumPhoto, 128, true);
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
        if(obj instanceof TransformToPlayerBlurBackground) {
            TransformToPlayerBlurBackground tp = (TransformToPlayerBlurBackground) obj;
            return RAND_ID == tp.RAND_ID;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return ID.hashCode();
    }
}
