package nemesiss.com.lyricscroller.LyricParser.View;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class LyricScrollerView extends NestedScrollView
{
    private boolean NoNeedSwitch = false;
    public LyricScrollerView(@NonNull Context context)
    {
        super(context);
    }

    public LyricScrollerView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public LyricScrollerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        super.onInterceptTouchEvent(ev);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        super.onTouchEvent(ev);
        int action = ev.getAction();

        if(action == 2)
        {
            NoNeedSwitch = true;
        }
        if(action == 1)
        {
            if(NoNeedSwitch)
            {
                NoNeedSwitch = false;
            }
            else
            {
                callOnClick();
            }
        }

        return true;
    }
}
