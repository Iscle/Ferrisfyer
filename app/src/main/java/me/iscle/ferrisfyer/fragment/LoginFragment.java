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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.WebSocketManager;
import me.iscle.ferrisfyer.activity.MainActivity;
import me.iscle.ferrisfyer.databinding.FragmentLoginBinding;
import me.iscle.ferrisfyer.model.WebSocketCapsule;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().setTitle(R.string.login);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);

        binding.login.setOnClickListener(v -> doLogin());
        binding.goToRegisterTv.setOnClickListener(v -> goToRegister());

        sharedPreferences = requireActivity().getSharedPreferences("me.iscle.ferrisfyer.LoginPreferences", Context.MODE_PRIVATE);
        binding.username.setText(sharedPreferences.getString("username", null));
        binding.password.setText(sharedPreferences.getString("password", null));
        binding.keepLoggedIn.setChecked(sharedPreferences.getBoolean("keep_logged_in", false));

        if (!((MainActivity) requireActivity()).getFerrisfyer().getWebSocketManager().isOpened()) {
            ((MainActivity) requireActivity()).getFerrisfyer().getWebSocketManager().openWebSocket(webSocketCallback);
        } else {
            tryAutoLogin();
        }

        return binding.getRoot();
    }

    private void tryAutoLogin() {
        if (sharedPreferences.getBoolean("keep_logged_in", false)) {
            doLogin();
        }
    }

    private void goToRegister() {
        NavDirections action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment();
        NavHostFragment.findNavController(this).navigate(action);
    }

    private void doLogin() {
        ((MainActivity) requireActivity()).getFerrisfyer().getWebSocketManager().send(WebSocketCapsule.getLoginJson(binding.username.getText().toString(), binding.password.getText().toString()));
    }

    private void onLoginSuccess() {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("keep_logged_in", binding.keepLoggedIn.isChecked());
        sharedPreferencesEditor.putString("username", binding.username.getText().toString());
        if (binding.keepLoggedIn.isChecked()) {
            sharedPreferencesEditor.putString("password", binding.password.getText().toString());
        } else {
            sharedPreferencesEditor.remove("password");
        }
        sharedPreferencesEditor.apply();

        NavDirections action = LoginFragmentDirections.actionLoginFragmentToDeviceControlFragment();
        NavHostFragment.findNavController(this).navigate(action);
    }

    private void onLoginError(String errorString) {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("keep_logged_in", false);
        sharedPreferencesEditor.apply();

        Toast.makeText(requireContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private final WebSocketManager.WebSocketCallback webSocketCallback = new WebSocketManager.WebSocketCallback() {
        @Override
        public void onConnect() {
            requireActivity().runOnUiThread(() -> {
                tryAutoLogin();
            });
        }

        @Override
        public void onDisconnect() {
            requireActivity().runOnUiThread(() -> {
                onLoginError("Disconnected from the server!");
            });
        }

        @Override
        public void onAuthenticateResponse(boolean success, String message) {
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    onLoginSuccess();
                } else {
                    onLoginError(message);
                }
            });
        }

        @Override
        public void onError(String error) {
            requireActivity().runOnUiThread(() -> {
                onLoginError(error);
            });
        }
    };
}
