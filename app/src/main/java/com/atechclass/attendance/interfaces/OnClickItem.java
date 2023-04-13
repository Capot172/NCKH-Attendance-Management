package com.atechclass.attendance.interfaces;

public interface OnClickItem {
    void deleteItem(int position);
    void editItem(int position);
    void getData(String[] listData, String startTime, String endTime, String address, String room);
}
