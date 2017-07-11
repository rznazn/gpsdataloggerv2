package com.example;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by sport on 4/28/2017.
 */

public class WamFormater {

    private static final String DEVICE_DESIGNATION = "001";

    public WamFormater(){}

    /**
     * this method is used to create a WAM formatted entry from the provided data.
     * @param eventTime
     * @param azimuth
     * @param latitude
     * @param longitude
     * @param note
     * @param trueForEventFalseForNote
     * @return
     * @throws ParseException
     */

    public static String formatToWam(String eventTime, String azimuth, String latitude, String longitude,
                              String note, boolean trueForEventFalseForNote) throws ParseException {
        String[] dateTime = eventTime.split(" ");
        String dateToLog = dateTime[0];
        String timeToLog = dateTime[1];

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HHmmss");
        Date eventDate = dateFormat.parse(eventTime);
        long eventTimeInMil = eventDate.getTime();
        long eventEndTimeInMil = eventTimeInMil + 10000;
         String eventEndTimeString =  dateFormat.format(eventEndTimeInMil);

        String[] endDateTime = eventEndTimeString.split(" ");
        String endDateToLog = endDateTime[0];
        String endTimeToLog = endDateTime[1];

        String[] latSplit = latitude.split(":");
        String northOrSouth = "N";
        double wamLatDegrees = Integer.valueOf( latSplit[0]) * 100;
        if (wamLatDegrees <0){
            wamLatDegrees = wamLatDegrees * -1;
            northOrSouth = "S";

        }
        double wamLatMinutes = Double.valueOf(latSplit[1]);
        double wamLatFormatted = wamLatDegrees + wamLatMinutes;

        String[] lonSplit = longitude.split(":");
        String eastOrWest = "E";
        double wamLonDegrees = Double.valueOf( lonSplit[0]) * 100;
        if (wamLonDegrees <0){
            wamLonDegrees = wamLonDegrees * -1;
            eastOrWest = "W";

        }
        double wamLonMinutes = Double.valueOf(lonSplit[1]);
        double wamLonFormatted = wamLonDegrees + wamLonMinutes;

        if (trueForEventFalseForNote) {
            return "ACTION\\"
                    + dateToLog + "\\"
                    + timeToLog + "\\000\\\\\\" + DEVICE_DESIGNATION + "\\\\\\\\\\\\\\"
                    +  note + "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\1\\TEXT_LINEB_LL\\"
                    +  wamLatFormatted + "\\" + northOrSouth + "\\"
                    + wamLonFormatted + "\\" + eastOrWest + "\\123.0\\11\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"
                    + azimuth + "\\2.5\\\\\\0.1\\"
                    + endDateToLog + "\\" + endTimeToLog + "\\000\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\" +
                    "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\GDD\n"
                    + "POINT\\"
                    + dateToLog + "\\"
                    + timeToLog + "\\000\\"
                    + wamLatFormatted + "\\" + northOrSouth + "\\"
                    + wamLonFormatted + "\\" + eastOrWest + "\\123.0\\0.0\\0.0\\MSL\\\\\\\\JADeMobile\\\\L" +
                    "\\\\F\\\\\\\\\\\\"+ DEVICE_DESIGNATION +"\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\RADAR\\GDD\\\n";
        } else {
            return "ACTION\\"
                    + dateToLog + "\\"
                    + timeToLog + "\\000\\\\\\"+ DEVICE_DESIGNATION +"\\\\\\\\\\\\\\"
                    +  note + "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\1\\TEXT_LL\\"
                    +  wamLatFormatted + "\\" + northOrSouth + "\\"
                    + wamLonFormatted + "\\" + eastOrWest + "\\123.0\\11\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"
                    + azimuth + "\\2.5\\\\\\0.1\\"
                    + endDateToLog + "\\" + endTimeToLog + "\\000\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\" +
                    "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\GDD\n"
                    + "POINT\\"
                    + dateToLog + "\\"
                    + timeToLog + "\\000\\"
                    + wamLatFormatted + "\\" + northOrSouth + "\\"
                    + wamLonFormatted + "\\" + eastOrWest + "\\123.0\\0.0\\0.0\\MSL\\\\\\\\JADeMobile\\\\L" +
                    "\\\\F\\\\\\\\\\\\"+ DEVICE_DESIGNATION +"\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\RADAR\\GDD\\\n";
        }
    }
}

