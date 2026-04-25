package com.example.myapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomeActivity extends AppCompatActivity implements DataClient.OnDataChangedListener {

    private static final String HEART_RATE_PATH = "/heart_rate_data_path";
    private TextView heartRateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        heartRateTextView = findViewById(R.id.heart_rate_text_view);

        DataClient dataClient = Wearable.getDataClient(this);
        dataClient.addListener(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check if the event's URI path matches your data path
                if ("/heart_rate_data_path".equals(event.getDataItem().getUri().getPath())) {
                    // Extract data from DataItem
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    DataMap dataMap = dataMapItem.getDataMap();
                    float heartRate = dataMap.getFloat("heart_rate");
                    Log.d("HeartRateLog", "Heart rate: " + heartRate);
                    String heartRateText = Float.toString(heartRate);
                    heartRateTextView.setText(heartRateText);




                    // Update the UI on the main thread
//                    runOnUiThread(() -> {
//                        Log.d("HeartRateLog", "Updating UI with heart rate: " + heartRate);
//                        String heartRateText = getString(R.string.heart_rate_text, heartRate);
//                        heartRateTextView.setText(heartRateText);
//                    });
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DataClient dataClient = Wearable.getDataClient(this);
        dataClient.removeListener(this);
    }
}

