package com.alten.booking.controller;

import com.alten.booking.entity.Reservation;
import com.alten.booking.exception.InvalidRequestException;
import com.alten.booking.exception.RoomBookedException;
import com.alten.booking.model.ReservationRequest;
import com.alten.booking.model.ReservationResponse;
import com.alten.booking.model.Room;
import com.alten.booking.model.UpdateRequest;
import com.alten.booking.repository.ReservationRepository;
import com.alten.booking.utils.ReservationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.alten.booking.utils.ErrorMessage.*;

@RestController
public class BookingController {

    Logger log = LoggerFactory.getLogger(BookingController.class);

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
        log.info("Room availability dates retrieved");
        return room;
    }

    @GetMapping(path = "/checkReservation")
    public Reservation getRoomAvailability(@RequestParam Long reservationId) {
        if (Objects.isNull(reservationId)) {
            throw new InvalidRequestException(RESERVATION_ID_REQUIRED);
        }
        Optional<Reservation> reservation = reservationRepository.findById(reservationId);
        if (reservation.isEmpty()) {
            log.error(RESERVATION_NOT_FOUND + reservationId);
            throw new InvalidRequestException(RESERVATION_NOT_FOUND + reservationId);
        }

        log.info("Reservation retrieved");

        return reservation.get();
    }

    @PostMapping(path = "/roomReservation")
    @ResponseStatus(code = HttpStatus.OK)
    public ReservationResponse roomReservation(@RequestBody ReservationRequest reservationRequest) {

        if (Objects.nonNull(reservationRequest)) {
            //Reservation date/days check

            /*
                1 DAY = from 00 to 23.59
                Remove 1 day for DATES LOGIC
                Example: reservation for 2 days, startDate: 2022-06-09 [with reservationDays = 1 -> 2022-06-09, 2022-06-10]
                Example: reservation for 1 day, startDate: 2022-06-15 [with reservationDays = 0 -> 2022-06-15]
             */
            int reservationDays = reservationRequest.getDays() - 1;

            LocalDate reservationStartDate = reservationRequest.getStartDate();

            //DATES & DAYS VALIDATION
            isReservationDateValid(reservationStartDate, reservationDays);

            LocalDate reservationEndDate = reservationStartDate.plusDays(reservationDays);

            List<LocalDate> availableDates = findAvailableDates();

            boolean isReservationPossible = isReservationPossible(availableDates, reservationStartDate, reservationEndDate, reservationDays);
            log.debug("isReservationPossible: {} ", isReservationPossible);

            if (isReservationPossible) {
                Reservation reservation = saveReservation(reservationStartDate, reservationEndDate);
                log.info("Reservation completed");
                return createReservationResponse(reservation);
            } else {
                //Couldn't find available slot, room booked for the selected dates
                log.error(ROOM_BOOKED);
                throw new RoomBookedException(ROOM_BOOKED);
            }

        } else {
            log.error(REQUEST_NULL);
            throw new InvalidRequestException(REQUEST_NULL);
        }

    }

    @PutMapping(path = "/updateReservation")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public ReservationResponse reservationUpdate(@RequestBody UpdateRequest updateRequest) {

        if (Objects.isNull(updateRequest.getReservationId())) {
            log.error(RESERVATION_ID_REQUIRED);
            throw new InvalidRequestException(RESERVATION_ID_REQUIRED);
        }

        LocalDate reservationStartDate = updateRequest.getStartDate();
        LocalDate reservationEndDate = updateRequest.getEndDate();
        long daysBetween = reservationStartDate.until(reservationEndDate, ChronoUnit.DAYS);

        //dates check
        isReservationDateValid(updateRequest.getStartDate(), (int) daysBetween);

        Optional<Reservation> reservation = reservationRepository.findById(updateRequest.getReservationId());
        if (reservation.isPresent()) {
            List<LocalDate> availableDates = findAvailableDates();

            //Adding previous reservation dates to the list
            availableDates.addAll(reservation.get().getFromDate().datesUntil(reservation.get().getToDate()).collect(Collectors.toList()));
            //Reorder by fromDate
            Collections.sort(availableDates);

            boolean isReservationPossible = isReservationPossible(availableDates, reservationStartDate, reservationEndDate, daysBetween);

            if (isReservationPossible) {
                Reservation reservationUpdated = updateReservation(reservation.get(), reservationStartDate, reservationEndDate);
                log.info("Reservation with id {} updated", updateRequest.getReservationId());
                return createReservationResponse(reservationUpdated);
            } else {
                //Couldn't find available slot, room booked for the selected dates
                log.error(ROOM_BOOKED);
                throw new RoomBookedException(ROOM_BOOKED);
            }

        } else {
            log.error(RESERVATION_NOT_FOUND + updateRequest.getReservationId());
            throw new InvalidRequestException(RESERVATION_NOT_FOUND + updateRequest.getReservationId());
        }
    }

    @DeleteMapping(path = "/cancelReservation")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public ReservationResponse cancelReservation(@RequestParam long reservationId) {

        Optional<Reservation> reservation = reservationRepository.findById(reservationId);
        if (reservation.isPresent()) {
            deleteReservation(reservationId);
        } else {
            log.error(RESERVATION_NOT_FOUND, reservationId);
            throw new InvalidRequestException(RESERVATION_NOT_FOUND + reservationId);
        }

        ReservationResponse cancelResponse = new ReservationResponse();
        cancelResponse.setReservationId(reservationId);
        cancelResponse.setStatus(ReservationStatus.CANCELLED);

        log.info("Reservation with id {} cancelled", reservationId);
        return cancelResponse;
    }

    /**
     * Validation of reservation request parameters
     *
     * @param reservationDate booking startDate
     * @param days            total reservation days
     * @throws InvalidRequestException when finds one parameter not valid
     */
    private void isReservationDateValid(LocalDate reservationDate, Integer days) {
        // NULL REQUEST CHECK
        if (Objects.isNull(reservationDate) || Objects.isNull(days)) {
            throw new InvalidRequestException(DATE_NOT_SELECTED);
        } else {
            // DATES CHECK
            LocalDate todayDate = LocalDate.now(); // current sys date
            // 30 days  in advance check
            if (reservationDate.isAfter(todayDate.plusDays(30))) {
                throw new InvalidRequestException(NO_MORE_THAN_30);
            }
            // Future date check
            if (reservationDate.isBefore(todayDate)) {
                throw new InvalidRequestException(FUTURE_DATE);
            }
            // Reservation start next day of booking check
            if (reservationDate.isEqual(todayDate)) {
                throw new InvalidRequestException(NO_RESERVATION_TODAY);
            }
            // Days of reservation check (1-3 range)
            if (days < 0 || days > 2) {
                throw new InvalidRequestException(RESERVATION_RANGE);
            }
        }

        log.debug("Reservation params check ok");

    }

    /**
     * Retrieves all the reservations and calculate the next available dates
     *
     * @return a list of the available in the next 30 days
     */
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

    /**
     * Check if the selected dates for the reservation are available
     *
     * @param availableDates       List of available dates
     * @param reservationStartDate booking starting date
     * @param reservationEndDate   booking end date
     * @param reservationDays      number of days
     * @return true / false
     */
    private boolean isReservationPossible(List<LocalDate> availableDates, LocalDate
            reservationStartDate, LocalDate reservationEndDate, long reservationDays) {

        //Scan through available dates,
        for (int i = 0; i < availableDates.size() - 1; i++) {
            LocalDate firstFreeDay = availableDates.get(i);
            if (reservationStartDate.isEqual(firstFreeDay)) {
                //if reservation for more than 1 day, check the second available date
                if (reservationDays > 0) {
                    for (int j = i + 1; j < availableDates.size() - 1; j++) {
                        LocalDate secondFreeDay = availableDates.get(j);
                        if (secondFreeDay.isBefore(reservationEndDate) || secondFreeDay.isEqual(reservationEndDate)) {
                            long daysBetween = firstFreeDay.until(secondFreeDay, ChronoUnit.DAYS);

                            if (daysBetween >= reservationDays) {
                                return true;
                            }
                        }
                    }
                } else {
                    //reservation for 1 day and that date is available
                    return true;
                }
            }
        }

        return false;
    }

    private Reservation saveReservation(LocalDate reservationStartDate, LocalDate reservationEndDate) {
        Reservation reservation = new Reservation(reservationStartDate, reservationEndDate, ReservationStatus.CONFIRMED);
        return reservationRepository.saveAndFlush(reservation);
    }

    private Reservation updateReservation(Reservation reservation, LocalDate reservationStartDate, LocalDate
            reservationEndDate) {
        reservation.setFromDate(reservationStartDate);
        reservation.setToDate(reservationEndDate);
        reservation.setStatus(ReservationStatus.UPDATED);
        return reservationRepository.saveAndFlush(reservation);
    }

    private void deleteReservation(Long reservationId) {
        reservationRepository.deleteById(reservationId);
    }

    private ReservationResponse createReservationResponse(Reservation reservation) {
        return new ReservationResponse(reservation.getReservationId(), reservation.getFromDate(),
                reservation.getToDate(), reservation.getStatus());
    }

}
