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

    private int MoveX = 0, MoveY = 0, DownX = 0, DownY = 0;
    private long TouchTime = 0;


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

        switch (action){
            case MotionEvent.ACTION_DOWN:
            {
                MoveX = 0;
                MoveY = 0;
                DownX = (int) (ev.getX()) ;DownY = (int) ev.getY();
                TouchTime = System.currentTimeMillis();
                break;
            }
            case MotionEvent.ACTION_MOVE:
            {
                int CurrX = (int) ev.getX(), CurrY = (int) ev.getY();
                MoveX += Math.abs(CurrX - DownX);
                MoveY += Math.abs(CurrY - DownY);
                DownX = CurrX;
                DownY = CurrY;
                break;
            }
            case MotionEvent.ACTION_UP:
            {
                long currTime = System.currentTimeMillis();
                if((currTime - TouchTime) > 50 && (MoveY > 20 || MoveX > 20))
                    break;
                else callOnClick();
            }
        }
        return true;
    }

    @Override
    public void fling(int velocityY)
    {
        super.fling(velocityY / 5);
    }
}
