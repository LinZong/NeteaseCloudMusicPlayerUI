package nemesiss.com.lyricscroller.LyricParser.Model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BitmapRectCropInfo
{
    private int CropStartX;
    private int CropStartY;
    private int CropEndX;
    private int CropEndY;
    private int CroppedEdgeLength;
}
