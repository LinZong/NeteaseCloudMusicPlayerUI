package nemesiss.com.lyricscroller.LyricParser.Adapter;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nemesiss.com.lyricscroller.LyricParser.View.DiscView;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class MusicDiscPagerAdapter extends PagerAdapter
{

    private List<DiscView> MusicDiscList;

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position)
    {
        DiscView dv = MusicDiscList.get(position);
        dv.EnsureLazyInitFinished();
        container.addView(dv,position);
        return dv;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object)
    {
        container.removeView(MusicDiscList.get(position));
    }

    @Override
    public int getCount()
    {
        return MusicDiscList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o)
    {
        return view == o;
    }
}
