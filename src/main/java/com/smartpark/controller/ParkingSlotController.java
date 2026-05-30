package com.smartpark.controller;

import com.smartpark.model.ParkingSlot;
import com.smartpark.service.ParkingSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho parking slot - public access
 * Hiển thị tình trạng bãi đỗ xe cho mọi người xem (không cần đăng nhập)
 */
@Controller
public class ParkingSlotController {

    @Autowired
    private ParkingSlotService parkingSlotService;

    /**
     * Trang xem bãi đỗ xe công khai
     * GET /parking-lot-view
     */
    @GetMapping("/parking-lot-view")
    public String parkingLotView(Model model) {
        // Lấy thống kê
        ParkingSlotService.SlotStats stats = parkingSlotService.getStats();
        
        model.addAttribute("total", stats.total());
        model.addAttribute("filled", stats.filled());
        model.addAttribute("empty", stats.empty());
        model.addAttribute("motoTotal", stats.motoTotal());
        model.addAttribute("motoFilled", stats.motoFilled());
        model.addAttribute("motoEmpty", stats.motoEmpty());
        model.addAttribute("carTotal", stats.carTotal());
        model.addAttribute("carFilled", stats.carFilled());
        model.addAttribute("carEmpty", stats.carEmpty());
        model.addAttribute("pct", stats.pct());
        
        // Lấy danh sách slots
        model.addAttribute("motoSlots", parkingSlotService.getSlotsByZone("motorbike"));
        model.addAttribute("carSlots", parkingSlotService.getSlotsByZone("car"));
        
        // Rows cho hiển thị
        model.addAttribute("motoRows", List.of("A", "B", "C", "D", "E"));
        model.addAttribute("carRows", List.of("A", "B", "C"));
        
        return "parking-lot-view";
    }


}
