package org.sharedhealth.freeshrUpdate.utils;

import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static int getYearOf(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        return instance.get(Calendar.YEAR);
    }
}
