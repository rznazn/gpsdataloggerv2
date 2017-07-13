package com.babykangaroo.android.gpsdataloggerv2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class LoggingActivity extends AppCompatActivity {

    private TextView tvLogEvent;
    private TextView tvCurrentLogName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);

        tvLogEvent = (TextView) findViewById(R.id.tv_log_event);
        tvCurrentLogName = (TextView) findViewById(R.id.tv_current_log_name);
        Intent intent = getIntent();
        tvCurrentLogName.setText(intent.getStringExtra("log name"));

    }
}
