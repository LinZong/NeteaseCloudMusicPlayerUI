package nemesiss.com.lyricscroller.LyricParser.Adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import nemesiss.com.lyricscroller.LyricParser.Model.LyricInfo;
import nemesiss.com.lyricscroller.R;

public class LyricRecycleAdapter extends RecyclerView.Adapter<LyricRecycleAdapter.LyricRecycleViewHolder>
{

    private LyricInfo _lyricInfo;

    public LyricRecycleAdapter(LyricInfo lyricInfo)
    {
        _lyricInfo = lyricInfo;
    }

    @NonNull
    @Override
    public LyricRecycleViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.lyric_sentence,viewGroup,false);
        return new LyricRecycleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LyricRecycleViewHolder lyricVH, int i)
    {
        lyricVH.Sentence.setText(_lyricInfo.getSentences().get(i).getText());
    }

    @Override
    public int getItemCount()
    {
        return _lyricInfo.getSentences().size();
    }

    class LyricRecycleViewHolder extends RecyclerView.ViewHolder
    {
        @BindView(R.id.LyricRecycle_Sentence)
        public TextView Sentence;

        LyricRecycleViewHolder(@NonNull View itemView)
        {
            super(itemView);
            ButterKnife.bind(this,itemView);
            setIsRecyclable(false);
        }
    }
}