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
        public static final String COLUMN_LOG_NAME = "log";

        public static final String COLUMN_ITEM_PARENT_LIST = "parent_list";
        public static final String COLUMN_EVENT_KEYWORD = "keyword";
        public static final String COLUMN_EVENT_TIME = "event_time";
        public static final String COlUMN_TRACK_NUMBER = "track_number";
        public static final String COLUMN_EVENT_DIRECTIVE = "action_name";
        public static final String COLUMN_EVENT_LATITUDE = "event_latitude";
        public static final String COLUMN_EVENT_LONGITUDE = "event_longitude";
        public static final String COLUMN_EVENT_ALTITUDE = "event_altitude";
        public static final String COLUMN_FIGURE_COLOR = "figure_color";
        public static final String COLUMN_EVENT_BEARING_MAG = "bearing_magnetic";
        public static final String COLUMN_EVENT_BEARING_FROM_LAST = "bearing_from_last";
        public static final String COLUMN_EVENT_RANGE = "event_range";
        public static final String COLUMN_EVENT_END_TIME = "event_end_time";
        public static final String COLUMN_GPS_ACCURACY = "gps_accuracy";
        public static final String COLUMN_SPEED_FROM_LAST = "gps_speed";
        public static final String COLUMN_ITEM_NOTE = "note";
    }
}
