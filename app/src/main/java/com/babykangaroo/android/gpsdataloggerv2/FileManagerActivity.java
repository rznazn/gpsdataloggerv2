package com.babykangaroo.android.gpsdataloggerv2;

import android.Manifest;
import android.app.Activity;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.babykangaroo.android.mydatabaselibrary.ListContract;
import com.example.WamFormater;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

public class FileManagerActivity extends AppCompatActivity implements MyCursorAdapter.ListItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>{

    private ImageView ivEnterNewEntry;
    private EditText etNewEntry;
    private RecyclerView rvLogList;
    private MyCursorAdapter mAdapter;
    private TextView tvTrackId;
    private TextView tvLogInterval;
    private TextView tvDestinationIp;
    private TextView tvDestinationPort;
    private Switch swLiveUpdates;
    private Switch swMinimiedTracking;
    private TextView tvAdminPassword;

    private FrameLayout loadingIndicatorView;
    private LinearLayout mainContent;

    private SharedPreferences sharedPreferences;

    private static final int LOADER_ID = 9998;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        context = this;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        loadingIndicatorView = (FrameLayout) findViewById(R.id.cp_loading);
        mainContent = (LinearLayout) findViewById(R.id.ll_main_content);
        etNewEntry = (EditText) findViewById(R.id.et_new_list);
        // Allow addition of new log through the DONE input - Carte - Jun 2020
        etNewEntry.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    createNewLog();
                    return true;
                }
                return false;
            }
        });

        rvLogList = (RecyclerView) findViewById(R.id.rv_list_directory);
        mAdapter = new MyCursorAdapter(this, this, sharedPreferences);
        rvLogList.setAdapter(mAdapter);
        ivEnterNewEntry = (ImageView) findViewById(R.id.iv_add_new_entry);
        /**
         * enter new log name into the "directory" table
         */
        ivEnterNewEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Created separate createNewLog() function to share functionality with the built-in DONE input
                String itemName = etNewEntry.getText().toString();
                if (!itemName.matches("") && !itemName.contains("/")) {
                    ContentValues cv = new ContentValues();
                    cv.put(ListContract.ListContractEntry.COLUMN_LOG_NAME, itemName);
                    Uri uri = getContentResolver().insert(ListContract.ListContractEntry.DIRECTORY_CONTENT_URI, cv);
                    etNewEntry.setText("");

                    // once the new entry is added, close the keyboard
                    closeKeyboard();
                } else {
                    Toast toast = Toast.makeText(context, "invalid log name", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP,0 ,0);
                    toast.show();
                }
                // */
                createNewLog();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvLogList.setLayoutManager(layoutManager);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        getLoaderManager().restartLoader(LOADER_ID, null, this);

        tvTrackId = (TextView) findViewById(R.id.tv_track_id);
        tvTrackId.setText(sharedPreferences.getString(getString(R.string.track_id), getString(R.string.default_track_id)));
        tvTrackId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setTrackId();
            }
        });

        tvLogInterval = (TextView) findViewById(R.id.tv_log_interval);
        tvLogInterval.setText(String.valueOf(sharedPreferences.getInt(getString(R.string.log_interval), getResources().getInteger(R.integer.default_log_interval))));
        tvLogInterval.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLogInterval();
            }
        });
        tvDestinationIp = (TextView) findViewById(R.id.tv_ip_address);
        tvDestinationIp.setText(sharedPreferences.getString(getString(R.string.destination_ip), getString(R.string.default_ip)));
        tvDestinationIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDestinationIp();
            }
        });

        tvDestinationPort = (TextView) findViewById(R.id.tv_port);
        tvDestinationPort.setText(sharedPreferences.getString(getString(R.string.destination_port), getString(R.string.default_port)));
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
        tvAdminPassword.setText(sharedPreferences.getString(getString(R.string.admin_password), getString(R.string.default_admin_password)));
        tvAdminPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeAdminPassword();
            }
        });
    }

    @Override
    public void onItemClick(final long itemCursorID, final String itemName) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Select option for " + itemName);
        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sharedPreferences.edit().putString(getString(R.string.current_log), itemName).apply();
                Intent intent = new Intent(context, LoggingActivity.class);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Export Log", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showLoading();
                //***add another pop up in here asking whether to make WAM files or CSV files
                WriteToWamAsyncTask asyncTask = new WriteToWamAsyncTask();
                asyncTask.execute(itemName);
            }
        });
        builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                builder1.setMessage("DELETING " + itemName + " PLEASE CONFIRM");
                builder1.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int logDeleted = context.getContentResolver().delete(ListContract.ListContractEntry.DIRECTORY_CONTENT_URI,
                                ListContract.ListContractEntry._ID+ " = ? ",
                                new String[]{String.valueOf(itemCursorID)});
                        int itemsDeleted = context.getContentResolver().delete(ListContract.ListContractEntry.ITEMS_CONTENT_URI,
                                ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST + " = ? ",
                                new String[]{itemName});
                        Log.v("FILE MANAGER", logDeleted + " / " + itemsDeleted);
                        if (itemName.equals(sharedPreferences.getString(getString(R.string.current_log), "default"))) {
                            sharedPreferences.edit().remove(getString(R.string.current_log)).apply();
                        }
                    }
                });
                builder1.setNegativeButton("cancel", null);

                AlertDialog ad = builder1.create();
                ad.show();
            }
        });

        AlertDialog ad = builder.create();
        ad.show();
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

    void setTrackId(){
        final View adView = getLayoutInflater().inflate(R.layout.log_event_alert_dialog, null);
        final TextView tvMessage = (TextView) adView.findViewById(R.id.tv_event_summary);
        tvMessage.setText("Enter Track Id");
        final EditText etTrackId = (EditText) adView.findViewById(R.id.et_event_note);
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
                String trackId = etTrackId.getText().toString();
                tvTrackId.setText(trackId);
                sharedPreferences.edit().putString(getString(R.string.track_id), trackId).apply();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    void setLogInterval(){
        final View adView = getLayoutInflater().inflate(R.layout.log_event_alert_dialog, null);
        final TextView tvMessage = (TextView) adView.findViewById(R.id.tv_event_summary);
        tvMessage.setText("Enter Logging Interval (seconds)");
        final EditText etInterval = (EditText) adView.findViewById(R.id.et_event_note);
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
                int interval = Integer.valueOf(etInterval.getText().toString());
                tvLogInterval.setText(String.valueOf(interval));
                sharedPreferences.edit().putInt(getString(R.string.log_interval), interval).apply();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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
        tvMessage.setText("Enter Destinaton Port");
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
        final View adView = getLayoutInflater().inflate(R.layout.log_event_alert_dialog, null);
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
                tvAdminPassword.setText(adminPassword);
                sharedPreferences.edit().putString(getString(R.string.admin_password), adminPassword).apply();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    void exportToCSV(String logName){
        String fileType = "_CSV";
        Cursor cursor = context.getContentResolver().query(ListContract.ListContractEntry.ITEMS_CONTENT_URI,
                null,
                ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST + " = ? ",
                new String[]{logName},
                null);

        final String headerStr = "keyword, user_action, was_cancelled, event_time, track_number, action_name, event_latitude, " +
                           "event_longitude, event_altitude, figure_color, bearing_magnetic, bearing_from_last, event_range, " +
                           "end_time, gps_accuracy, gps_speed, note\n";
        String log = headerStr;

        while (cursor.moveToNext()) {
            String typeOfEvent = cursor.getString((cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_TYPE)));
            String keyword = cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD));

            if (typeOfEvent.equals("ENGAGE STARTED")) {
                keyword = "POINT";
            } else if (typeOfEvent.equals("NOTE ADDED")) {
                keyword = "CSV ONLY";
            }

            String note = cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_ITEM_NOTE));
            if (cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_WAS_CANCELLED)).equals("TRUE")) {
                if (!note.contains("*cancelled*")) {
                    note = "*cancelled*" + " " + note;
                }
            } else if (!note.contains("*confirmed*")) {
                note = "*confirmed* " + note;
            }

            log += cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD )) +", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_TYPE))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_WAS_CANCELLED))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_TIME ))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_TRACK_NUMBER))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_FIGURE_COLOR))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_FROM_LAST))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_RANGE))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_GPS_ACCURACY))+", "
                   + cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST))+", "
                   + note+"\n";
        }

        writeToExternalStorage(this, logName, log, fileType);
    }
    void exportToWam(String logName) {
        String fileType = ".txt";
        Cursor cursor = context.getContentResolver().query(ListContract.ListContractEntry.ITEMS_CONTENT_URI,
                null,
                ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST + " = ? ",
                new String[]{logName},
                null);

        while (cursor.moveToNext()){
            String typeOfEvent = cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_TYPE));
            String keyword = cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD));
            String log = "";

            switch (keyword){
                case "ACTION":
                    try {
                        String note = cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_ITEM_NOTE));
                        if (cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_WAS_CANCELLED)).equals("TRUE")){
                            if (!note.contains("*cancelled*")){
                                note = "*cancelled*" + note;
                            }
                        }else  if (!note.contains("*confirmed*")){
                            note = "*confirmed* " + typeOfEvent + " " + note;
                        }

                        //if (note.contains("*confirmed*")) {  //this worked here, but not in the Point switch case, commenting out for now
                        log = WamFormater.formatAction(cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_TIME)),
                                cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME)),
                                cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_TRACK_NUMBER)),
                                cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG)),
                                cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE)),
                                cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE)),
                                cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE)),
                                note,
                                cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE)));
                        //}
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    break;
                case "POINT":

                    log = WamFormater.formatPoint(cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_TIME)),
                            cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_TRACK_NUMBER)),
                            cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE)),
                            cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE)),
                            cursor.getString(cursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE)));

                    break;
            }
            String writeLogName = logName + "_WAM";
            // writeToExternalStorage(this, logName, log, fileType);
            writeToExternalStorage(this, writeLogName, log, fileType);
        }
    } // end exportToWam
    /**
     * write current readings to file
     */
    public static void writeToExternalStorage(Context context, String filename,
                                              String content, String fileType) {

        /**
         * request storage permission
         */
        ActivityCompat.requestPermissions((Activity) context,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                9989);

        FileOutputStream fos;

        String storageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(storageState)) {
            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File dir = new File(root.getAbsolutePath() + "/GTAv2");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File log = new File(dir, filename + fileType);

            try {
                    fos = new FileOutputStream(log, true);
                PrintWriter pw = new PrintWriter(fos);
                pw.write(content);
                pw.flush();
                pw.close();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(context, "External Storage Unavailable", Toast.LENGTH_LONG).show();
        }
    }

    class WriteToWamAsyncTask extends AsyncTask<String,Void,Void>{

        @Override
        protected Void doInBackground(String... strings) {
            exportToWam(strings[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            showMain();
        }
    }

    void showLoading(){
        loadingIndicatorView.setVisibility(View.VISIBLE);
        mainContent.setVisibility(View.INVISIBLE);
    }
    void showMain(){
        loadingIndicatorView.setVisibility(View.INVISIBLE);
        mainContent.setVisibility(View.VISIBLE);
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();

        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void createNewLog() {
        String itemName = etNewEntry.getText().toString();
        if (!itemName.matches("") && !itemName.contains("/")) {
            ContentValues cv = new ContentValues();
            cv.put(ListContract.ListContractEntry.COLUMN_LOG_NAME, itemName);
            Uri uri = getContentResolver().insert(ListContract.ListContractEntry.DIRECTORY_CONTENT_URI, cv);
            etNewEntry.setText("");

            // once the new entry is added, close the keyboard
            closeKeyboard();

            Toast toast = Toast.makeText(context, "New log created", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM,0 ,0);
            toast.show();

            return;
        }
        Toast toast = Toast.makeText(context, "invalid log name", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM,0 ,0);
        toast.show();
    }
}
