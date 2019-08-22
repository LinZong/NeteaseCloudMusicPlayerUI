package nemesiss.com.lyricscroller.LyricParser.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MusicInfo
{
    private String MusicName;
    private String ArtistName;
    private String AlbumPhoto;
    private LyricInfo Lyrics;
}
