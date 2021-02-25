package me.iscle.ferrisfyer.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import me.iscle.ferrisfyer.Ferrisfyer;
import me.iscle.ferrisfyer.activity.MainActivity;
import me.iscle.ferrisfyer.databinding.FragmentSelectModeBinding;

public class SelectModeFragment extends Fragment {

    private FragmentSelectModeBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSelectModeBinding.inflate(inflater, container, false);

        binding.localDeviceButton.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getFerrisfyer().setMode(Ferrisfyer.Mode.LOCAL);

            NavDirections action = SelectModeFragmentDirections.actionSelectModeFragmentToDeviceControlFragment();
            NavHostFragment.findNavController(this).navigate(action);
        });

        binding.remoteDeviceButton.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getFerrisfyer().setMode(Ferrisfyer.Mode.REMOTE);

            NavDirections action = SelectModeFragmentDirections.actionSelectModeFragmentToLoginFragment();
            NavHostFragment.findNavController(this).navigate(action);
        });

        return binding.getRoot();
    }
}
