package me.iscle.ferrisfyer.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import me.iscle.ferrisfyer.databinding.FragmentSelectModeCompanyBinding;

public class SelectModeFragmentCompany extends BaseFragment {

    private FragmentSelectModeCompanyBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSelectModeCompanyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.controlledButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(SelectModeFragmentCompanyDirections.actionSelectModeFragmentCompanyToDeviceControlFragment(DeviceControlFragment.Mode.REMOTE));
        });

        binding.controllerButton.setOnClickListener(v -> {
            // TODO: go to listView fragment
        });
    }
}
