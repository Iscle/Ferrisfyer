package me.iscle.ferrisfyer.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.activity.MainActivity;
import me.iscle.ferrisfyer.databinding.FragmentRegisterBinding;
import me.iscle.ferrisfyer.model.WebSocketCapsule;

public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().setTitle(R.string.register);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);

        binding.register.setOnClickListener(v -> doRegister());
        binding.goToLoginTv.setOnClickListener(v -> goToLogin());

        return binding.getRoot();
    }

    private void goToLogin() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    private void doRegister() {
        // TODO: MIRAR QUE ELS DOS CAMPS DE PASSWORD SIGUIN IGUALS I SINO TIRAR ERROR

        ((MainActivity) requireActivity()).getFerrisfyer().getWebSocketManager().send(WebSocketCapsule.getRegisterJson(binding.username.getText().toString(), binding.password.getText().toString()));
        goToLogin();
    }
}
