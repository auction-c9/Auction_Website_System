//package com.example.auction_management.dto;
//
//import java.time.LocalDateTime;
//
//public class FollowResponseDTO {
//    public static FollowResponseDTO of(String message) {
//        return new FollowResponseDTO(message, null);
//    }
//
//    public static FollowResponseDTO status(boolean isFollowing) {
//        return new FollowResponseDTO(null, isFollowing);
//    }
//}
//
//// ErrorResponseDTO.java
//public record ErrorResponseDTO(String error, String message, LocalDateTime timestamp) {
//    public ErrorResponseDTO(String error, String message) {
//        this(error, message, LocalDateTime.now());
//    }
//}
