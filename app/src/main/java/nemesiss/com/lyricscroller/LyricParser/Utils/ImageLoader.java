package nemesiss.com.lyricscroller.LyricParser.Utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.target.Target;

import java.io.BufferedInputStream;
import java.io.IOException;

public class ImageLoader
{
    public static Target LoadMusicAlbumPhotoForAssets(Context context, String fileName, BitmapTransformation bitmapTransformation, Target target)
    {
        // This code is working for local assets. (Demonstration)
        // For network resources or absolute path like /sdcard/somepic.png, plz use method below.
        try
        {
            BufferedInputStream bis = new BufferedInputStream(context.getAssets().open(fileName));
            int total = bis.available();
            byte[] buffer = new byte[total];
            bis.read(buffer);
            bis.close();
            RequestBuilder<Drawable> rb = Glide.with(context)
                    .load(buffer)
                    .dontAnimate();
            if (bitmapTransformation != null)
            {
                rb = rb.transform(bitmapTransformation);
            }
            return rb.into(target);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static Target LoadMusicAlbumPhoto(Context context, String fileName, BitmapTransformation bitmapTransformation, Target target)
    {
        RequestBuilder<Drawable> rb = Glide.with(context)
                .load(fileName)
                .dontAnimate();
        if (bitmapTransformation != null)
        {
            rb = rb.transform(bitmapTransformation);
        }
        return rb.into(target);
    }
}
