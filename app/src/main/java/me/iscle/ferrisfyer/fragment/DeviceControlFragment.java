package me.iscle.ferrisfyer.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import me.iscle.ferrisfyer.BLEService;
import me.iscle.ferrisfyer.IDeviceControl;
import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.databinding.FragmentDeviceControlBinding;

public class DeviceControlFragment extends Fragment {

    private FragmentDeviceControlBinding binding;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CHOOSE_BT_DEVICE = 2;
    private static final int REQUEST_LOGIN = 3;

    private byte lastSliderValue = -1;
    private IDeviceControl deviceControl;
    private BLEService service = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceControlBinding.inflate(inflater, container, false);

        requireActivity().setTitle(R.string.app_name);

        // TODO: IMPLEMENT

        return binding.getRoot();
    }

    private void onSliderChange() {
        // TODO: modificar dataset

        binding.chart.notifyDataSetChanged();
        binding.chart.invalidate();
    }
}
