package ru.pvapersonal.orders.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.UserInfoActivity;
import ru.pvapersonal.orders.model.QueueItem;
import ru.pvapersonal.orders.service.ServerUpdateListener;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueAdapterViewHolder> {

    private SortedList<QueueItem> sortedList;
    private List<QueueItem> mModelList;
    public Context ctx;
    private ServerUpdateListener updateListener;


    public QueueAdapter(Context ctx, ServerUpdateListener updateListener) {
        this.sortedList = new SortedList<QueueItem>(QueueItem.class, new SortedList.Callback<QueueItem>() {
            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }

            @Override
            public int compare(QueueItem o1, QueueItem o2) {
                if(o1.self && !o2.self){
                    return -1;
                }
                if(!o1.self && o2.self){
                    return 1;
                }
                if (o1.enabled && !o2.enabled) {
                    return -1;
                }
                if (!o1.enabled && o2.enabled) {
                    return 1;
                }
                return o1.registartionDate.compareTo(o2.registartionDate);
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(QueueItem oldItem, QueueItem newItem) {
                return oldItem.areContentsSame(newItem);
            }

            @Override
            public boolean areItemsTheSame(QueueItem item1, QueueItem item2) {
                return item1.areItemsTheSame(item2);
            }
        });
        this.ctx = ctx;
        this.updateListener = updateListener;
    }

    @NonNull
    @NotNull
    @Override
    public QueueAdapterViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new QueueAdapterViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.queue_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull QueueAdapterViewHolder holder, int position) {
        holder.bind(sortedList.get(position));
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void setItems(List<QueueItem> queueList) {
        mModelList = queueList;
        filter();
    }

    public void toggleItem(int userId, boolean enabled, Long date) {
        for (int i = 0; i < mModelList.size(); i++) {
            if (mModelList.get(i).userId == userId) {
                mModelList.get(i).enabled = enabled;
                if(date != null){
                    mModelList.get(i).registartionDate = date;
                }
            }
        }
        filter();
    }

    public void addItem(QueueItem item) {
        mModelList.add(item);
        filter();
    }

    public void removeById(int userId) throws JSONException {
        //This is horrible, should change!
        mModelList.remove(new QueueItem(userId));
        filter();
    }

    public void filter(){
        List<QueueItem> newList = new ArrayList<>();
        for(QueueItem it : mModelList){
            if(it.enabled || it.self){
                newList.add(it);
            }
        }
        sortedList.replaceAll(newList);
        notifyDataSetChanged();
    }

    public class QueueAdapterViewHolder extends RecyclerView.ViewHolder {

        TextView userName;
        LinearLayout date;
        TextView enabled;
        QueueItem item;

        public QueueAdapterViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            //date = itemView.findViewById(R.id.wait_since_user);
            date = itemView.findViewById(R.id.linearLayout6);
            enabled = itemView.findViewById(R.id.status_user);
            userName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(item != null){
                        Intent launch = new Intent(ctx, UserInfoActivity.class);
                        launch.putExtra(UserInfoActivity.USER_ID, item.userId);
                        ctx.startActivity(launch);
                    }
                }
            });
        }

        public void bind(QueueItem item) {
            this.item = item;
            if(item.self){
                userName.setText(R.string.you);
            }else {
                userName.setText(item.userName);
            }
            if(item.enabled){
                enabled.setText(R.string.status_queue_wait);
                date.setVisibility(View.VISIBLE);
                ((TextView)date.findViewById(R.id.wait_since_user)).setText(item.getTime());
            }else{
                enabled.setText(R.string.status_queue_not_wait);
                date.setVisibility(View.GONE);
            }
        }
    }
}
