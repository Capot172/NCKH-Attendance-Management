package com.atechclass.attendance.ultis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HolidayUtils {
    public static List<String> getHoliday() {
        List<String> days = new ArrayList<>();
        days.add("01 thg 01 "); // Tet duong lich
        days.add("30 thg 04 "); // Ngày giải phóng
        days.add("01 thg 05 "); // Ngày quốc tế lao động
        days.add("02 thg 09 "); // Ngày quốc khánh

        if(Calendar.getInstance().get(Calendar.YEAR) == 2022) {
            days.add("10 thg 09, 2022");
        }
        if (Calendar.getInstance().get(Calendar.YEAR) == 2023) {
            days.add("29 thg 09, 2023");
            days.add("21 thg 01, 2023");
            days.add("22 thg 01, 2023");
            days.add("23 thg 01, 2023");
            days.add("24 thg 01, 2023");
            days.add("25 thg 01, 2023");
            days.add("26 thg 01, 2023");
        }
        if(Calendar.getInstance().get(Calendar.YEAR) == 2024) {
            days.add("18 thg 09, 2024");
            days.add("09 thg 02, 2024");
            days.add("10 thg 02, 2024");
            days.add("11 thg 02, 2024");
            days.add("12 thg 02, 2024");
            days.add("13 thg 02, 2024");
            days.add("14 thg 02, 2024");
        }
        if(Calendar.getInstance().get(Calendar.YEAR) == 2025) {
            days.add("07 thg 09, 2025");
            days.add("28 thg 01, 2025");
            days.add("29 thg 01, 2025");
            days.add("30 thg 01, 2025");
            days.add("31 thg 01, 2025");
            days.add("01 thg 02, 2025");
            days.add("02 thg 02, 2025");
        }
        if(Calendar.getInstance().get(Calendar.YEAR) == 2026) {
            days.add("26 thg 09, 2026");
            days.add("16 thg 02, 2026");
            days.add("17 thg 02, 2026");
            days.add("18 thg 02, 2026");
            days.add("19 thg 02, 2026");
            days.add("20 thg 02, 2026");
        }
        return days;
    }
}
