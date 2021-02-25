package me.iscle.ferrisfyer.activity;

import androidx.appcompat.app.AppCompatActivity;

import me.iscle.ferrisfyer.Ferrisfyer;

public class BaseActivity extends AppCompatActivity {

    public Ferrisfyer getFerrisfyer() {
        return (Ferrisfyer) getApplication();
    }

}
