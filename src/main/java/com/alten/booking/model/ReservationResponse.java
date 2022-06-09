package com.alten.booking.model;

import com.alten.booking.utils.ReservationStatus;

import java.time.LocalDate;

public class ReservationResponse {

    private Long reservationId;
    private Integer days;
    private LocalDate startDate;
    private ReservationStatus status;

    public ReservationResponse(Long reservationId, Integer days, LocalDate startDate, ReservationStatus status) {
        this.reservationId = reservationId;
        this.days = days;
        this.startDate = startDate;
        this.status = status;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
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

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
