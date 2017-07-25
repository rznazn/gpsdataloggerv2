package com.babykangaroo.android.gpsdataloggerv2;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.babykangaroo.android.mydatabaselibrary.ListContract;

/**
 * Created by Gene Denney on 7/25/2017.
 */

public class MyCursorAdapterEventLog extends RecyclerView.Adapter<MyCursorAdapterEventLog.ItemViewHolder> {

    /**
     * Cursor and Context for the adpater
     */
    private Cursor mCursor;
    private Context mContext;
    private MyCursorAdapterEventLog.ListItemClickListener mClickListener;


    /** interface for list item CLicks */
    public interface ListItemClickListener {
        void onItemClick(long itemCursorID);
    }

    /**
     * public constructor for use by activities
     */
    public MyCursorAdapterEventLog(Context context, MyCursorAdapterEventLog.ListItemClickListener clickListener) {
        this.mContext = context;
        this.mClickListener = clickListener;
    }

    /**
     * When data changes and a re-query occurs, this function swaps the old Cursor
     * with a newly updated Cursor (Cursor c) that is passed in.
     */
    public void swapCursor(Cursor c) {
        if (c != null) {
            mCursor = c;
            this.notifyDataSetChanged();
        }
    }

    /**
     * inflate desired xml resource
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public MyCursorAdapterEventLog.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.log_event_alert_dialog, parent, false);

        return new MyCursorAdapterEventLog.ItemViewHolder(view);
    }

    /**
     * assign and attach data in the view holder
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(MyCursorAdapterEventLog.ItemViewHolder holder, int position) {
        if (mCursor.moveToPosition(position)) {
            String eventSummary = mCursor.getString(mCursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_TIME)) + "\nBearing: "
                    + mCursor.getString(mCursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG)) + "\nLat: "
                    + mCursor.getString(mCursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE)) + "\nLong:"
                    + mCursor.getString(mCursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE));

            holder.tvEventData.setText(eventSummary);

            holder.etNote.setText(mCursor.getString(mCursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_ITEM_NOTE)));
        }

    }

    /**
     * @return the count of the cursor or 0 if null
     */
    @Override
    public int getItemCount() {
        if (mCursor != null) {
            int count = mCursor.getCount();
            return count;
        }
        return 0;
    }

    /**
     * Viewholder for holding individual items in the recyclerview
     */
    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView tvEventData;
        public TextView etNote;
        public ItemViewHolder(View itemView) {
            super(itemView);
            tvEventData = (TextView) itemView.findViewById(R.id.tv_event_summary);
            etNote = (TextView) itemView.findViewById(R.id.et_event_note);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mCursor.moveToPosition(getAdapterPosition());
            long itemCursorID = mCursor.getLong(mCursor.getColumnIndex(BaseColumns._ID));
            mClickListener.onItemClick(itemCursorID);
        }
    }
}
