package com.example.auction_management.dto;

public class ChatMessageDTO {
    private Integer senderId; // ID của người gửi (User = customerId, Admin = accountId)
    private String senderRole; // "ROLE_USER" hoặc "ROLE_ADMIN"
    private Integer receiverId; // ID của người nhận (User nhận tin từ Admin)
    private String content; // Nội dung tin nhắn

    public ChatMessageDTO() {}
    public ChatMessageDTO(Integer senderId, String senderRole, Integer receiverId, String content) {
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.receiverId = receiverId;
        this.content = content;
    }
    public Integer getSenderId() {
        return senderId;
    }
    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }
    public String getSenderRole() {
        return senderRole;
    }
    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }
    public Integer getReceiverId() {
        return receiverId;
    }
    public void setReceiverId(Integer receiverId) {
        this.receiverId = receiverId;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

}
