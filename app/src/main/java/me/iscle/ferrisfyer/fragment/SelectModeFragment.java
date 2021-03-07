package me.iscle.ferrisfyer.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import me.iscle.ferrisfyer.databinding.FragmentSelectModeBinding;

public class SelectModeFragment extends BaseFragment {

    private FragmentSelectModeBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSelectModeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.localDeviceButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(SelectModeFragmentDirections.actionSelectModeFragmentToDeviceControlFragment(DeviceControlFragment.Mode.LOCAL));
        });

        binding.remoteDeviceButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(SelectModeFragmentDirections.actionSelectModeFragmentToLoginFragment());
        });
    }
}
