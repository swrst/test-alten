package com.alten.booking;

import com.alten.booking.controller.BookingController;
import com.alten.booking.entity.Reservation;
import com.alten.booking.exception.InvalidRequestException;
import com.alten.booking.exception.RoomBookedException;
import com.alten.booking.model.ReservationRequest;
import com.alten.booking.model.ReservationResponse;
import com.alten.booking.model.Room;
import com.alten.booking.model.UpdateRequest;
import com.alten.booking.repository.ReservationRepository;
import com.alten.booking.utils.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class BookingApplicationTests {

	@InjectMocks
	private BookingController bookingController;

	@Mock
	private ReservationRepository reservationRepository;

	@Test
	void testCheckRoomAvailability() {
		//Mocking return of NO reservations
		when(reservationRepository.findAllByOrderByFromDateAsc()).thenReturn(new ArrayList<>());
		Room room = bookingController.getRoomAvailability();

		//Available dates should contain every date from today to +30 days
		assertNotNull(room);
		assertTrue(room.getNextAvailableDates().size() > 28);
	}

	@Test
	void testRoomReservation() {
		LocalDate reservationStartDate = LocalDate.now().plusDays(3);

		//Mocking return of NO reservations
		when(reservationRepository.findAllByOrderByFromDateAsc()).thenReturn(new ArrayList<>());

		Reservation reservation = new Reservation(reservationStartDate, reservationStartDate.plusDays(3), ReservationStatus.CONFIRMED);
		reservation.setReservationId(1L);
		when(reservationRepository.saveAndFlush(any(Reservation.class))).thenReturn(reservation);

		//VALID REQUEST
		ReservationRequest reservationRequest = new ReservationRequest(2, reservationStartDate);

		ReservationResponse reservationResponse = bookingController.roomReservation(reservationRequest);
		//SHOULD BE OK
		assertNotNull(reservationResponse);
		assertTrue(reservationResponse.getStartDate().isEqual(reservationStartDate));
		assertEquals(3L, reservationResponse.getStartDate().until(reservationResponse.getEndDate(), ChronoUnit.DAYS));
	}

	@Test
	void testReservationUpdate() {
		LocalDate newReservationStartDate = LocalDate.now().plusDays(10);
		LocalDate newReservationEndDate = newReservationStartDate.plusDays(2);

		LocalDate oldReservationStartDate = LocalDate.now().plusDays(3);

		//Mocking return of 1 reservation
		Reservation reservation = new Reservation(oldReservationStartDate, oldReservationStartDate.plusDays(3), ReservationStatus.CONFIRMED);
		reservation.setReservationId(1L);
		when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

		Reservation reservationUpdated = new Reservation(newReservationStartDate, newReservationEndDate, ReservationStatus.UPDATED);
		reservationUpdated.setReservationId(1L);
		when(reservationRepository.saveAndFlush(any(Reservation.class))).thenReturn(reservationUpdated);

		//VALID REQUEST
		UpdateRequest updateRequest = new UpdateRequest(1L, newReservationStartDate, newReservationEndDate);

		ReservationResponse reservationResponse = bookingController.reservationUpdate(updateRequest);

		//Should update the reservation with new dates
		assertNotNull(reservationResponse);
		assertTrue(reservationResponse.getStartDate().isEqual(newReservationStartDate));
		assertTrue(reservationResponse.getEndDate().isEqual(newReservationEndDate));
		assertEquals(2L, reservationResponse.getStartDate().until(reservationResponse.getEndDate(), ChronoUnit.DAYS));
		assertEquals(ReservationStatus.UPDATED, reservationResponse.getStatus());
	}

	@Test
	void testCancelReservation() {
		LocalDate reservationStartDate = LocalDate.now().plusDays(1);
		LocalDate reservationEndDate = reservationStartDate.plusDays(3);

		//Mocking return of NO reservations
		Reservation reservation = new Reservation(reservationStartDate, reservationEndDate, ReservationStatus.CONFIRMED);
		reservation.setReservationId(1L);
		when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

		Mockito.doNothing().when(reservationRepository).deleteById(any());

		//VALID REQUEST
		ReservationResponse reservationResponse = bookingController.cancelReservation(reservation.getReservationId());

		//SHOULD BE OK
		assertNotNull(reservationResponse);
		assertEquals(reservation.getReservationId(),reservationResponse.getReservationId());
		assertEquals(ReservationStatus.CANCELLED, reservationResponse.getStatus());
	}

	@Test
	void testReservationWithMoreThan3Days() {
		LocalDate reservationStartDate = LocalDate.now().plusDays(1);
		//creating reservation with 6 days, should throw exception. (range 1-3)
		ReservationRequest reservationRequest = new ReservationRequest(6, reservationStartDate);

		assertThrows(InvalidRequestException.class, () -> bookingController.roomReservation(reservationRequest));

	}

	@Test
	void testReservationOver30Days() {
		//Reservation over 30 days from now, should throw exception
		LocalDate reservationStartDate = LocalDate.now().plusDays(35);
		ReservationRequest reservationRequest = new ReservationRequest(2, reservationStartDate);

		assertThrows(InvalidRequestException.class, () -> bookingController.roomReservation(reservationRequest));

	}

	@Test
	void testReservationLessThen1Day() {
		//Reservation for 0 days, should throw exception
		LocalDate reservationStartDate = LocalDate.now().plusDays(1);
		ReservationRequest reservationRequest = new ReservationRequest(0, reservationStartDate);

		assertThrows(InvalidRequestException.class, () -> bookingController.roomReservation(reservationRequest));

	}

	@Test
	void testReservationNullRequest() {
		//Reservation with null request, should throw exception
		assertThrows(InvalidRequestException.class, () -> bookingController.roomReservation(null));

	}

	@Test
	void testReservationPastDate() {
		//Reservation with past date, should throw exception
		LocalDate reservationStartDate = LocalDate.now().minusDays(10);
		ReservationRequest reservationRequest = new ReservationRequest(3, reservationStartDate);

		assertThrows(InvalidRequestException.class, () -> bookingController.roomReservation(reservationRequest));
	}

	@Test
	void testReservationForToday() {
		//Reservation for the same day of booking, should throw exception
		LocalDate reservationStartDate = LocalDate.now();
		ReservationRequest reservationRequest = new ReservationRequest(2, reservationStartDate);

		assertThrows(InvalidRequestException.class, () -> bookingController.roomReservation(reservationRequest));
	}

	@Test
	void testReservationRoomAlreadyBooked() {
		//Reservation for already booked dates, should throw exception
		LocalDate existingReservationStartDate = LocalDate.now().plusDays(3);

		Reservation existingReservation = new Reservation(existingReservationStartDate, existingReservationStartDate.plusDays(3), ReservationStatus.CONFIRMED);
		existingReservation.setReservationId(1L);

		List<Reservation> reservationList = new ArrayList<>();
		reservationList.add(existingReservation);

		//Mocking return of 1 reservations
		when(reservationRepository.findAllByOrderByFromDateAsc()).thenReturn(reservationList);

		//VALID REQUEST
		ReservationRequest reservationRequest = new ReservationRequest(2, existingReservationStartDate);
		assertThrows(RoomBookedException.class, () -> bookingController.roomReservation(reservationRequest));

	}

}
