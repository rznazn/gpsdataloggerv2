package com.babykangaroo.android.gpsdataloggerv2;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.babykangaroo.android.mydatabaselibrary.ListContract;
import com.babykangaroo.android.mylocationlibrary.LocationAccess;

public class FileManagerActivity extends AppCompatActivity {

    private ImageView ivEnterNewEntry;
    private EditText etNewEntry;
    private LocationAccess mLocationAccess;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        context = this;
        mLocationAccess = new LocationAccess(context);
        etNewEntry = (EditText) findViewById(R.id.et_new_list);
        ivEnterNewEntry = (ImageView) findViewById(R.id.iv_add_new_entry);
        ivEnterNewEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String itemName = etNewEntry.getText().toString();
                if (!itemName.matches("")) {
                    ContentValues cv = new ContentValues();
                    cv.put(ListContract.ListContractEntry.COLUMN_LOG_NAME, itemName);
                    Uri uri = getContentResolver().insert(ListContract.ListContractEntry.DIRECTORY_CONTENT_URI, cv);
                    etNewEntry.setText("");
                    Toast.makeText(context, uri.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

        /**
         * TODO remove test button when ready
         */
        testButton = (ImageView) findViewById(R.id.test_button);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("MY TEST LOG", "confirmed click");
                double latt = mLocationAccess.getmBearingMagnetic();
                Toast.makeText(context, String.valueOf(latt), Toast.LENGTH_SHORT).show();
            }
        });
    }

//    @OnClick(R.id.test_button)
//    public void myMethodFOrMe(){
//        Log.v("MY TEST LOG", "confirmed click");
//        ContentValues cv = new ContentValues();
//        cv.put(ListContract.ListContractEntry.COLUMN_LOG_NAME, "test item");
//        Uri uri = getContentResolver().insert(ListContract.ListContractEntry.DIRECTORY_CONTENT_URI, cv);
//        Log.v("MY TEST LOG", uri.toString());
//        Toast.makeText(this, uri.toString(), Toast.LENGTH_SHORT).show();
//    }
}
