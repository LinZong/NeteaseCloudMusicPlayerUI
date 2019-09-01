package nemesiss.com.lyricscroller.LyricParser.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import nemesiss.com.lyricscroller.LyricParser.Activity.PlayerActivity;
import nemesiss.com.lyricscroller.LyricParser.Model.BitmapRectCropInfo;


public class DisplayUtil {

    /*手柄起始角度*/
    public static final float ROTATION_INIT_NEEDLE = -30;

    /*截图屏幕宽高*/
    private static final float BASE_SCREEN_WIDTH = (float) 1080.0;
    private static final float BASE_SCREEN_HEIGHT = (float) 1920.0;

    /*唱针宽高、距离等比例*/
    public static final float SCALE_NEEDLE_WIDTH = (float) (276.0 / BASE_SCREEN_WIDTH);
    public static final float SCALE_NEEDLE_MARGIN_LEFT = (float) (500.0 / BASE_SCREEN_WIDTH);
    public static final float SCALE_NEEDLE_PIVOT_X = (float) (48.4 / 276);
    public static final float SCALE_NEEDLE_PIVOT_Y = (float) (49.15 / 414);
    public static final float SCALE_NEEDLE_HEIGHT = (float) (414.0 / BASE_SCREEN_HEIGHT);
    public static final float SCALE_NEEDLE_MARGIN_TOP = (float) (43.0 / BASE_SCREEN_HEIGHT);

    /*唱盘比例*/
    public static final float SCALE_DISC_SIZE = (float) (804.0 / BASE_SCREEN_WIDTH);
    public static final float SCALE_DISC_MARGIN_TOP = (float) (215 / BASE_SCREEN_HEIGHT);

    /*专辑图片比例*/
    public static final float SCALE_MUSIC_PIC_SIZE = (float) (532.0 / BASE_SCREEN_WIDTH);

    /*设备屏幕宽度*/
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /*设备屏幕高度*/
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static float getScreenWHRatio(Context context) {
        int screenWidth = DisplayUtil.getScreenWidth(context);
        int screenHeight = DisplayUtil.getScreenHeight(context);
        return (float) screenWidth / screenHeight;
    }

    public static BitmapRectCropInfo GetAlbumPhotoCropPixel(Bitmap OriginalAlbumPhoto)
    {
        int width = OriginalAlbumPhoto.getWidth();
        int height = OriginalAlbumPhoto.getHeight();

        int minEdge = Math.min(width,height);
        int CropStartX = 0, CropStartY = 0;

        if(minEdge == width) {
            // 宽是最短边, 切割高
            CropStartX = 0;
            CropStartY = (height - width)/2;
        }
        else
        {
            // 高是最短边，切割宽
            CropStartX = (width - height) / 2;
            CropStartY = 0;
        }
        return new BitmapRectCropInfo(
                CropStartX,
                CropStartY,
                CropStartX + minEdge,
                CropStartY + minEdge,
                minEdge);
    }


    public static BitmapRectCropInfo GetPlayerBackgroundCropPixel(Bitmap OriginalAlbumPhoto,float DisplayWHRatio)
    {
        int width = OriginalAlbumPhoto.getWidth();
        int height = OriginalAlbumPhoto.getHeight();

        int minEdge = Math.min(width,height);
        int CropStartX = 0, CropStartY = 0, CroppedWidth = 0, CroppedHeight = 0;

        // width 切割
         int newWidth = (int) (height * (DisplayWHRatio));
         int newHeight = height;
         if(newWidth <= width)
         {
             // OK
             int startX = (int) ((float)(width - newWidth) / 2);
             return new BitmapRectCropInfo(startX,
                     0,
                     startX + newWidth,
                     newHeight,
                     0);
         }
         else
         {
             newHeight = (int) (width / DisplayWHRatio);
             newWidth = width;
             if(newHeight < height)
             {
                 int startY = (int) ((float) (height - (newHeight)) /2);
                 return new BitmapRectCropInfo(0,
                         startY,
                         newWidth,
                         startY + newHeight,
                         0);
             }
         }
         throw new IllegalArgumentException("图片分辨率不正确");
    }
}
