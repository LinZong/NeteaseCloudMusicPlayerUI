package nemesiss.com.lyricscroller.LyricParser.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LyricSentence
{
    private long Mills;
    private String Text;

    public LyricSentence(long mills, String text)
    {
        Mills = mills;
        Text = text;
    }
}
