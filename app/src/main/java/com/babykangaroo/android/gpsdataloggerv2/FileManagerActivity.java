package com.babykangaroo.android.gpsdataloggerv2;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
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
        tvDestinationIp.setText(sharedPreferences.getString(getString(R.string.destination_ip), getString(R.string.destination_ip)));
        tvDestinationIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDestinationIp();
            }
        });

        tvDestinationPort = (TextView) findViewById(R.id.tv_port);
        tvDestinationPort.setText(sharedPreferences.getString(getString(R.string.destination_port), getString(R.string.destination_port)));
        tvDestinationPort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDestinationPort();
            }
        });

        swLiveUpdates = (Switch) findViewById(R.id.sw_live_tracking);
        swLiveUpdates.setChecked(sharedPreferences.getBoolean(getString(R.string.live_updates), false));
        swLiveUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleLiveUpdates();
            }
        });

        swMinimiedTracking = (Switch) findViewById(R.id.sw_minimized_tracking);
        swMinimiedTracking.setChecked(sharedPreferences.getBoolean(getString(R.string.minimized_tracking), false));
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

    void setDestinationIp(){
        final View adView = getLayoutInflater().inflate(R.layout.log_event_alert_dialog, null);
        final TextView tvMessage = (TextView) adView.findViewById(R.id.tv_event_summary);
        tvMessage.setText("Enter Destinaton IP");
        final EditText etIP = (EditText) adView.findViewById(R.id.et_event_note);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(adView);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String destinationIp = etIP.getText().toString();
                tvDestinationIp.setText(destinationIp);
                sharedPreferences.edit().putString(getString(R.string.destination_ip), destinationIp).apply();
                }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    void setDestinationPort(){
        final View adView = getLayoutInflater().inflate(R.layout.log_event_alert_dialog, null);
        final TextView tvMessage = (TextView) adView.findViewById(R.id.tv_event_summary);
        tvMessage.setText("Enter Destinaton IP");
        final EditText etPort = (EditText) adView.findViewById(R.id.et_event_note);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(adView);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String destinationPort = etPort.getText().toString();
                tvDestinationPort.setText(destinationPort);
                sharedPreferences.edit().putString(getString(R.string.destination_port), destinationPort).apply();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    void toggleLiveUpdates(){
        sharedPreferences.edit().putBoolean(getString(R.string.live_updates), swLiveUpdates.isChecked()).apply();
    }

    void toggleMinimizedTracking(){
        sharedPreferences.edit().putBoolean(getString(R.string.minimized_tracking), swMinimiedTracking.isChecked()).apply();
    }

    void changeAdminPassword(){
        final View adView = getLayoutInflater().inflate(R.layout.password_dialog, null);
        final TextView tvMessage = (TextView) adView.findViewById(R.id.tv_event_summary);
        tvMessage.setText("Enter New Admin Password");
        final EditText etAdminPassword = (EditText) adView.findViewById(R.id.et_event_note);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(adView);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String adminPassword = etAdminPassword.getText().toString();
                tvDestinationPort.setText(adminPassword);
                sharedPreferences.edit().putString(getString(R.string.admin_password), adminPassword).apply();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


}
