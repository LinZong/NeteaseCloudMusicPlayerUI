package nemesiss.com.lyricscroller.LyricParser;

import nemesiss.com.lyricscroller.LyricParser.Model.LyricInfo;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricSentence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricParserImpl
{

    /*
      LRC头部信息
    * [ar:Lyrics artist]

      [al:Album where the song is from]

      [ti:Lyrics (song) title]

      [au:Creator of the Songtext]

      [length:How long the song is]

      [by:Creator of the LRC file]
    * */
    private static final Pattern LyricHeaderPattern = Pattern.compile("\\[(by|ar|al|ti|au|length):");
    private static final Pattern LyricTimePatter = Pattern.compile("\\[[0-9]{2}:[0-9]{2}\\.[0-9]{2,}]");

    public static LyricInfo ParseLyric(String lyric)
    {
        HashMap<String, String> LyricHeader = new HashMap<>();// 这个HashMap里面保存着LRC头部信息。
        List<LyricSentence> sentences = new ArrayList<>();

        if (!Pattern.compile("\n").matcher(lyric).find())
        {
            throw new IllegalArgumentException("歌词文件中没有分行标志!");
        }
        // 要求Lyrics必须有分行标志!传入之前先检查!
        boolean ParsingHeader = true;
        String[] lines = lyric.split("\n");
        for (int i = 0; i < lines.length; i++)
        {
            if (ParsingHeader)
            {

                // test if next line have header
                Matcher headerMat = LyricHeaderPattern.matcher(lines[i]);
                if (headerMat.find())
                {
                    String matched = headerMat.group(0);
                    String tag = matched.replace("[", "").replace(":", "");
                    String value = lines[i].replace(matched, "").replace("]", "");
                    LyricHeader.put(tag, value);
                }
                if (i != lines.length - 1 && !LyricHeaderPattern.matcher(lines[i + 1]).find())
                {
                    ParsingHeader = false;
                }
                continue;
            }

            // 开始匹配正文部分
            Matcher bodyMat = LyricTimePatter.matcher(lines[i]);
            if (bodyMat.find())
            {
                String matchedTime = bodyMat.group(0).replace("[", "").replace("]", "");
                String[] splitTime = matchedTime.split(":"); // 0 是分钟，1是秒.毫秒
                String minusStr = splitTime[0];
                String secondStr = splitTime[1];
                long totalMills = Integer.parseInt(minusStr) * 60 * 1000 + (int) (Double.parseDouble(secondStr) * 1000);
                String text = lines[i].replace("[" + matchedTime + "]", "");
                sentences.add(new LyricSentence(totalMills, text));
            }
        }

        return new LyricInfo(LyricHeader, sentences);
    }
}
