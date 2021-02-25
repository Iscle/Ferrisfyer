package me.iscle.ferrisfyer.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import me.iscle.ferrisfyer.Ferrisfyer;
import me.iscle.ferrisfyer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    public Ferrisfyer getFerrisfyer() {
        return (Ferrisfyer) getApplication();
    }
}
