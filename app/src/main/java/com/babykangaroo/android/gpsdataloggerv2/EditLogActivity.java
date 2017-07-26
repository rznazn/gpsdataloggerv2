package com.babykangaroo.android.gpsdataloggerv2;

import android.app.LoaderManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.babykangaroo.android.mydatabaselibrary.ListContract;

public class EditLogActivity extends AppCompatActivity implements MyCursorAdapterEventLog.ListItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>{

    private RecyclerView recyclerView;
    private MyCursorAdapterEventLog myCursorAdapterEventLog;
    private static final int LOADER_REQUEST = 9995;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_log);

        recyclerView = (RecyclerView) findViewById(R.id.rv_list_directory);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
         myCursorAdapterEventLog = new MyCursorAdapterEventLog(this,this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(myCursorAdapterEventLog);
        getLoaderManager().restartLoader(LOADER_REQUEST,null,this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                ListContract.ListContractEntry.ITEMS_CONTENT_URI,
                null,
                ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD + " = 'ACTION' and "
                        + ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST + " = '"
                        + PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.current_log), "default") + "'",
                null,
                ListContract.ListContractEntry.COLUMN_EVENT_TIME + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        myCursorAdapterEventLog.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        myCursorAdapterEventLog.swapCursor(null);
    }

    @Override
    public void onItemClick(final long itemCursorID, String summary, String note) {
        final View adLayout = getLayoutInflater().inflate(R.layout.log_event_alert_dialog, null);
        final TextView adtvEventSummary = (TextView) adLayout.findViewById(R.id.tv_event_summary);
        adtvEventSummary.setText(summary);
        final EditText adetEventNote = (EditText) adLayout.findViewById(R.id.et_event_note);
        adetEventNote.setText(note);
        AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setView(adLayout);
        builder.setNeutralButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        builder.setPositiveButton("update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_NOTE, adetEventNote.getText().toString());
                getContentResolver().update(ListContract.ListContractEntry.ITEMS_CONTENT_URI,
                        contentValues,
                        ListContract.ListContractEntry._ID + " = ? ",
                        new String[]{String.valueOf(itemCursorID)});
            }
        });

        AlertDialog ad = builder.create();
        ad.show();
    }
}
