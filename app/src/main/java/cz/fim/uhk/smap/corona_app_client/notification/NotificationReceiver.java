package cz.fim.uhk.smap.corona_app_client.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import cz.fim.uhk.smap.corona_app_client.MainActivity;
import cz.fim.uhk.smap.corona_app_client.R;

// receiver pro obsluhu notifikace
public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent repeating_intent = new Intent(context, MainActivity.class);
        repeating_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 100,
                repeating_intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                context.getResources().getString(R.string.channel_id))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.menu_icon2)
                .setContentTitle("Vývoj epidemie")
                .setContentText(intent.getStringExtra("notification_text"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        notificationManager.notify(100, builder.build());
        Log.d("Notification_receiver", "notifikace vytvořena");
    }
}
