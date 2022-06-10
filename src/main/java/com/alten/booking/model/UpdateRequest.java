package com.alten.booking.model;

import java.time.LocalDate;

public class UpdateRequest {

    private Long reservationId;
    private LocalDate startDate;
    private LocalDate endDate;

    public UpdateRequest(Long reservationId, LocalDate startDate, LocalDate endDate) {
        this.reservationId = reservationId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
