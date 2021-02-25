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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);

        requireActivity().setTitle(R.string.register);

        binding.register.setOnClickListener(v -> doRegister());
        binding.goToLoginTv.setOnClickListener(v -> goToLogin());

        return binding.getRoot();
    }

    private void goToLogin() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    private void doRegister() {
        if (formHasErrors()) {
            return;
        }

        ((MainActivity) requireActivity()).getFerrisfyer().getWebSocketManager().send(WebSocketCapsule.getRegisterJson(binding.username.getText().toString(), binding.password.getText().toString()));
        goToLogin();
    }

    private boolean formHasErrors() {
        boolean hasErrors = false;
        String username = binding.username.getText().toString();
        String password = binding.password.getText().toString();
        String passwordConfirm = binding.passwordConfirm.getText().toString();

        if (username.isEmpty()) {
            binding.username.setError(getResources().getString(R.string.field_cant_be_empty));
            hasErrors = true;
        }

        if (password.isEmpty()) {
            binding.password.setError(getResources().getString(R.string.field_cant_be_empty));
            hasErrors = true;
        }

        if (passwordConfirm.isEmpty()) {
            binding.passwordConfirm.setError(getResources().getString(R.string.field_cant_be_empty));
            hasErrors = true;
        }

        if (!password.isEmpty() && !passwordConfirm.isEmpty() && !password.equals(passwordConfirm)) {
            binding.password.setError(getResources().getString(R.string.passwords_must_match));
            binding.passwordConfirm.setError(getResources().getString(R.string.passwords_must_match));
            hasErrors = true;
        }

        return hasErrors;
    }
}
