package cn.qfys521;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import android.icu.text.SimpleDateFormat;
import java.util.Date;
import cn.qfys521.MainActivity;
import java.net.URL;

public class DeliveryStatusService extends Service {

    private static final String CHANNEL_ID = "DeliveryStatusChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long INTERVAL = 5 * 60 * 1000; // 30 minutes

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        scheduleAlarm();
        //System.exit(-1);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fetchDeliveryStatus();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Delivery Status Channel";
            String description = "Channel for delivery status notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), INTERVAL,
                pendingIntent);
    }

    private void fetchDeliveryStatus() {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.oioweb.cn/api/common/delivery?nu=JDVB31221980735");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                connection.disconnect();
                parseAndNotify(content.toString());
            } catch (Exception e) {
                Log.e("DeliveryStatusService", "Error fetching delivery status", e);
            }
        }).start();
    }

    private void parseAndNotify(String jsonResponse) {
        // 使用Gson解析JSON并提取快递状态信息
        String notificationContent = parseStatusFromJson(jsonResponse);
        sendNotification(notificationContent);
        sendBroadcastToActivity(jsonResponse);
    }

    private String parseStatusFromJson(String jsonResponse) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
        JsonObject result = jsonObject.getAsJsonObject("result");
        JsonArray infoArray = result.getAsJsonArray("info");

        if (infoArray != null && infoArray.size() > 0) {
            JsonElement latestInfo = infoArray.get(0);
            JsonObject latestInfoObject = latestInfo.getAsJsonObject();
            String time = latestInfoObject.get("time").getAsString();
            String context = latestInfoObject.get("context").getAsString();
            return time + ": " + context;
        }

        return "未知状态";
    }

    private void sendBroadcastToActivity(String jsonResponse) {
        Intent intent = new Intent("cn.qfys521.DELIVERY_STATUS_UPDATE");
        intent.putExtra("jsonResponse", jsonResponse);
        sendBroadcast(intent);
    }

    private void sendNotification(String status) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID).setContentTitle("快递状态更新 (通知更新时间: "+ new SimpleDateFormat("HH点mm分").format(new Date()))
                .setContentText(status).setSmallIcon(android.R.drawable.ic_dialog_info).setAutoCancel(true);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public static class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent serviceIntent = new Intent(context, DeliveryStatusService.class);
            context.startService(serviceIntent);
        }
    }

}