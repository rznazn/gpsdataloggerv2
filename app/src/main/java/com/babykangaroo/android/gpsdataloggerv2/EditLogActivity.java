package com.babykangaroo.android.gpsdataloggerv2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class EditLogActivity extends AppCompatActivity {

    private EditText etDeletable;
    private ImageView ivDeletable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);

        findViewById(R.id.et_new_list).setVisibility(View.GONE);
        findViewById(R.id.iv_add_new_entry).setVisibility(View.GONE);
    }
}
