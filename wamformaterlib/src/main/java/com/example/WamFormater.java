package com.example;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jdk.nashorn.internal.runtime.logging.Logger;

/**
 * Created by sport on 4/28/2017.
 */

public class WamFormater {


    public WamFormater(){}

    /**
     * this method is used to create a WAM formatted entry from the provided data.
     * @param eventTime
     * @param azimuth
     * @param latitude
     * @param longitude
     * @param note
     * @return
     * @throws ParseException
     */

    public static String formatAction(String eventTime, String eventEndTime, String trackID, String azimuth,
                                      String latitude, String longitude, String altitude,
                                      String note, String actionType) throws ParseException {

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

        String[] timeParts = eventTime.split("\\\\");
        String bookmark = timeParts[0].substring(0,4) + ":" + timeParts[0].substring(4,6) + ":" + timeParts[0].substring(6,8) + " "
                    + timeParts[1] .substring(0,2) + ":" + timeParts[1].substring(2,4) + ":" + timeParts[1].substring(4,6)
                    + " " + note;

            return bookmark + "\n" +
                    "ACTION\\"
                    + eventTime + "\\\\\\" + trackID + "\\\\\\\\\\\\\\"
                    +  note + "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\1\\"+ actionType + "\\"
                    +  wamLatFormatted + "\\" + northOrSouth + "\\"
                    + wamLonFormatted + "\\" + eastOrWest + "\\" +altitude + "\\11\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"
                    + azimuth + "\\2.5\\\\\\0.1\\"
                    + eventEndTime + "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\" +
                    "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\GTA\n";

    }

    public static String formatPoint(String time, String trackNumber, String latitude, String longitude
            , String altitude){
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

        return  "POINT\\"
                + time +"\\"
                + wamLatFormatted + "\\" + northOrSouth + "\\"
                + wamLonFormatted + "\\" + eastOrWest + "\\" + altitude +"\\0.0\\0.0\\MSL\\\\\\\\JADeMobile\\\\L" +
                "\\\\F\\\\\\\\\\\\"+ trackNumber +"\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\RADAR\\GTA\\\n";
    }
}

