package com.smartpark.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "parking_slots")
@Data @NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParkingSlot {

    @Id
    private String id;

    private String zone;       // motorbike / car
    private boolean occupied;
    private boolean reserved;  // đặt trước — hiển thị đỏ trên map
    private String licensePlate;
    private String checkinTime;

    public ParkingSlot(String id, String zone) {
        this.id = id;
        this.zone = zone;
        this.occupied = false;
        this.reserved = false;
    }
}
