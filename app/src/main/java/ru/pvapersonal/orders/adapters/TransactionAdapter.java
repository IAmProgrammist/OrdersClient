package ru.pvapersonal.orders.adapters;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.model.TransItem;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public boolean reverse = false;

    private List<TransItem> mTransItems;
    private SortedList<TransItem> sortedList;
    private Resources resources;

    public TransactionAdapter(Resources res){
        mTransItems = new ArrayList<>();
        resources = res;
        sortedList = new SortedList<TransItem>(TransItem.class, new SortedList.Callback<TransItem>() {

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
            public int compare(TransItem o1, TransItem o2) {
                return reverse ? (o1.transDate.compareTo(o2.transDate)) :
                        (o2.transDate.compareTo(o1.transDate));
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(TransItem oldItem, TransItem newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areItemsTheSame(TransItem item1, TransItem item2) {
                return item1 == item2;
            }
        });
    }

    public void reverse(){
        reverse = !reverse;
        sortedList.replaceAll(mTransItems);
    }

    public void setItems(List<TransItem> items){
        mTransItems = items;
        sortedList.replaceAll(mTransItems);
        notifyDataSetChanged();
    }

    @NonNull
    @NotNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new TransactionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.trans_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull TransactionViewHolder holder, int position) {
        holder.bind(sortedList.get(position));
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public class TransactionViewHolder extends RecyclerView.ViewHolder{

        private TextView payVal;
        private TextView payComment;
        private TextView date;

        public TransactionViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            payVal = itemView.findViewById(R.id.payVal);
            payComment = itemView.findViewById(R.id.payComm);
            date = itemView.findViewById(R.id.payDate);
        }

        public void bind(TransItem item){
            payVal.setText(item.getPayString());
            payVal.setTextColor(resources.getColor(item.getColor()));
            payComment.setText(item.getComment(resources));
            date.setText(item.getDate());
        }
    }


}
