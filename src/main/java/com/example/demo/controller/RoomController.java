package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Room;
import com.example.demo.model.User;
import com.example.demo.service.RoomService;
import com.example.demo.service.UserService;



@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService roomService;
    private final UserService userService;

    public RoomController(RoomService roomService, UserService userService)  {
        this.roomService = roomService;
        this.userService = userService;
    }

    @GetMapping
     public List<Room> getAllRooms(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        if (user.getRole().getName().equals("USER")) {
            return roomService.getRoomsByManager(user.getId());
        } else {
            return roomService.getAllRooms();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        Room room = roomService.getRoomById(id);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (user.getRole().getName().equals("USER") && 
            !roomService.isRoomManager(id, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(room);
    }

     @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        if (room.getManager() == null || room.getManager().getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        User manager = userService.getUserById(room.getManager().getId());
        if (manager == null) {
            return ResponseEntity.badRequest().build();
        }
        
        room.setManager(manager);
        
        Room createdRoom = roomService.createRoom(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRoom);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room roomDetails) {
        if (roomDetails.getManager() != null && roomDetails.getManager().getId() != null) {
            if (userService.getUserById(roomDetails.getManager().getId()) == null) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        Room updatedRoom = roomService.updateRoom(id, roomDetails);
        if (updatedRoom != null) {
            return ResponseEntity.ok(updatedRoom);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // DELETE /api/rooms/{id} - удалить комнату
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        boolean deleted = roomService.deleteRoom(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    
}