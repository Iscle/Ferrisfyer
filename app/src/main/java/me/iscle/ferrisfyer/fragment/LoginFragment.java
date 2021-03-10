package me.iscle.ferrisfyer.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.ServerManager;
import me.iscle.ferrisfyer.databinding.FragmentLoginBinding;

public class LoginFragment extends BaseFragment {
    private FragmentLoginBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.login.setOnClickListener(v -> doLogin());
        binding.goToRegisterTv.setOnClickListener(v -> goToRegister());

        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        binding.username.setText(sharedPreferences.getString("username", null));
        binding.password.setText(sharedPreferences.getString("password", null));
        binding.keepLoggedIn.setChecked(sharedPreferences.getBoolean("keep_logged_in", false));

        tryAutoLogin();
    }

    private void tryAutoLogin() {
        if (sharedPreferences.getBoolean("keep_logged_in", false)) doLogin();
    }

    private void goToRegister() {
        NavDirections action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment();
        NavHostFragment.findNavController(this).navigate(action);
    }

    private void doLogin() {
        if (formHasErrors()) return;

        getFerrisfyer().getServerManager().login(binding.username.getText().toString(), binding.password.getText().toString(), loginCallback);
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

        return hasErrors;
    }

    private final ServerManager.AuthenticationCallback loginCallback = new ServerManager.AuthenticationCallback() {
        @Override
        public void onAuthenticationSuccess() {
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putBoolean("keep_logged_in", binding.keepLoggedIn.isChecked());
            sharedPreferencesEditor.putString("username", binding.username.getText().toString());
            if (binding.keepLoggedIn.isChecked()) {
                sharedPreferencesEditor.putString("password", binding.password.getText().toString());
            } else {
                sharedPreferencesEditor.remove("password");
            }
            sharedPreferencesEditor.apply();

            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(LoginFragmentDirections.actionLoginFragmentToSelectModeFragmentCompany());
        }

        @Override
        public void onAuthenticationError(String error) {
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putBoolean("keep_logged_in", false);
            sharedPreferencesEditor.apply();

            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        }
    };
}
