package com.alten.booking.model;

import com.alten.booking.utils.ReservationStatus;

import java.time.LocalDate;

public class ReservationResponse {

    private Long reservationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private ReservationStatus status;

    public ReservationResponse() {
    }

    public ReservationResponse(Long reservationId, LocalDate startDate, LocalDate endDate, ReservationStatus status) {
        this.reservationId = reservationId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
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

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
