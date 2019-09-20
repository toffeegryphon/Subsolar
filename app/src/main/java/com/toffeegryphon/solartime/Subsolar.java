package com.toffeegryphon.solartime;

import android.util.Log;

import java.util.Calendar;
import java.lang.Math;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

class Subsolar {

    static class Coordinate {
        double longitude, latitude;

        Coordinate(double latitude, double longitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }

    static Coordinate equationOfTime() { // Accurate version
        //TODO CONVERT TO RADIAN MODE
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        double D = calendar.get(Calendar.DAY_OF_YEAR) - 0.16; // Day of year, -0.16 is minor adjustment
        double t = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0 + calendar.get(Calendar.SECOND) / 3600.0;
        Log.d("T", String.valueOf(t));

        double W = 360.0 / 365.24;
        double A = W * (D + 10.0);
//        Log.d("A", String.valueOf(A));
//        Log.d("DAY OF YEAR", String.valueOf(calendar.get(Calendar.DAY_OF_YEAR)));

        double B = A + 360.0/PI * 0.0167 * sin(toRadians(W * (D - 2.0)));
//        Log.d("B", String.valueOf(toRadians(B)));

        double C = (A - toDegrees(atan(tan(toRadians(B))/cos(toRadians(23.44))))) / 180.0;
//        Log.d("C", String.valueOf(C));

        double EOT = 720 * (C - (int) C);
//        Log.d("EOT", String.valueOf(EOT));

        //TODO lat is inaccurate
        double lat = toDegrees(asin(sin(toRadians(-23.44)) * cos(toRadians(B))));
//        Log.d("LAT", String.valueOf(lat));

        double lon = ((60 * (12 - t)) - EOT) / 4.0;
//        Log.d("LONG", String.valueOf(lon));

        Log.d("LONG_LAT", lon + ", " + lat);

        return new Coordinate(lat, lon);
    }
}
