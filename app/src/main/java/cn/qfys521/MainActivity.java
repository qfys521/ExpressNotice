package cn.qfys521;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import android.app.Activity;
import com.google.gson.JsonObject;

public class MainActivity extends Activity {

    private TextView deliveryStatusTextView;
    private DeliveryStatusReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deliveryStatusTextView = findViewById(R.id.deliveryStatusTextView);

        Intent intent = new Intent(this, DeliveryStatusService.class);
        startService(intent);

        receiver = new DeliveryStatusReceiver();
        IntentFilter filter = new IntentFilter("cn.qfys521.DELIVERY_STATUS_UPDATE");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private class DeliveryStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String jsonResponse = intent.getStringExtra("jsonResponse");
            updateDeliveryStatus(jsonResponse);
        }
    }

    private void updateDeliveryStatus(String jsonResponse) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
        JsonObject result = jsonObject.getAsJsonObject("result");
        JsonArray infoArray = result.getAsJsonArray("info");

        StringBuilder statusBuilder = new StringBuilder();
        for (int i = 0; i < infoArray.size(); i++) {
            JsonObject infoObject = infoArray.get(i).getAsJsonObject();
            String time = infoObject.get("time").getAsString();
            String context = infoObject.get("context").getAsString();
            statusBuilder.append(time).append(": ").append(context).append("\n");
        }

        deliveryStatusTextView.setText(statusBuilder.toString());
    }
}