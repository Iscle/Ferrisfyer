package me.iscle.ferrisfyer.activity;

import androidx.appcompat.app.AppCompatActivity;

import me.iscle.ferrisfyer.App;

public class BaseAppCompatActivity extends AppCompatActivity {

    public App getApp() {
        return (App) getApplication();
    }

}
