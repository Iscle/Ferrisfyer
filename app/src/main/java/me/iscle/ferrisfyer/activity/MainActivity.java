package me.iscle.ferrisfyer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.iscle.ferrisfyer.BLEService;
import me.iscle.ferrisfyer.Ferrisfyer;
import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CHOOSE_BT_DEVICE = 65538;
    private static final int REQUEST_LOGIN = 3;

    public BLEService service = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("BLUETOOTH", "HELLOOOOOOOOO | " + requestCode + "|||" + resultCode);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, R.string.error_bluetooth_required, Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == REQUEST_CHOOSE_BT_DEVICE) {
            if (resultCode == RESULT_OK) {
                service.connectDevice(data.getStringExtra("device_address"));
            }
        }
    }

    public Ferrisfyer getFerrisfyer() {
        return (Ferrisfyer) getApplication();
    }
}
