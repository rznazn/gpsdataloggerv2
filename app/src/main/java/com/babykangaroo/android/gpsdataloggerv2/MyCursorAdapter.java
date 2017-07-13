package com.babykangaroo.android.gpsdataloggerv2;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.babykangaroo.android.mydatabaselibrary.ListContract;

/**
 * Created by sport on 7/13/2017.
 */

public class MyCursorAdapter extends RecyclerView.Adapter<MyCursorAdapter.ItemViewHolder> {

    /**
     * Cursor and Context for the adpater
     */
    private Cursor mCursor;
    private Context mContext;
    private ListItemClickListener mClickListener;


    /** interface for list item CLicks */
    public interface ListItemClickListener {
        void onItemClick(long itemCursorID, String itemName);
    }

    /**
     * public constructor for use by activities
     */
    public MyCursorAdapter(Context context, ListItemClickListener clickListener) {
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
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.list_item_view, parent, false);

        return new ItemViewHolder(view);
    }

    /**
     * assign and attach data in the view holder
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {

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
        public TextView tvLogName;
        public ItemViewHolder(View itemView) {
            super(itemView);
            tvLogName = (TextView) itemView.findViewById(R.id.tv_list_item_name);
        }

        @Override
        public void onClick(View v) {
            mCursor.moveToPosition(getAdapterPosition());
            long itemCursorID = mCursor.getLong(mCursor.getColumnIndex(BaseColumns._ID));
            String itemName = mCursor.getString(
                    mCursor.getColumnIndex(ListContract.ListContractEntry.COLUMN_LOG_NAME));
            mClickListener.onItemClick(itemCursorID, itemName);
        }
    }
}
