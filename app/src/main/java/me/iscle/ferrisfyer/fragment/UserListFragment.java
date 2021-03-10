package me.iscle.ferrisfyer.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import me.iscle.ferrisfyer.adapter.UserListAdapter;
import me.iscle.ferrisfyer.databinding.FragmentUserListBinding;


public class UserListFragment extends BaseFragment implements UserListAdapter.RecyclerItemClick, SearchView.OnQueryTextListener {

    private FragmentUserListBinding binding;
    private UserListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initRv();
        initListener();
    }

    private void initRv() {
        adapter = new UserListAdapter(this);
        binding.usersRv.setAdapter(adapter);
        binding.usersRv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // TODO: ASK FOR USER LIST AND ON RESPONSE DO THE FOLLOWING
        // adapter.setUsers(users);
    }

    private void initListener() {
        binding.searchSv.setOnQueryTextListener(this);
    }

    @Override
    public void itemClick(String username) {
        // TODO: ATTEMPT TO CONNECT WITH THE SELECTED USER
        // TODO: IF ATTEMPT IS SUCCESSFUL, GO TO DEVICE CONTROL FRAGMENT
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapter.filter(newText);
        return false;
    }
}
