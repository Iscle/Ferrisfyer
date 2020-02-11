package me.iscle.ferrisfyer;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    public static final String SERVICE_CHANNEL_ID = "me.iscle.ferrysfier.notification.SERVICE_CHANNEL";
    public static final String BROADCAST_SEND_COMMAND = "me.iscle.ferrysfier.broadcast.SEND_COMMAND";

    private Mode mode = Mode.UNDEFINED;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannels();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    private void createNotificationChannels() {
        // Create the NotificationChannels if we are on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(SERVICE_CHANNEL_ID, "Services", NotificationManager.IMPORTANCE_LOW);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    public enum Mode {
        UNDEFINED,
        LOCAL,
        REMOTE
    }
}
