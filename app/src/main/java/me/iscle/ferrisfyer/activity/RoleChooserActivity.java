package me.iscle.ferrisfyer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import me.iscle.ferrisfyer.App;
import me.iscle.ferrisfyer.R;

public class RoleChooserActivity extends BaseAppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_REMOTE_LOGIN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_chooser);

        findViewById(R.id.local_device_button).setOnClickListener(this);
        findViewById(R.id.remote_device_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.local_device_button:
                getApp().setMode(App.Mode.LOCAL);
                Intent mainActivityIntent = new Intent(this, LocalControlActivity.class);
                startActivity(mainActivityIntent);
                finishAffinity();
                break;
            case R.id.remote_device_button:
                getApp().setMode(App.Mode.REMOTE);
                Intent loginActivityIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginActivityIntent, REQUEST_REMOTE_LOGIN);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_REMOTE_LOGIN && resultCode == RESULT_OK) {
            Intent i = new Intent(this, RemoteControlActivity.class);
            startActivity(i);
            finishAffinity();
        }
    }
}
