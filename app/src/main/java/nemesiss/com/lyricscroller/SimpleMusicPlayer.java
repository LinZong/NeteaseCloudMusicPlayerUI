package nemesiss.com.lyricscroller;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import io.reactivex.subjects.BehaviorSubject;
import lombok.Getter;
import lombok.Setter;
import nemesiss.com.lyricscroller.LyricParser.View.MusicStatus;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

@Getter
public class SimpleMusicPlayer
{
    @Getter
    private MediaPlayer InnerPlayer;
    private Context mContext;
    private BehaviorSubject<MusicStatus> MusicPlayStatus = BehaviorSubject.createDefault(MusicStatus.STOP);

    private BehaviorSubject<Boolean> IsPrepared = BehaviorSubject.createDefault(false);

    @Getter
    @Setter
    private OnPlayerTimeElapsedListener TimeElapsedListener;

    private Handler TimeElapsedHandler = new Handler(this::TimeElapsedDispatcher);

    private boolean TimeElapsedDispatcher(Message message)
    {
        switch (message.what) {
            case 688: {
                if(TimeElapsedHandler != null && MusicPlayStatus.getValue() == MusicStatus.PLAY && InnerPlayer.isPlaying()) {
                    TimeElapsedListener.update(message.arg1);
                    TimeElapsedHandler.sendMessageDelayed(GetTimeElapsedMessage(),50);
                }
                break;
            }
        }
        return true;
    }

    private Message GetTimeElapsedMessage()
    {
        Message msg = new Message();
        msg.what = 688;
        msg.arg1 = InnerPlayer.getCurrentPosition();
        return msg;
    }

    public SimpleMusicPlayer(Context context)
    {
        InnerPlayer = new MediaPlayer();
        mContext = context;
        InnerPlayer.setOnSeekCompleteListener(this::OnFinishSeek);
        InnerPlayer.setOnPreparedListener(this::OnFinishPrepare);

        MusicPlayStatus.subscribe((status) -> {
            if(status == MusicStatus.PAUSE && InnerPlayer.isPlaying())
                _Pause();
            else if(status == MusicStatus.PLAY && IsPrepared.getValue()) {
                _Play();
            }
        });
    }


    private void _Play()
    {
        InnerPlayer.start();
        BeginDispatchElapsedTimeStamp();
    }
    private void _Pause()
    {
        InnerPlayer.pause();
    }


    private void OnFinishPrepare(MediaPlayer mediaPlayer)
    {
        IsPrepared.onNext(true);

        if(MusicPlayStatus.getValue() == MusicStatus.PLAY)
        {
            InnerPlayer.start();
            BeginDispatchElapsedTimeStamp();
        }
    }

    public void BeginDispatchElapsedTimeStamp()
    {
        TimeElapsedHandler.sendMessage(GetTimeElapsedMessage());
    }

    private void OnFinishSeek(MediaPlayer mp)
    {
        Log.d("SimpleMusicPlayer","完成 Seek");
    }

    public void LoadMusic(Uri uri) throws IOException
    {
        InnerPlayer.reset();
        IsPrepared.onNext(false);
        InnerPlayer.setDataSource(mContext,uri);
        InnerPlayer.prepareAsync();
    }


    public void LoadMusic(String uriString) throws IOException
    {
        LoadMusic(Uri.parse(uriString));
    }
    public void LoadMusicFromFilePath(String filePath) throws IOException
    {
        IsPrepared.onNext(false);
        InnerPlayer.reset();
        InnerPlayer.setDataSource(filePath);
        InnerPlayer.prepareAsync();
    }
    public void LoadMusic(FileInputStream fis) throws IOException
    {
        IsPrepared.onNext(false);
        InnerPlayer.reset();
        InnerPlayer.setDataSource(fis.getFD());
        InnerPlayer.prepareAsync();
    }
    public void LoadMusic(AssetFileDescriptor assetFileDescriptor) throws IOException
    {
        IsPrepared.onNext(false);
        InnerPlayer.reset();
        FileDescriptor fd = assetFileDescriptor.getFileDescriptor();
        InnerPlayer.setDataSource(fd,assetFileDescriptor.getStartOffset(),assetFileDescriptor.getLength());
        InnerPlayer.prepareAsync();
    }

    public void SeekTo(int position)
    {
        int MillsPosition = (int) (InnerPlayer.getDuration() * (((float) position) / 100));
        Log.d("SimpleMusicPlayer","开始Seek "+MillsPosition);
        InnerPlayer.seekTo(MillsPosition);
    }

    public void Play(boolean NeedLoop)
    {
        MusicPlayStatus.onNext(MusicStatus.PLAY);
    }
    public void SetLooping(boolean loop)
    {
        InnerPlayer.setLooping(loop);
    }
    public void Pause()
    {
        MusicPlayStatus.onNext(MusicStatus.PAUSE);
    }

    public interface OnPlayerTimeElapsedListener
    {
        void update(int CurrentTimeStamp);
    }

    public int GetDuration()
    {
        return InnerPlayer.getDuration();
    }

    public void SafetyDestory()
    {
        MusicPlayStatus.onNext(MusicStatus.STOP);
        IsPrepared.onNext(false);
        InnerPlayer.reset();
        InnerPlayer.release();
    }
}
