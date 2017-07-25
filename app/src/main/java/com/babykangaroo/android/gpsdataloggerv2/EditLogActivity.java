package com.babykangaroo.android.gpsdataloggerv2;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
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
                null);
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
    public void onItemClick(long itemCursorID) {

    }
}
