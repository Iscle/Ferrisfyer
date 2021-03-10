package me.iscle.ferrisfyer.adapter;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.iscle.ferrisfyer.R;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {
    private List<String> users;
    private List<String> originalsUsers;
    private RecyclerItemClick itemClick;

    public UserListAdapter(RecyclerItemClick itemClick) {
        this.users = new ArrayList<>();
        this.originalsUsers = new ArrayList<>();
        this.itemClick = itemClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        holder.usernameTv.setText(users.get(position));

        holder.itemView.setOnClickListener(v -> itemClick.itemClick(users.get(position)));
    }

    @Override
    public int getItemCount() {
        return users == null ? 0 : users.size();
    }

    public void filter(final String strSearch) {
        if (strSearch.length() == 0) {
            users.clear();
            users.addAll(originalsUsers);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                users.clear();
                List<String> collect = originalsUsers.stream()
                        .filter(i -> i.toLowerCase().contains(strSearch))
                        .collect(Collectors.toList());

                users.addAll(collect);
            } else {
                users.clear();
                for (String i : originalsUsers) {
                    if (i.toLowerCase().contains(strSearch)) {
                        users.add(i);
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView usernameTv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            usernameTv = itemView.findViewById(R.id.username_tv);
        }
    }

    public interface RecyclerItemClick {
        void itemClick(String username);
    }
}
