package com.babykangaroo.android.gpsdataloggerv2;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.babykangaroo.android.mydatabaselibrary.ListContract;

public class FileManagerActivity extends AppCompatActivity {

    Button testButton;

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        context = this;
        testButton = (Button) findViewById(R.id.test_button);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("MY TEST LOG", "confirmed click");
                ContentValues cv = new ContentValues();
                cv.put(ListContract.ListContractEntry.COLUMN_ITEM_NAME, "test item");
                Uri uri = getContentResolver().insert(ListContract.ListContractEntry.DIRECTORY_CONTENT_URI, cv);
                Log.v("MY TEST LOG", uri.toString());
                Toast.makeText(context, uri.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

//    @OnClick(R.id.test_button)
//    public void myMethodFOrMe(){
//        Log.v("MY TEST LOG", "confirmed click");
//        ContentValues cv = new ContentValues();
//        cv.put(ListContract.ListContractEntry.COLUMN_ITEM_NAME, "test item");
//        Uri uri = getContentResolver().insert(ListContract.ListContractEntry.DIRECTORY_CONTENT_URI, cv);
//        Log.v("MY TEST LOG", uri.toString());
//        Toast.makeText(this, uri.toString(), Toast.LENGTH_SHORT).show();
//    }
}
