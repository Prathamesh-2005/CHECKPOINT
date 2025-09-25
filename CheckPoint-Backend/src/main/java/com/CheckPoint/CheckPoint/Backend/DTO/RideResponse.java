package com.CheckPoint.CheckPoint.Backend.DTO;

import com.CheckPoint.CheckPoint.Backend.Model.RideStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class RideResponse {
    private UUID id;
    private DriverDto driver;
    private Double startLatitude;
    private Double startLongitude;
    private Double endLatitude;
    private Double endLongitude;
    private LocalDateTime departureTime;
    private BigDecimal price;
    private RideStatus status;
    private Integer availableSeats;
}
