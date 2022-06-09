package com.alten.booking.model;

import java.time.LocalDate;
import java.util.List;

public class Room {

    private List<LocalDate> nextAvailableDates;
    private LocalDate nextAvailableDate;

    public Room(List<LocalDate> nextAvailableDates) {
        this.nextAvailableDates = nextAvailableDates;
    }

    public List<LocalDate> getNextAvailableDates() {
        return nextAvailableDates;
    }

    public void setNextAvailableDates(List<LocalDate> nextAvailableDates) {
        this.nextAvailableDates = nextAvailableDates;
    }

}
