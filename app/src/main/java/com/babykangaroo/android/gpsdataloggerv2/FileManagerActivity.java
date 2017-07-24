package com.babykangaroo.android.gpsdataloggerv2;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.babykangaroo.android.mydatabaselibrary.ListContract;

public class FileManagerActivity extends AppCompatActivity implements MyCursorAdapter.ListItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>{

    private ImageView ivEnterNewEntry;
    private EditText etNewEntry;
    private RecyclerView rvLogList;
    private MyCursorAdapter mAdapter;
    private TextView tvDestinationIp;
    private TextView tvDestinationPort;
    private Switch swLiveUpdates;
    private Switch swMinimiedTracking;
    private TextView tvAdminPassword;
    private SharedPreferences sharedPreferences;

    private static final int LOADER_ID = 9998;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        context = this;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        etNewEntry = (EditText) findViewById(R.id.et_new_list);
        rvLogList = (RecyclerView) findViewById(R.id.rv_list_directory);
        mAdapter = new MyCursorAdapter(this, this);
        rvLogList.setAdapter(mAdapter);
        ivEnterNewEntry = (ImageView) findViewById(R.id.iv_add_new_entry);
        /**
         * enter new log name into the "directory" table
         */
        ivEnterNewEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String itemName = etNewEntry.getText().toString();
                if (!itemName.matches("")) {
                    ContentValues cv = new ContentValues();
                    cv.put(ListContract.ListContractEntry.COLUMN_LOG_NAME, itemName);
                    Uri uri = getContentResolver().insert(ListContract.ListContractEntry.DIRECTORY_CONTENT_URI, cv);
                    etNewEntry.setText("");
                }
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvLogList.setLayoutManager(layoutManager);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        getLoaderManager().restartLoader(LOADER_ID, null, this);

        tvDestinationIp = (TextView) findViewById(R.id.tv_ip_address);
        tvDestinationIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDestinationIp();
            }
        });

        tvDestinationPort = (TextView) findViewById(R.id.tv_port);
        tvDestinationPort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDestinationPort();
            }
        });

        swLiveUpdates = (Switch) findViewById(R.id.sw_live_tracking);
        swLiveUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleLiveUpdates();
            }
        });

        swMinimiedTracking = (Switch) findViewById(R.id.sw_minimized_tracking);
        swMinimiedTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleMinimizedTracking();
            }
        });

        tvAdminPassword = (TextView) findViewById(R.id.tv_admin_password);
        tvAdminPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeAdminPassword();
            }
        });
    }

    @Override
    public void onItemClick(long itemCursorID, String itemName) {
        sharedPreferences.edit().putString(getString(R.string.current_log), itemName).apply();
        Intent intent = new Intent(this, LoggingActivity.class);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                ListContract.ListContractEntry.DIRECTORY_CONTENT_URI,
                null,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    void setDestinationIp(){}

    void setDestinationPort(){}

    void toggleLiveUpdates(){
    }

    void toggleMinimizedTracking(){}

    void changeAdminPassword(){}

}
