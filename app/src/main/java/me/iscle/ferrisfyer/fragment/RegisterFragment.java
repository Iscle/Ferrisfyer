package me.iscle.ferrisfyer.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.ServerManager;
import me.iscle.ferrisfyer.databinding.FragmentRegisterBinding;

public class RegisterFragment extends BaseFragment {
    private FragmentRegisterBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.register.setOnClickListener(v -> doRegister());
        binding.goToLoginTv.setOnClickListener(v -> goToLogin());
    }

    private void goToLogin() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    private void doRegister() {
        if (formHasErrors()) return;

        getFerrisfyer().getServerManager().register(binding.username.getText().toString(), binding.password.getText().toString(), registerCallback);
    }

    private boolean formHasErrors() {
        boolean hasErrors = false;

        String username = binding.username.getText().toString();
        if (username.isEmpty()) {
            binding.username.setError(getResources().getString(R.string.field_cant_be_empty));
            hasErrors = true;
        }

        String password = binding.password.getText().toString();
        if (password.isEmpty()) {
            binding.password.setError(getResources().getString(R.string.field_cant_be_empty));
            hasErrors = true;
        }

        String passwordConfirm = binding.passwordConfirm.getText().toString();
        if (passwordConfirm.isEmpty()) {
            binding.passwordConfirm.setError(getResources().getString(R.string.field_cant_be_empty));
            hasErrors = true;
        }

        if (!password.isEmpty() && !passwordConfirm.isEmpty() && !password.equals(passwordConfirm)) {
            binding.passwordConfirm.setError(getResources().getString(R.string.passwords_must_match));
            hasErrors = true;
        }

        return hasErrors;
    }

    private final ServerManager.AuthenticationCallback registerCallback = new ServerManager.AuthenticationCallback() {
        @Override
        public void onAuthenticationSuccess() {
            goToLogin();
        }

        @Override
        public void onAuthenticationError(String error) {
            Toast.makeText(RegisterFragment.this.getContext(), error, Toast.LENGTH_SHORT).show();
        }
    };
}
