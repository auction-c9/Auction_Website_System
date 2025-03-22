package com.example.auction_management.service.impl;

import com.example.auction_management.dto.BidDTO;
import com.example.auction_management.dto.BidResponseDTO;
import com.example.auction_management.exception.*;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import com.example.auction_management.service.IBidService;
import com.example.auction_management.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class BidService implements IBidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    @Autowired
    private JavaMailSender mailSender;

    // ---------------------- CRUD BASIC ----------------------

    @Override
    public List<Bid> findAll() {
        return bidRepository.findAll();
    }

    @Override
    public Optional<Bid> findById(Integer id) {
        return bidRepository.findById(id);
    }

    @Override
    public Bid save(Bid bid) {
        return bidRepository.save(bid);
    }

    @Override
    public void deleteById(Integer id) {
        Bid bid = bidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá thầu với ID: " + id));
        bidRepository.delete(bid);
    }

    public List<BidResponseDTO> getBidHistoryByCustomerId(Integer customerId) {
        List<Bid> bids = bidRepository.findByCustomer_CustomerIdOrderByBidTimeDesc(customerId);
        return bids.stream()
                .map(this::mapToBidResponseDTOByCustomer)
                .collect(Collectors.toList());
    }

    // Sửa lại phương thức mapToBidResponseDTO để thêm thông tin từ Auction
    private BidResponseDTO mapToBidResponseDTOByCustomer(Bid bid) {
        Auction auction = bid.getAuction();
        Product product = auction.getProduct();
        return BidResponseDTO.builder()
                .bidId(bid.getBidId())
                .auctionId(bid.getAuction().getAuctionId())
                .customerId(bid.getCustomer().getCustomerId())
                .bidAmount(bid.getBidAmount())
                .bidTime(bid.getBidTime())
                .isWinner(bid.getIsWinner())
                .message("Thông tin đấu giá")
                .user(bid.getAccount())
                .registrationDate(bid.getAuction().getCreatedAt())
                .productName(product.getName())
                .auctionStatus(auction.getStatus().toString())
                .build();
    }

    // ---------------------- PLACE BID LOGIC ----------------------
    @Override
    @Transactional
    public BidResponseDTO placeBid(BidDTO bidDTO) {
        Auction auction = auctionRepository.findById(bidDTO.getAuctionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá!"));

        validateAuctionStatus(auction);

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Customer customer = customerRepository.findByAccountUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng!"));

        if (auction.getProduct().getAccount().getAccountId().equals(customer.getAccount().getAccountId())) {
            throw new InvalidActionException("Bạn không thể đặt giá cho bài đấu giá của chính mình!");
        }

        // ✅ Kiểm tra giao dịch đặt cọc trước khi đặt giá
        boolean hasDeposit = checkDeposit(customer.getCustomerId(), auction.getAuctionId());
        if (!hasDeposit) {
            throw new InvalidActionException("Bạn cần hoàn tất đặt cọc để tham gia đấu giá!");
        }

        validateBidAmount(auction, bidDTO.getBidAmount());

        resetOldBids(auction);

        Bid newBid = new Bid();
        newBid.setAuction(auction);
        newBid.setCustomer(customer);
        newBid.setBidAmount(bidDTO.getBidAmount());
        newBid.setBidTime(LocalDateTime.now());
        newBid.setIsWinner(Boolean.TRUE);
        Bid savedBid = bidRepository.save(newBid);

        // Gửi thông báo đến người bán
        Customer seller = auction.getProduct().getAccount().getCustomer();

        System.out.println("Owner Account ID: " + seller);
        System.out.println("Sending notification to seller: " + seller);
        notificationService.sendNotification(seller.getCustomerId(), "Có người vừa đặt giá mới cho sản phẩm của bạn!");

        // Gửi thông báo đến những người tham gia đấu giá (ngoại trừ người đặt giá hiện tại)
        // Giả sử bạn có một phương thức trong bidRepository để lấy danh sách các customer đã tham gia
        List<Customer> participants = bidRepository.findDistinctCustomersByAuctionId(auction.getAuctionId());
        for (Customer participant : participants) {
            if (!participant.getCustomerId().equals(customer.getCustomerId())) {
                System.out.println("Sending notification to participant: " + participant.getCustomerId());
                notificationService.sendNotification(participant.getCustomerId(), "Có người vừa đặt giá cao hơn bạn trong phiên đấu giá!");
            }
        }

        return mapToBidResponseDTO(savedBid);
    }


    // ---------------------- HISTORY & WINNER ----------------------

    public List<BidResponseDTO> getBidHistoryByAuctionId(Integer auctionId) {
        List<Bid> bids = bidRepository.findByAuction_AuctionIdOrderByBidAmountDesc(auctionId);
        if (bids == null || bids.isEmpty()) {
            return new ArrayList<>();
        }

        return bids.stream().map(bid -> BidResponseDTO.builder()
                        .bidId(bid.getBidId())
                        .auctionId(bid.getAuction().getAuctionId())
                        .customerId(bid.getCustomer().getCustomerId())
                        .bidAmount(bid.getBidAmount())
                        .bidTime(bid.getBidTime())
                        .isWinner(bid.getIsWinner()) // ✅ Trả về Boolean thay vì Integer
                        .message("Lịch sử đấu giá")
                        .user(bid.getAccount())
                        .build())
                .collect(Collectors.toList());
    }

    public Optional<Bid> getCurrentHighestBid(Integer auctionId) {
        return bidRepository.findTopByAuctionOrderByBidAmountDesc(auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá!")));
    }

    public Integer getCustomerIdFromUsername(String username) {
        Customer customer = customerRepository.findByAccountUsername(username)
                .orElseThrow(() -> new CustomerNotFoundException("Không tìm thấy tài khoản khách hàng!"));
        return customer.getCustomerId();
    }

    // ---------------------- SUPPORT METHODS ----------------------
    private void validateAuctionStatus(Auction auction) {
        if (!auction.getStatus().equals(Auction.AuctionStatus.active)) {
            throw new InvalidActionException("Phiên đấu giá không còn hoạt động!");
        }
        if (auction.getAuctionEndTime().isBefore(LocalDateTime.now())) {
            throw new InvalidActionException("Phiên đấu giá đã kết thúc!");
        }
    }

    private void validateBidAmount(Auction auction, BigDecimal bidAmount) {
        BigDecimal minNextBid = auction.getCurrentPrice().add(auction.getBidStep());
        if (bidAmount.compareTo(minNextBid) < 0) {
            throw new InvalidActionException("Giá đấu phải tối thiểu từ " + minNextBid + " trở lên!");
        }
    }

    private void resetOldBids(Auction auction) {
        List<Bid> oldBids = bidRepository.findByAuction(auction);
        oldBids.forEach(b -> {
            b.setIsWinner(Boolean.FALSE); // ✅ Sử dụng Boolean.FALSE thay vì false
            System.out.println("Reset isWinner: " + b.getBidId() + " -> false");
        });
        bidRepository.saveAll(oldBids);
    }

    private BidResponseDTO mapToBidResponseDTO(Bid bid) {
        System.out.println("Bid ID: " + bid.getBidId() + " | isWinner: " + bid.getIsWinner());
        return new BidResponseDTO(
                bid.getBidId(),
                bid.getAuction().getAuctionId(),
                bid.getCustomer().getCustomerId(),
                bid.getBidAmount(),
                bid.getBidTime(),
                bid.getIsWinner(), // ✅ Trả về Boolean thay vì Integer
                "Đặt giá thành công!",
                bid.getAccount()
        );
    }

    /**
     * Kiểm tra xem người dùng đã thanh toán đặt cọc hay chưa
     */
    public boolean checkDeposit(Integer customerId, Integer auctionId) {
        return transactionRepository.existsByCustomer_CustomerIdAndAuction_AuctionIdAndStatus(customerId, auctionId, "SUCCESS");
    }


    /**
     * Lưu thông tin giao dịch đặt cọc
     */
    public void saveDepositTransaction(Integer customerId, Integer auctionId, Double amount, String method) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng!"));

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá!"));

        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setAuction(auction);
        transaction.setAmount(amount);
        transaction.setPaymentMethod(method);
        transaction.setTransactionType("DEPOSIT");
        transaction.setStatus("SUCCESS");
        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
    }

    @Scheduled(fixedRate = 600000) // Mỗi 60 giây
    @Transactional
    public void updateAuctionStatuses() {
        // Cập nhật trạng thái của các phiên đấu giá dựa trên thời gian hiện tại
        auctionRepository.updateAuctionStatuses(LocalDateTime.now());

        // Lấy danh sách các phiên đấu giá đã kết thúc mà chưa được thông báo (winnerNotified = false)
        List<Auction> endedAuctions = auctionRepository.findByAuctionEndTimeBeforeAndWinnerNotifiedFalse(LocalDateTime.now());
        for (Auction auction : endedAuctions) {
            if (auction.getAuctionEndTime().plusMinutes(2).isBefore(LocalDateTime.now())) {
                continue;
            }

            Optional<Bid> winningBid = getCurrentHighestBid(auction.getAuctionId());
            if (winningBid.isPresent()) {
                Customer winner = winningBid.get().getCustomer();
                sendWinnerEmail(winner.getEmail(), auction);

                Customer seller = customerRepository.findByAccountUsername(
                        auction.getProduct().getAccount().getUsername()
                ).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người bán!"));

                sendOwnerNotificationEmail(auction, seller, winner);

                // Gửi email cho người đấu giá thất bại
                sendLoserEmails(auction, winner);
            }

            auction.setWinnerNotified(true);
            auctionRepository.save(auction);
        }

    }

    private void sendWinnerEmail(String recipientEmail, Auction auction) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Thông báo kết thúc phiên đấu giá");
        message.setText("Chúc mừng! Bạn đã chiến thắng phiên đấu giá số " + auction.getAuctionId() +
                ". Vui lòng thanh toán trong vòng 3 ngày, nếu không bạn sẽ mất tiền đặt cọc.");
        mailSender.send(message);
    }

    private void sendOwnerNotificationEmail(Auction auction, Customer seller, Customer winner) {
        String recipientEmail = seller.getEmail();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Thông báo kết thúc phiên đấu giá");
        message.setText("Phiên đấu giá số " + auction.getAuctionId() + " của bạn đã kết thúc. " +
                "Người chiến thắng là: " + winner.getAccount().getUsername() +
                ". Vui lòng liên hệ với người chiến thắng và hoàn tất giao dịch trong vòng 3 ngày.");
        mailSender.send(message);
    }
    private void sendLoserEmails(Auction auction, Customer winner) {
        List<Bid> allBids = bidRepository.findByAuction_AuctionIdOrderByBidAmountDesc(auction.getAuctionId());

        for (Bid bid : allBids) {
            Customer bidder = bid.getCustomer();
            if (!bidder.getCustomerId().equals(winner.getCustomerId())) {
                sendLoserEmail(bidder.getEmail(), auction);
            }
        }
    }

    private void sendLoserEmail(String recipientEmail, Auction auction) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Thông báo kết thúc phiên đấu giá");
        message.setText("Rất tiếc! Bạn đã không chiến thắng phiên đấu giá số " + auction.getAuctionId() +
                ". Tiền đặt cọc sẽ được hoàn trả vào tài khoản của bạn trong 3 ngày làm việc tiếp theo.");
        mailSender.send(message);
    }
    public byte[] exportFailedBidsToExcel() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);

        List<Transaction> failedDeposits = transactionRepository.findFailedDeposits(yesterday, now);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Failed Bids");

            // Tạo tiêu đề
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Auction ID", "Product Name", "Customer Name", "Identity Card", "Phone", "Bank Account", "Bank Name", "Deposit Amount"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            // Thêm dữ liệu
            int rowNum = 1;
            for (Transaction transaction : failedDeposits) {
                Row row = sheet.createRow(rowNum++);
                Auction auction = transaction.getAuction();
                Customer customer = transaction.getCustomer();

                row.createCell(0).setCellValue(auction.getAuctionId());
                row.createCell(1).setCellValue(auction.getProduct().getName());
                row.createCell(2).setCellValue(customer.getName());
                row.createCell(3).setCellValue(customer.getIdentityCard());
                row.createCell(4).setCellValue(customer.getPhone());
                row.createCell(5).setCellValue(customer.getBankAccount());
                row.createCell(6).setCellValue(customer.getBankName());
                row.createCell(7).setCellValue(transaction.getAmount().doubleValue());
            }

            // Ghi file vào byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel", e);
        }
    }


    // Tạo style cho header
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    public void sendFailedBidsReportToAdmin() {
        byte[] excelFile = exportFailedBidsToExcel();
        List<String> adminEmails = customerRepository.findAdminEmails();

        if (adminEmails.isEmpty()) {
            throw new RuntimeException("Không tìm thấy email của ADMIN!");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(adminEmails.toArray(new String[0]));
            helper.setSubject("Báo cáo danh sách đấu giá thất bại");
            helper.setText("Gửi ADMIN,\n\nĐây là danh sách những tài khoản đã tham gia đấu giá nhưng không chiến thắng.");

            // Đính kèm file Excel
            helper.addAttachment("Failed_Bids_Report.xlsx", new ByteArrayResource(excelFile));

            mailSender.send(message);
            System.out.println("Email báo cáo đã được gửi thành công!");
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi khi gửi email!", e);
        }
    }
    @Scheduled(cron = "0 53 15 * * ?")
    public void scheduleFailedBidsReport() {
        sendFailedBidsReportToAdmin();
    }
}