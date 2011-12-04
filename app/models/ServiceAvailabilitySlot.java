package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class ServiceAvailabilitySlot extends Model {
    @ManyToOne
    public Service service;

    public DayOfWeek dayOfWeek;

    public int startTimeHour;

    public int startTimeMinute;

    public int endTimeHour;

    public int endTimeMinute;

    public int startTimeMinutesAfterMidnight;

    public int endTimeMinutesAfterMidnight;

    public ServiceAvailabilitySlot(Service service,
                                   DayOfWeek dayOfWeek,
                                   int startTimeHour,
                                   int startTimeMinute,
                                   int endTimeHour,
                                   int endTimeMinute) {
        this.service = service;
        this.dayOfWeek = dayOfWeek;
        this.startTimeHour = startTimeHour;
        this.startTimeMinute = startTimeMinute;
        this.endTimeHour = endTimeHour;
        this.endTimeMinute = endTimeMinute;

        this.startTimeMinutesAfterMidnight = this.startTimeHour * 60 + this.startTimeMinute;
        this.endTimeMinutesAfterMidnight = this.endTimeHour * 60 + this.endTimeMinute;
    }
}
