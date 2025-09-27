package com.CheckPoint.CheckPoint.Backend.Service;

import com.CheckPoint.CheckPoint.Backend.DTO.UpdateBookingStatusRequest;
import com.CheckPoint.CheckPoint.Backend.Model.*;
import com.CheckPoint.CheckPoint.Backend.Repository.BookingRepository;
import com.CheckPoint.CheckPoint.Backend.Repository.RideRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;

    public BookingService(BookingRepository bookingRepository, RideRepository rideRepository) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
    }

    @Transactional
    public Booking createBooking(UUID rideId, User passenger) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("Ride not found with id: " + rideId));

        if (!ride.getStatus().equals(RideStatus.REQUESTED)) {
            throw new IllegalStateException("This ride is no longer available for booking.");
        }
        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new IllegalArgumentException("You cannot book your own ride.");
        }
        bookingRepository.findByRideIdAndPassengerId(rideId, passenger.getId()).ifPresent(b -> {
            throw new IllegalStateException("You have already sent a request for this ride.");
        });

        Booking newBooking = new Booking();
        newBooking.setRide(ride);
        newBooking.setPassenger(passenger);
        newBooking.setStatus(BookingStatus.PENDING);

        return bookingRepository.save(newBooking);
    }

    @Transactional
    public Booking updateBookingStatus(UUID bookingId, UpdateBookingStatusRequest request, User driver) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        Ride ride = booking.getRide();

        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("You are not authorized to modify this booking.");
        }

        if (request.getStatus() == BookingStatus.ACCEPTED) {
            if (!ride.getStatus().equals(RideStatus.REQUESTED)) {
                throw new IllegalStateException("This ride has already been confirmed or cancelled.");
            }
            booking.setStatus(BookingStatus.ACCEPTED);
            ride.setStatus(RideStatus.CONFIRMED);

            List<Booking> otherPendingBookings = bookingRepository.findByRideAndStatus(ride, BookingStatus.PENDING);
            for (Booking otherBooking : otherPendingBookings) {
                if (!otherBooking.getId().equals(booking.getId())) {
                    otherBooking.setStatus(BookingStatus.REJECTED);
                    bookingRepository.save(otherBooking);
                }
            }

        } else if (request.getStatus() == BookingStatus.REJECTED) {
            booking.setStatus(BookingStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Invalid status update provided. Can only be 'ACCEPTED' or 'REJECTED'.");
        }

        rideRepository.save(ride);
        return bookingRepository.save(booking);
    }
}

