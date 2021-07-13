package ru.pvapersonal.orders.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.model.FullUserItem;
import static ru.pvapersonal.orders.other.App.URL;

public class FullUsersAdapter extends RecyclerView.Adapter<FullUsersAdapter.FullUsersViewHolder>{

    private SortedList<FullUserItem> sortedList;
    private Context ctx;

    public FullUsersAdapter(Context ctx){
        this.ctx = ctx;
        sortedList = new SortedList<>(FullUserItem.class, new SortedList.Callback<FullUserItem>() {
            @Override
            public int compare(FullUserItem o1, FullUserItem o2) {
                return o1.compareTo(o2);
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(FullUserItem oldItem, FullUserItem newItem) {
                return oldItem.areContentsSame(newItem);
            }

            @Override
            public boolean areItemsTheSame(FullUserItem item1, FullUserItem item2) {
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
    public FullUsersViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new FullUsersViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull FullUsersViewHolder holder, int position) {
        holder.bind(sortedList.get(position));
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void updateItems(List<FullUserItem> items){
        sortedList.replaceAll(items);
    }

    public class FullUsersViewHolder extends RecyclerView.ViewHolder{

        TextView name;
        ImageView avatar;
        ImageButton phone;
        ImageButton whatsapp;
        ImageButton viber;
        FullUserItem item;

        LinearLayout lastActionCont;
        TextView lastActionText;
        LinearLayout roleCont;
        TextView roleText;

        public FullUsersViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.userName);
            avatar = itemView.findViewById(R.id.avatar);
            phone = itemView.findViewById(R.id.call_phone);
            whatsapp = itemView.findViewById(R.id.call_whatsapp);
            viber = itemView.findViewById(R.id.call_viber);
            lastActionCont = itemView.findViewById(R.id.last_action_cont);
            lastActionText = itemView.findViewById(R.id.last_action_text);
            roleCont = itemView.findViewById(R.id.is_admin_cont);
            roleText = itemView.findViewById(R.id.is_admin_text);
            phone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (item != null && ctx != null) {
                            Intent intent = new Intent(Intent.ACTION_DIAL, item.getPhoneCallUri());
                            ctx.startActivity(intent);
                        }
                    }catch (Exception e){
                        Toast.makeText(ctx, R.string.not_found_requiered_apps, Toast.LENGTH_LONG).show();
                    }
                }
            });
            whatsapp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (item != null && ctx != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, item.getWhatsAppCallUri());
                            ctx.startActivity(intent);
                        }
                    }catch (Exception e){
                        Toast.makeText(ctx, R.string.not_found_requiered_apps, Toast.LENGTH_LONG).show();
                    }
                }
            });
            viber.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (item != null && ctx != null) {
                            Intent intent = new Intent("android.intent.action.VIEW");
                            intent.setClassName("com.viber.voip", "com.viber.voip.WelcomeActivity");
                            intent.setData(item.getPhoneCallUri());
                            ctx.startActivity(intent);
                        }
                    }catch (Exception e){
                        Toast.makeText(ctx, R.string.not_found_requiered_apps, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        public void bind(FullUserItem item){
            if(item.name == null){
                name.setText(R.string.you);
            }else{
                name.setText(item.name);
            }
            if(item.image == null || ctx == null){
                avatar.setImageResource(R.drawable.ic_avatar_empty);
            }else{
                Picasso.get().load(URL + "images/" + item.image).into(avatar);
            }
            if(item.lastAction != null){
                lastActionCont.setVisibility(View.VISIBLE);
                lastActionText.setText(item.getLastActionText());
            }else{
                lastActionCont.setVisibility(View.GONE);
                lastActionText.setText("");
            }
            if(item.isAdmin!=null){
                roleCont.setVisibility(View.VISIBLE);
                roleText.setText(item.isAdmin ? R.string.admin : R.string.member);
            }else{
                roleCont.setVisibility(View.GONE);
            }
            this.item = item;
        }
    }
}
