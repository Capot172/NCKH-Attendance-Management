package com.atechclass.attendance.ultis;

import android.os.Build;
import android.util.Log;
import android.widget.EditText;

import org.checkerframework.checker.units.qual.C;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarUtils {
    public static LocalDate localDate;

    public static LocalDate dates() {
        LocalDate dateNow = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dateNow = LocalDate.now();
        }
        return dateNow;
    }

    public static String getWeekYear(LocalDate localDate) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            return localDate.format(formatter);
        }
        return "";
    }

    public static String getToday(LocalDate localDate, String format) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return localDate.format(formatter);
        }
        return "";
    }

    public static List<String> getWeekNow(LocalDate localDate, String format) {
        List<String> dayOfWeeks = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate current = monDay(localDate);
            LocalDate endWeek = current.plusWeeks(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            while (current.isBefore(endWeek)) {
                dayOfWeeks.add(current.format(formatter));
                current = current.plusDays(1);
            }
        }
        return dayOfWeeks;
    }

    public static List<String> getListMonth(Calendar cldStart, Calendar cldEnd) {
        List<String> listMonth = new ArrayList<>();
        if(cldStart.equals(cldEnd)) {
            listMonth.add((cldStart.get(Calendar.MONTH) + 1) + "/" + cldStart.get(Calendar.YEAR));
            return listMonth;
        }
        cldStart.set(Calendar.DAY_OF_MONTH, 1);
        while (!cldStart.after(cldEnd)) {
            listMonth.add((cldStart.get(Calendar.MONTH) + 1) + "/" + cldStart.get(Calendar.YEAR));
            cldStart.add(Calendar.MONTH, 1);
        }
        return listMonth;
    }

    public static List<String> getListWeek(Calendar cldStart) {
        Calendar temp = (Calendar) cldStart.clone();
        List<String> listWeek = new ArrayList<>();
        int numberOfWeek = temp.getActualMaximum(Calendar.WEEK_OF_MONTH);
        for (int i = 0; i < numberOfWeek; i ++){
            listWeek.add("Tuần " + temp.get(Calendar.WEEK_OF_YEAR));
            temp.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return listWeek;
    }

    public static boolean isSameDay(Date date1, Date date2) {
        LocalDate localDate1 = date1.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate localDate2 = date2.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return localDate1.isEqual(localDate2);
    }

    public static List<String> getEndOfWeek(LocalDate localDate, String format, EditText repetition) {
        List<String> dayOfWeeks = new ArrayList<>();
        LocalDate current = localDate;
        // Số lần lặp
        for(int i = 0; i < Integer.valueOf(repetition.getText().toString()); i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate endWeek = sunDay(current);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                //Lặp ngày hiện tại cho đến cuối tuần
                while (!current.isAfter(endWeek)) {
                    dayOfWeeks.add(current.format(formatter));
                    current = current.plusDays(1);
                }
            }
        }
        return dayOfWeeks;
    }
    public static String monthYearFromDate(LocalDate date)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        return date.format(formatter);
    }
    public static List<Long> getDayRepeatMonth(LocalDate localDate, EditText repetition) {
        List<Long> dayOfMonth = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate current = localDate;
            LocalDate endMonth = current.plusMonths(1);
            String day = current.getDayOfWeek().toString();
            //Số lần lặp
            for(int i = 0; i < Integer.valueOf(repetition.getText().toString()); i++) {
                //Kiểm tra ngày hiện tại ở trước ngày kết thúc
                while (!current.isAfter(endMonth)) {
                    //Nếu ngày hiện tại bằng ngày kết thúc thì thêm ngày vào danh sách
                    if(day.contains(current.getDayOfWeek().toString())) {
                        dayOfMonth.add(current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
                        break;
                    }
                    //Tăng thêm 1 ngày để lặp
                    else {
                        current = current.plusDays(1);
                    }
                }
                //Tăng thêm 1 tháng để lăp
                current.plusMonths(1);
                endMonth = current.plusMonths(1);
                //Lấy ngày đầu tiên của tháng
                current = getFirstDay(endMonth);
                //Lấy ngày kết thúc của tháng
                endMonth = getEndMonth(current);
            }
        }
        return dayOfMonth;
    }

    public static LocalDate getEndMonth(LocalDate current) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            YearMonth month = YearMonth.from(current);
            current = month.atEndOfMonth();
        }
        return current;
    }

    public static LocalDate getFirstDay(LocalDate current) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            YearMonth month = YearMonth.from(current);
            current = month.atDay(1);
        }
        return current;
    }

    public static List<String> getMonthNow(LocalDate localDate) {
        List<String> dayOfMonth = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate current = LocalDate.now().withDayOfMonth(1);
            LocalDate endMonth = current.plusMonths(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM, yyyy");
            while (current.isBefore(endMonth)) {
                dayOfMonth.add(current.format(formatter));
                current = current.plusDays(1);
            }
        }
        return dayOfMonth;
    }

    public static LocalDate monDay(LocalDate localDate) {
        LocalDate onWeekAgo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onWeekAgo = localDate.minusWeeks(1);
            while(localDate.isAfter(onWeekAgo)) {
                if(localDate.getDayOfWeek() == DayOfWeek.MONDAY)
                    return localDate;
                else localDate = localDate.minusDays(1);
            }
        }
        return onWeekAgo;
    }

    public static LocalDate tuesDay(LocalDate localDate) {
        LocalDate onWeekAgo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onWeekAgo = localDate.minusWeeks(1);
            while(localDate.isAfter(onWeekAgo)) {
                if(localDate.getDayOfWeek() == DayOfWeek.TUESDAY)
                    return localDate;
                else localDate = localDate.plusDays(1);
            }
        }
        return onWeekAgo;
    }

    public static LocalDate wednesDay(LocalDate localDate) {
        LocalDate onWeekAgo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onWeekAgo = localDate.minusWeeks(1);
            while(localDate.isAfter(onWeekAgo)) {
                if(localDate.getDayOfWeek() == DayOfWeek.WEDNESDAY)
                    return localDate;
                else localDate = localDate.plusDays(1);
            }
        }
        return onWeekAgo;
    }

    public static LocalDate thursDay(LocalDate localDate) {
        LocalDate onWeekAgo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onWeekAgo = localDate.minusWeeks(1);
            while(localDate.isAfter(onWeekAgo)) {
                if(localDate.getDayOfWeek() == DayOfWeek.THURSDAY)
                    return localDate;
                else localDate = localDate.plusDays(1);
            }
        }
        return onWeekAgo;
    }

    public static LocalDate friDay(LocalDate localDate) {
        LocalDate onWeekAgo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onWeekAgo = localDate.minusWeeks(1);
            while(localDate.isAfter(onWeekAgo)) {
                if(localDate.getDayOfWeek() == DayOfWeek.FRIDAY)
                    return localDate;
                else localDate = localDate.plusDays(1);
            }
        }
        return onWeekAgo;
    }

    public static LocalDate saturDay(LocalDate localDate) {
        LocalDate onWeekAgo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onWeekAgo = localDate.minusWeeks(1);
            while(localDate.isAfter(onWeekAgo)) {
                if(localDate.getDayOfWeek() == DayOfWeek.SATURDAY)
                    return localDate;
                else localDate = localDate.plusDays(1);
            }
        }
        return onWeekAgo;
    }

    public static LocalDate sunDay(LocalDate localDate) {
        LocalDate onWeekAgo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onWeekAgo = localDate.plusWeeks(1);
            while(!localDate.isAfter(onWeekAgo)) {
                if(localDate.getDayOfWeek() == DayOfWeek.SUNDAY)
                    return localDate;
                else localDate = localDate.plusDays(1);
            }
        }
        return onWeekAgo;
    }

    public static boolean isAfterDay(LocalDate localDate) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate date = LocalDate.now();
            if(localDate.isAfter(date))
                return false;
        }
        return true;
    }
}
