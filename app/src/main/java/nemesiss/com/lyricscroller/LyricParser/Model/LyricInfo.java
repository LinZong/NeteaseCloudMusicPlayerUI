package nemesiss.com.lyricscroller.LyricParser.Model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class LyricInfo
{
    private HashMap<String ,String> lyricHeader;
    private List<LyricSentence> sentences;

    public LyricInfo(HashMap<String, String> lyricHeader, List<LyricSentence> sentences)
    {
        this.lyricHeader = lyricHeader;
        this.sentences = sentences;
    }
}
