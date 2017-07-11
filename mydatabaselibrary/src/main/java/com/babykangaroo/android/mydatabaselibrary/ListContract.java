package com.babykangaroo.android.mydatabaselibrary;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by sport on 5/31/2017.
 */

public class ListContract {
    /**
     * URI component strings
     */
    //TODO change authority to current app
    public static final String CONTENT_AUTHORITY = "com.babykangaroo.android.gpsdataloggerv2";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://"+ CONTENT_AUTHORITY);
    public static final String DIRECTORY_PATH_NAME = "directory";
    public static final String ITEMS_PATH_NAME = "items";

    public static final class ListContractEntry implements BaseColumns {
        /**
         * The content URI to access the list data in the provider
         */
        public static final Uri DIRECTORY_CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(DIRECTORY_PATH_NAME).build();
        public static final Uri ITEMS_CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, ITEMS_PATH_NAME);

        /**
         * Set the String Constants to represent the column names
         */
        public static final String DIRECTORY_TABLE_NAME = "directory";
        public static final String ITEMS_TABLE_NAME = "items";
        public final static String _ID = BaseColumns._ID;
        public static final String COLUMN_ITEM_NAME = "name";
        public static final String COLUMN_ITEM_PARENT_LIST = "parent_list";
        public static final String COLUMN_ITEM_NOTE = "note";
    }
}
