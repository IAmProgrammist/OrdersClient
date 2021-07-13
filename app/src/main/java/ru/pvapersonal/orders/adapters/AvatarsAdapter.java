package ru.pvapersonal.orders.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.UserInfoActivity;
import ru.pvapersonal.orders.model.RoomFull;

import static ru.pvapersonal.orders.other.App.URL;

public class AvatarsAdapter extends RecyclerView.Adapter<AvatarsAdapter.AvatarsViewHolder> {
    private SortedList<RoomFull.ShortUser> sortedList;
    private Context ctx;

    public AvatarsAdapter(Context ctx){
        this.ctx = ctx;
        sortedList = new SortedList<>(RoomFull.ShortUser.class, new SortedList.Callback<RoomFull.ShortUser>() {
            @Override
            public int compare(RoomFull.ShortUser o1, RoomFull.ShortUser o2) {
                return o1.compareTo(o2);
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(RoomFull.ShortUser oldItem, RoomFull.ShortUser newItem) {
                return oldItem.areContentsSame(newItem);
            }

            @Override
            public boolean areItemsTheSame(RoomFull.ShortUser item1, RoomFull.ShortUser item2) {
                return item1 == item2;
            }

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
        });
    }

    @NonNull
    @NotNull
    @Override
    public AvatarsViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new AvatarsViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.short_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull AvatarsViewHolder holder, int position) {
        holder.bind(sortedList.get(position));
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void updateItems(List<RoomFull.ShortUser> items){
        sortedList.replaceAll(items);
    }

    public class AvatarsViewHolder extends RecyclerView.ViewHolder{
        ImageView avatar;
        TextView name;

        RoomFull.ShortUser shortUser = null;
        public AvatarsViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.nickname);
            avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (shortUser != null) {
                        Intent intent = new Intent(ctx, UserInfoActivity.class);
                        intent.putExtra(UserInfoActivity.USER_ID, shortUser.userId);
                        ctx.startActivity(intent);
                    }
                }
            });
        }

        public void bind(RoomFull.ShortUser info){
            shortUser = info;
            if(info.avatar == null){
                avatar.setImageResource(R.drawable.ic_avatar_empty);
            }else{
                Picasso.get().load(URL + "images/" + info.avatar).into(avatar);
            }
            if(info.self){
                name.setText(ctx.getString(R.string.you));
            }else{
                name.setText(info.shortName);
            }
        }
    }
}
