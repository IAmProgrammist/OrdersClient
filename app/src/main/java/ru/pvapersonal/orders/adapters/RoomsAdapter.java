package ru.pvapersonal.orders.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.DetailActivity;
import ru.pvapersonal.orders.activities.SetupRoomActivity;
import ru.pvapersonal.orders.model.Room;
import ru.pvapersonal.orders.model.Status;
import ru.pvapersonal.orders.service.ServerUpdateListener;

public class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.RoomsViewHolder> {

    private List<Room> mModelList;
    private SortedList<Room> sortedList;
    private Context ctx;
    private ServerUpdateListener updateListener;
    private String query = "";
    private boolean isAdmin = false;
    private static Map<Status, String> statusStrings;
    private RoomsAdapterTypes type;
    private LinearLayout titleView;

    public RoomsAdapter(Context ctx, ServerUpdateListener updateListener, boolean isAdmin, RoomsAdapterTypes type, LinearLayout titleView) {
        this.isAdmin = isAdmin;
        this.titleView = titleView;
        this.ctx = ctx;
        this.type = type;
        mModelList = new ArrayList<>();
        this.updateListener = updateListener;
        sortedList = new SortedList<>(Room.class, new SortedList.Callback<Room>() {
            @Override
            public int compare(Room o1, Room o2) {
                if (o1.isAdmin && !o2.isAdmin) {
                    return -1;
                }
                if (!o1.isAdmin && o2.isAdmin) {
                    return 1;
                }
                return (int) (o2.creationDate - o1.creationDate);
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(Room oldItem, Room newItem) {
                return oldItem.areContentsTheSame(newItem);
            }

            @Override
            public boolean areItemsTheSame(Room item1, Room item2) {
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
        statusStrings = new HashMap<>();
        if (ctx != null) {
            String[] keys = ctx.getResources().getStringArray(R.array.status_keys);
            String[] vals = ctx.getResources().getStringArray(R.array.status_values);
            for (int i = 0; i < keys.length; i++) {
                Status st = Status.valueOf(keys[i]);
                statusStrings.put(st, vals[i]);
            }
        }
    }

    public void filter(String filter) {
        query = filter;
        filter();
    }

    private void filter() {
        List<Room> sorted = new ArrayList<>();
        for (Room a : mModelList) {
            switch (type) {
                case ADMIN:
                    if (a.isAdmin) {
                        sorted.add(a);
                    }
                    break;
                case MEMBER:
                    if (a.partType == 1 && a.status != Status.NOT_SET_UP) {
                        sorted.add(a);
                    }
                    break;
                case NOT_MEMBER:
                    if (a.partType == 0 && a.status != Status.NOT_SET_UP) {
                        sorted.add(a);
                    }
                    break;
            }
        }
        if(sorted.size() == 0){
            titleView.setVisibility(View.GONE);
        }else{
            titleView.setVisibility(View.VISIBLE);
        }
            if (query == null || query.trim().equals("")) {
                sortedList.replaceAll(sorted);
            } else {
                List<Room> newList = new ArrayList<>();
                for (Room a : sorted) {
                    if (a.matchesFilter(query)) {
                        newList.add(a);
                    }
                }
                if(newList.size() == 0){
                    titleView.setVisibility(View.GONE);
                }else{
                    titleView.setVisibility(View.VISIBLE);
                }
                sortedList.replaceAll(newList);
            }

    }

    @NonNull
    @NotNull
    @Override
    public RoomsViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.room_item, parent, false);
        return new RoomsViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RoomsViewHolder holder, int position) {
        holder.bind(sortedList.get(position));
    }

    public int getPositionByRoom(Room room) {
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).equals(room)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void setItems(List<Room> items) {
        mModelList = items;
        filter();
        notifyDataSetChanged();
    }

    public void deleteRoomByID(int roomId) {
        List<Room> newList = new ArrayList<>();
        for (Room a : mModelList) {
            if (a.id != roomId) {
                newList.add(a);
            }
        }
        mModelList = newList;
        filter();
        notifyDataSetChanged();
    }

    public void editRoom(int roomId, JSONObject eventData) throws JSONException {
        //There is only room name and islocked yet
        Room m = null;
        for (Room k : mModelList) {
            if (k.id == roomId) {
                m = k;
            }
        }
        if (m != null) {
            m.name = eventData.getString("name");
            m.isLocked = eventData.getBoolean("isLocked");
            filter();
            notifyDataSetChanged();
        }
    }

    public void userExited(int roomId, boolean self) {
        //There is only room name and islocked yet
        Room m = null;
        for (Room k : mModelList) {
            if (k.id == roomId) {
                m = k;
            }
        }
        if (m != null) {
            if (self) {
                m.partType = 0;
            }
            filter();
            notifyDataSetChanged();
        }
    }

    public void userAdd(int roomId, boolean self) {
        //There is only room name and islocked yet
        Room m = null;
        for (Room k : mModelList) {
            if (k.id == roomId) {
                m = k;
            }
        }
        if (m != null) {
            if (self) {
                m.partType = 1;
            }
            filter();
            notifyDataSetChanged();
        }
    }

    public boolean add(Room room) {
        boolean duplicate;
        if (!mModelList.contains(room)) {
            duplicate = false;
            mModelList.add(room);
        } else {
            duplicate = true;
        }
        filter();
        notifyDataSetChanged();
        return duplicate;
    }

    public boolean changeStatus(int roomId, Status st, Long start, Long end) {
        boolean duplicate = true;
        Room m = null;
        for (Room g : mModelList) {
            if (g.id == roomId) {
                m = g;
                duplicate = false;
            }
        }
        if (m != null) {
            m.status = st;
            if (start != null && end != null) {
                m.start = start;
                m.end = end;
            }
        }
        filter();
        notifyDataSetChanged();
        return duplicate;
    }

    private RecyclerView mRecyclerView = null;

    @Override
    public void onAttachedToRecyclerView(@NonNull @NotNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;
    }

    public class RoomsViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView status;
        ImageView isLocked;
        View divider;
        Room m;

        public RoomsViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            divider = itemView.findViewById(R.id.divider);
            name = itemView.findViewById(R.id.room_name);
            status = itemView.findViewById(R.id.status_room);
            isLocked = itemView.findViewById(R.id.is_locked);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (m != null) {
                        if (m.status == Status.NOT_SET_UP && m.isAdmin) {
                            Intent launch = new Intent(ctx, SetupRoomActivity.class);
                            launch.putExtra(SetupRoomActivity.ROOM_ID, m.id);
                            launch.putExtra(SetupRoomActivity.MAX_MEMBERS, m.maxMembers);
                            ctx.startActivity(launch);
                        } else {
                            Intent launch = new Intent(ctx, DetailActivity.class);
                            launch.putExtra(DetailActivity.ROOM_ID, m.id);
                            ctx.startActivity(launch);
                        }
                    }
                }
            });
        }

        public void bind(Room m) {

            divider.setVisibility(View.VISIBLE);
            TypedValue outValue = new TypedValue();
            ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            itemView.setBackgroundResource(outValue.resourceId);
            this.m = m;
            switch (m.status) {
                case WAIT:
                    status.setTextColor(ContextCompat.getColor(ctx, R.color.greenish));
                    break;
                default:
                    status.setTextColor(ContextCompat.getColor(ctx, R.color.reddish));
                    break;
            }
            name.setText(m.name);
            status.setText(statusStrings.get(m.status));
            isLocked.setVisibility(m.isLocked ? View.VISIBLE : View.GONE);

        }
    }
}
