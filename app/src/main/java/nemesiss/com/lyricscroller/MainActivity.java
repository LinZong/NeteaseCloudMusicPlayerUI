package nemesiss.com.lyricscroller;

import android.animation.ValueAnimator;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import nemesiss.com.lyricscroller.LyricParser.Adapter.LyricRecycleAdapter;
import nemesiss.com.lyricscroller.LyricParser.LyricParserImpl;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricInfo;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricSentence;

import java.io.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity
{

    @BindView(R.id.LyricRecycleContainer)
    NestedScrollView LyricRecycleContainer;

    @BindView(R.id.LyricRecycle)
    RecyclerView lyricRecycle;

    @BindView(R.id.CurrentPosition)
    TextView CurrentPosition;

    private LinearLayoutManager linearLayoutManager;
    private LyricRecycleAdapter lyricRecycleAdapter;
    private LyricInfo lyricInfo;

    private int CurrentLyric = 0;
    private int recycleViewHeight;

    private boolean IsPlaying = false;
    private boolean IsPause = false;

    private MediaPlayer mediaPlayer;


    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        InitLyrics();

        // Prepare view components.
        LyricRecycleContainer.setSmoothScrollingEnabled(true);
        ViewCompat.setNestedScrollingEnabled(lyricRecycle, false);
    }



    private void PauseMedia() {
        if(mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            IsPlaying = false;
            IsPause = true;
        }
    }

    private void PrepareMediaPlayer()
    {
        if(mediaPlayer == null)
        {
            CurrentLyric = 0;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer)
                {
                    IsPlaying = true;
                    IsPause = false;
                    BeginScrollingLyric();
                    mediaPlayer.start();
                }
            });
        }
        
        if(IsPause)
        {
            IsPause = false;
            IsPlaying = true;
            mediaPlayer.start();
            BeginScrollingLyric();
        }
        else
        {
            mediaPlayer.reset();
            CurrentLyric = 0;
            try
            {
                AssetFileDescriptor afd = getAssets().openFd("Imawokakerushoujyo.mp3");
                mediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                mediaPlayer.prepareAsync();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

    }

    @OnClick({R.id.TogglePlayOrPause})
    void TogglePlayOrPause()
    {
        if(!IsPlaying)
            PrepareMediaPlayer();
        else
            PauseMedia();
    }



    @OnClick({R.id.ScrollNext})
    void ScrollNext()
    {
        if (0 <= CurrentLyric && CurrentLyric < lyricInfo.getSentences().size() - 1)
        {
            int next = CurrentLyric + 1;
            CurrentPosition.setText(String.valueOf(next));

            ChangeSentenceColor(CurrentLyric,false);
            ChangeSentenceColor(next,true);

            CurrentLyric++;
        }
    }

    @OnClick({R.id.ScrollBefore})
    void ScrollBefore()
    {
        if (0 < CurrentLyric && CurrentLyric <= lyricInfo.getSentences().size() - 1)
        {
            int before = CurrentLyric - 1;
            CurrentPosition.setText(String.valueOf(before));

            ChangeSentenceColor(CurrentLyric,false);
            ChangeSentenceColor(before,true);

            CurrentLyric--;
        }
    }

    private void MeasureFirstLyricPaddingTop()
    {
        lyricRecycle.post(() -> {
            recycleViewHeight = LyricRecycleContainer.getHeight();
            lyricRecycle.setPadding(0, recycleViewHeight / 2, 0, recycleViewHeight / 2);
            lyricRecycle.scrollToPosition(0);
            ChangeSentenceColor(0,true);
        });
    }
    private void ChangeSentenceColor(int position,boolean IsHighlight)
    {
        RecyclerView.ViewHolder holder = lyricRecycle.findViewHolderForAdapterPosition(position);
        if (holder != null)
        {
            TextView sentence = holder.itemView.findViewById(R.id.LyricRecycle_Sentence);
            sentence.setTextColor(IsHighlight ? Color.BLACK : Color.rgb(162, 162, 162));

            if(IsHighlight)
                ScrollToCenter(position, holder.itemView);
        }
    }

    private void ScrollToCenter(int position,View view)
    {
        int height = view.getHeight();
        int totalHeight = height * position;
        int begin = LyricRecycleContainer.getScrollY();
        BeginSmoothScroll(begin, totalHeight);
    }

    private void BeginSmoothScroll(int begin,int end)
    {
        ValueAnimator va = ValueAnimator.ofInt(begin,end);
        va.setDuration(300);
        va.addUpdateListener(valueAnimator -> {
            int curr = (int) valueAnimator.getAnimatedValue();
            LyricRecycleContainer.scrollTo(0,curr);
        });
        va.start();
    }
    private void InitLyrics()
    {
        try
        {
            Log.d(TAG, "InitLyrics's thread "+Thread.currentThread().getId());


            ExecutorService pool = Executors.newFixedThreadPool(1);
            LyricParserCallable lpc = new LyricParserCallable(getAssets().open("Imawokakerushoujyo.lrc"));
            Future<LyricInfo> lyricParserFuture = pool.submit(lpc);

            linearLayoutManager = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false);
            lyricRecycle.setLayoutManager(linearLayoutManager);

            lyricInfo = lyricParserFuture.get();
            lyricRecycleAdapter = new LyricRecycleAdapter(lyricInfo);

            lyricRecycle.setAdapter(lyricRecycleAdapter);

            // Let first sentence position to center.
            MeasureFirstLyricPaddingTop();

        } catch (IOException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
    }
    private void BeginScrollingLyric()
    {
        new Thread(() -> {
            while (IsPlaying && mediaPlayer != null && lyricInfo != null) {
                int curr = mediaPlayer.getCurrentPosition();
                int shouldRender = GetCurrentLyricPosition(curr, CurrentLyric,lyricInfo.getSentences().size());
                if(shouldRender == -2) {
                    continue;
                }
                else if(shouldRender == -1) {
                    runOnUiThread(this::ClearAllHighlight);

                    shouldRender = GetCurrentLyricPosition(curr, 0,lyricInfo.getSentences().size());

                    int fsr = shouldRender;
                    if(CurrentLyric != fsr)
                    {
                        runOnUiThread(() -> ChangeSentenceColor(fsr,true));
                    }

                    CurrentLyric = shouldRender;
                }
                else
                {
                    //render normally
                    int fsr1 = shouldRender;
                    if(CurrentLyric != fsr1)
                    {
                        runOnUiThread(this::ClearAllHighlight);
                        runOnUiThread(() -> ChangeSentenceColor(fsr1,true));
                    }
                    CurrentLyric = shouldRender;
                }
            }
        }).start();
    }

    private int GetCurrentLyricPosition(int CurrentTime, int Begin, int SentenceCount)
    {
        // 特判时间超了
        LyricSentence end = lyricInfo.getSentences().get(SentenceCount - 1);
        LyricSentence begin = lyricInfo.getSentences().get(0);
        if(CurrentTime < begin.getMills()) return -2;// 告知此时仍然不需要渲染歌词
        if(CurrentTime >= end.getMills()) return SentenceCount - 1; // 渲染最后一个

        for (int i = Begin; i < SentenceCount - 1; i++)
        {
            LyricSentence lsCurr = lyricInfo.getSentences().get(i);
            LyricSentence lsNext = lyricInfo.getSentences().get(i+1);
            if(lsCurr.getMills() <= CurrentTime && CurrentTime < lsNext.getMills())
                return i;
        }
        return -1;// 给定Begin找不到结果
    }

    private void ClearAllHighlight()
    {
        for (int i = 0; i < lyricInfo.getSentences().size(); i++)
        {
            ChangeSentenceColor(i,false);
        }
    }




    class LyricParserCallable implements Callable<LyricInfo>
    {
        private InputStream lyricFileInputStream;

        LyricParserCallable(InputStream lyricFileInputStream)
        {
            this.lyricFileInputStream = lyricFileInputStream;
        }

        @Override
        public LyricInfo call() throws Exception
        {
            Log.d(TAG, "Callable's thread "+Thread.currentThread().getId());
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(lyricFileInputStream)))
            {
                while ((line = br.readLine()) != null)
                {
                    sb.append(line).append("\n");
                }
            }
            return LyricParserImpl.ParseLyric(sb.toString());
        }
    }
}
