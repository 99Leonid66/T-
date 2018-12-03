package ru.mtuci.tp_cw;

import java.util.Comparator;
import java.util.Date;

public class LogEntry {
    private int laneNumber;
    private Date datetime;
    private double speed; // MPS
    private double gap; // seconds
    private int volume;
    private double occupancy;

    public LogEntry(int laneNumber, Date datetime, double speed, double gap, int volume, double occupancy) {
        this.laneNumber = laneNumber;
        this.datetime = datetime;
        this.speed = speed;
        this.gap = gap;
        this.volume = volume;
        this.occupancy = occupancy;
    }

    /**
     * @return the laneNumber
     */
    public int getLaneNumber() {
        return laneNumber;
    }

    /**
     * @return the datetime
     */
    public Date getDatetime() {
        return datetime;
    }

    /**
     * @return the speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @return the gap
     */
    public double getGap() {
        return gap;
    }
    /**
     * @return the volume
     */
    public int getVolume() {
        return volume;
    }

    /**
     * @return the occupancy
     */
    public double getOccupancy() {
        return occupancy;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LogEntry)) return false;

        var entry = (LogEntry)o;
        return laneNumber == entry.laneNumber &&
            datetime == entry.datetime &&
            speed == entry.speed &&
            gap == entry.gap &&
            volume == entry.volume &&
            occupancy == entry.occupancy;
    }

    @Override
    public String toString() {
        return String.format(
            "(LogEntry: lane=%d, speed=%f, gap=%f, volume=%d, occupancy=%f, time=%tY-%<tm-%<td %<tH:%<tM:%<tS)",
            laneNumber, speed, gap, volume, occupancy, datetime
        );
    }

    public static Comparator<LogEntry> logDatetimeComparator = new Comparator<LogEntry>() {
        @Override
        public int compare(LogEntry o1, LogEntry o2) {
            return o1.datetime.compareTo(o2.datetime);
        }
    };
}
