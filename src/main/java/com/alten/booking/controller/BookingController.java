package com.alten.booking.controller;

import com.alten.booking.entity.Reservation;
import com.alten.booking.exception.InvalidRequestException;
import com.alten.booking.exception.RoomBookedException;
import com.alten.booking.model.ReservationRequest;
import com.alten.booking.model.ReservationResponse;
import com.alten.booking.model.Room;
import com.alten.booking.repository.ReservationRepository;
import com.alten.booking.utils.ReservationStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
public class BookingController {

    private final Room room;
    private final ReservationRepository reservationRepository;

    BookingController(ReservationRepository reservationRepository) {
        //room initialization
        this.room = new Room(new ArrayList<>());
        this.reservationRepository = reservationRepository;
    }

    @GetMapping(path = "/checkRoomAvailability")
    public Room getRoomAvailability() {
        this.room.setNextAvailableDates(findAvailableDates());
        return room;
    }

    @PostMapping(path = "/roomBooking")
    public ReservationResponse bookRoom(@RequestBody ReservationRequest reservationRequest) {

        if (Objects.nonNull(reservationRequest)) {
            //Reservation date/days check
            LocalDate reservationStartDate = reservationRequest.getStartDate();
            Integer reservationDays = reservationRequest.getDays() - 1;

            //DATES & DAYS VALIDATION
            isReservationDateValid(reservationStartDate, reservationDays);

            LocalDate reservationEndDate = reservationStartDate.plusDays(reservationDays);

            List<Reservation> reservationList = reservationRepository.findAllByOrderByFromDateAsc();

            //No reservations yet, first booking no more verifications
            if (reservationList.isEmpty()) {

                //Saving reservation to DB
                Reservation reservation = saveReservation(reservationDays, reservationStartDate, reservationEndDate);

                return new ReservationResponse(reservation.getReservationId(), reservation.getDays(),
                        reservation.getFromDate(), reservation.getStatus());
            } else if (reservationList.size() == 1) {
                //One reservation, check only if the new reservations is before or after the existing
                if (reservationStartDate.isAfter(reservationList.get(0).getToDate()) ||
                        (reservationStartDate.isBefore(reservationList.get(0).getFromDate()) && reservationEndDate.isBefore(reservationList.get(0).getToDate()))) {

                    //Saving reservation to DB
                    Reservation reservation = saveReservation(reservationDays, reservationStartDate, reservationEndDate);

                    return new ReservationResponse(reservation.getReservationId(), reservation.getDays(),
                            reservation.getFromDate(), reservation.getStatus());
                } else {
                    throw new RoomBookedException("Room not available for the selected dates!");
                }
            } else {
                //Any other case, retrieve the list of available dates and check the first range where it can fit
                List<LocalDate> availableDates = findAvailableDates();
                LocalDate firstAvailableDate = availableDates.get(0);
                for (int i = 1; i < reservationList.size(); i++) {
                    long daysBetween = ChronoUnit.DAYS.between(firstAvailableDate, reservationList.get(i).getFromDate());
                    if (daysBetween >= reservationDays) {

                        //Saving reservation to DB
                        Reservation reservation = saveReservation(reservationDays, reservationStartDate, reservationEndDate);

                        return new ReservationResponse(reservation.getReservationId(), reservation.getDays(),
                                reservation.getFromDate(), reservation.getStatus());
                    }
                    firstAvailableDate = reservationList.get(i).getToDate().plusDays(1);
                }

                throw new RoomBookedException("Room not available for the selected dates!");
            }
        } else {
            throw new InvalidRequestException("Reservation request is null!");
        }

    }

    @PutMapping(path = "/roomUpdate")
    public Room updateRoom(Room updatedRoom) {

//        room.setNextAvailableDate(updatedRoom.getNextAvailableDate());

        return room;
    }

    private void isReservationDateValid(LocalDate reservationDate, Integer days) {
        // NULL REQUEST CHECK
        if (Objects.isNull(reservationDate) || Objects.isNull(days)) {
            throw new InvalidRequestException("Date not selected");
        } else {
            // DATES CHECK
            LocalDate todayDate = LocalDate.now(); // current sys date
            // 30 days  in advance check
            if (reservationDate.isAfter(todayDate.plusDays(30))) {
                throw new InvalidRequestException("Cannot reserve the room more than 30 days in advance");
            }
            // Future date check
            if (reservationDate.isBefore(todayDate)) {
                throw new InvalidRequestException("Please select a date in the future");
            }
            // Reservation start next day of booking check
            if (reservationDate.isEqual(todayDate)) {
                throw new InvalidRequestException("Cannot make a reservation the same day of the booking, please select a date starting from tomorrow");
            }
            // Days of reservation check (1-3 range)
            if (days <= 0 || days > 3) {
                throw new InvalidRequestException("Please select a day range from 1 minimum to 3 maximum");
            }
        }
    }

    private Reservation saveReservation(Integer reservationDays, LocalDate reservationStartDate, LocalDate reservationEndDate) {
        Reservation reservation = new Reservation(reservationDays, reservationStartDate, reservationEndDate, ReservationStatus.CONFIRMED);
        return reservationRepository.saveAndFlush(reservation);
    }

    private void cancelReservation(Long reservationId) {
        reservationRepository.deleteById(reservationId);
    }

    private List<LocalDate> findAvailableDates() {
        List<Reservation> reservationList = reservationRepository.findAllByOrderByFromDateAsc();

        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusMonths(1);

        if (reservationList.isEmpty()) {
            return startDate.datesUntil(endDate).collect(Collectors.toList());
        } else {

            for (LocalDate date : startDate.datesUntil(endDate).collect(Collectors.toList())) {
                boolean bookedDay = false;
                for (Reservation reservation : reservationList) {
                    if (reservation.getFromDate().datesUntil(reservation.getToDate().plusDays(1)).anyMatch(localDate -> localDate.isEqual(date))) {
                        bookedDay = true;
                        break;
                    }
                }
                if (!bookedDay) {
                    availableDates.add(date);
                }
            }
        }

        return availableDates;

    }

}
