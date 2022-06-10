package com.alten.booking.entity;

import com.alten.booking.utils.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
public class Reservation {

    @Id
    @GeneratedValue
    private Long reservationId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;
    private ReservationStatus status;

    public Reservation() {
    }

    public Reservation(LocalDate fromDate, LocalDate toDate, ReservationStatus status) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.status = status;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
