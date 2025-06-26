package com.example.reminderapp;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public  class NotificationReceiver extends BroadcastReceiver {
    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String desc = intent.getStringExtra("desc");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminderChannel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(desc)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
