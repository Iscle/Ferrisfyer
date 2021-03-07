package me.iscle.ferrisfyer;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class Ferrisfyer extends Application {
    public static final String SERVICE_CHANNEL_ID = "me.iscle.ferrisfyer.notification.SERVICE_CHANNEL";

    private ServerManager serverManager;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannels();
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

    public ServerManager getServerManager() {
        if (serverManager == null) {
            synchronized (this) {
                if (serverManager == null) {
                    serverManager = new ServerManager();
                }
            }
        }

        return serverManager;
    }
}
