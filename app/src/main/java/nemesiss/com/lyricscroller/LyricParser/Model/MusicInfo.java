package nemesiss.com.lyricscroller.LyricParser.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class MusicInfo<TSource> implements Serializable
{
    private String MusicName;
    private String ArtistName;
    private String AlbumPhoto;
    private LyricInfo Lyrics;
    private TSource MusicFileName;
    private boolean LikeMusic;
    private Class<TSource> FileType;
}
