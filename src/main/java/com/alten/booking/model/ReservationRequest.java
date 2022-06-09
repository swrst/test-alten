package com.alten.booking.model;

import java.time.LocalDate;

public class ReservationRequest {

    private Integer days;
    private LocalDate startDate;

    public ReservationRequest(Integer days, LocalDate startDate) {
        this.days = days;
        this.startDate = startDate;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
