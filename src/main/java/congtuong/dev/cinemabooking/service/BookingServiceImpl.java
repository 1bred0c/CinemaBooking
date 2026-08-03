package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.BookingCreateRequest;
import congtuong.dev.cinemabooking.dto.response.BookingResponse;
import congtuong.dev.cinemabooking.dto.response.BookingSummaryResponse;
import congtuong.dev.cinemabooking.dto.response.MyBookingResponse;
import congtuong.dev.cinemabooking.entity.*;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatHoldStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.exception.BookingException;
import congtuong.dev.cinemabooking.mapper.BookingMapper;
import congtuong.dev.cinemabooking.repository.BookingItemRepository;
import congtuong.dev.cinemabooking.repository.BookingRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatHoldItemRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatHoldRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import congtuong.dev.cinemabooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final ShowSeatHoldRepository showSeatHoldRepository;
    private final ShowSeatHoldItemRepository showSeatHoldItemRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponse createBooking(
            UUID currentUserId,
            BookingCreateRequest request
    ) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BookingException("User not found"));
        if (!currentUser.isActive()) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "User is not active"
            );
        }

        ShowSeatHold currentHold = showSeatHoldRepository
                .findByIdAndUserIdForUpdate(request.holdId(), currentUserId)
                .orElseThrow(() -> new BookingException("Hold not found"));
        if (currentHold.getStatus() != ShowSeatHoldStatus.ACTIVE) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "Hold is not active"
            );
        }
        Instant now = Instant.now();

        if (!currentHold.getExpiresAt().isAfter(now)) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "Hold has expired"
            );
        }

        if (bookingRepository.existsByHoldId(request.holdId())) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "Booking already exists for this hold"
            );
        }

        List<ShowSeatHoldItem> holdItems = showSeatHoldItemRepository.findAllByShowSeatHoldId(request.holdId());
        if (holdItems.isEmpty()) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "Hold does not contain any seats"
            );
        }

        List<ShowSeat> showSeats = showSeatRepository.findAllByIdForUpdate(
                holdItems.stream().map(ShowSeatHoldItem::getShowSeat).map(ShowSeat::getId).toList()
        );

        if (showSeats.size() != holdItems.size()) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "One or more held seats no longer exist"
            );
        }

        Map<UUID, ShowSeat> lockedSeatsById = showSeats.stream()
                .collect(Collectors.toMap(
                        ShowSeat::getId,
                        Function.identity()
                ));

        for (ShowSeatHoldItem holdItem : holdItems) {
            ShowSeat lockedSeat = lockedSeatsById.get(holdItem.getShowSeat().getId());
            if (lockedSeat == null || lockedSeat.getStatus() != ShowSeatStatus.HELD) {
                throw new BookingException(
                        HttpStatus.CONFLICT,
                        "One or more seats are no longer held"
                );
            }
            if (!lockedSeat.getShowtime().getId().equals(currentHold.getShowtime().getId())) {
                throw new BookingException(
                        HttpStatus.CONFLICT,
                        "Held seat does not belong to the hold showtime"
                );
            }
        }

        BigDecimal totalPrice = holdItems.stream()
                .map(ShowSeatHoldItem::getHeldPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Booking newBooking = Booking.builder()
                .user(currentUser)
                .hold(currentHold)
                .showtime(currentHold.getShowtime())
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(totalPrice)
                .paymentExpiresAt(currentHold.getExpiresAt())
                .build();

        newBooking = bookingRepository.save(newBooking);

        List<BookingItem> bookingItems = new ArrayList<>();
        for (ShowSeatHoldItem holdItem : holdItems) {
            ShowSeat showSeat = lockedSeatsById.get(holdItem.getShowSeat().getId());
            BookingItem bookingItem = BookingItem.builder()
                    .booking(newBooking)
                    .showSeat(showSeat)
                    .seatRow(showSeat.getSeat().getRow())
                    .seatNumber(showSeat.getSeat().getNumber())
                    .seatType(showSeat.getSeat().getType())
                    .unitPrice(holdItem.getHeldPrice())
                    .build();
            bookingItems.add(bookingItem);
        }
        bookingItemRepository.saveAll(bookingItems);
        currentHold.setStatus(ShowSeatHoldStatus.CONFIRMED);

        return bookingMapper.toResponse(newBooking, bookingItems);
    }

    @Override
    public BookingResponse getBooking(UUID currentUserId, UUID bookingId) {
        Booking booking = bookingRepository
                .findByIdAndUserId(bookingId, currentUserId)
                .orElseThrow(() -> new BookingException("Booking not found"));
        return bookingMapper.toResponse(
                booking,
                bookingItemRepository.findAllByBookingId(bookingId)
        );
    }

    @Override
    public Page<BookingSummaryResponse> getUserBookings(
            UUID currentUserId,
            Pageable pageable
    ) {
        return bookingRepository.findAllByUserId(
                currentUserId,
                pageable
        ).map(bookingMapper::toSummaryResponse);
    }

    @Override
    public Page<MyBookingResponse> getMyBookings(
            UUID currentUserId,
            BookingStatus status,
            Pageable pageable
    ) {
        return bookingRepository.findMyBookings(
                currentUserId,
                status,
                pageable
        ).map(this::toMyBookingResponse);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID currentUserId, UUID bookingId) {
        Booking currentBooking = bookingRepository
                .findByIdAndUserIdForUpdate(bookingId, currentUserId)
                .orElseThrow(() -> new BookingException("Booking not found"));

        if (currentBooking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "Only pending-payment bookings can be cancelled"
            );
        }

        ShowSeatHold showSeatHold = showSeatHoldRepository
                .findByIdAndUserIdForUpdate(
                        currentBooking.getHold().getId(),
                        currentUserId
                )
                .orElseThrow(() -> new BookingException("Hold ID not found"));
        if (showSeatHold.getStatus() != ShowSeatHoldStatus.CONFIRMED) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "Booking hold is not in a cancellable state"
            );
        }

        List<BookingItem> bookingItems =
                bookingItemRepository.findAllByBookingId(bookingId);
        if (bookingItems.isEmpty()) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "Booking does not contain any seats"
            );
        }

        List<UUID> showSeatIds = bookingItems.stream()
                .map(item -> item.getShowSeat().getId())
                .sorted()
                .toList();
        List<ShowSeat> lockedShowSeats =
                showSeatRepository.findAllByIdForUpdate(showSeatIds);
        if (lockedShowSeats.size() != showSeatIds.size()) {
            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "One or more booking seats no longer exist"
            );
        }

        for (ShowSeat showSeat : lockedShowSeats) {
            if (showSeat.getStatus() != ShowSeatStatus.HELD) {
                throw new BookingException(
                        HttpStatus.CONFLICT,
                        "One or more booking seats are no longer held"
                );
            }
            showSeat.setStatus(ShowSeatStatus.AVAILABLE);
        }

        showSeatHold.setStatus(ShowSeatHoldStatus.CANCELLED);
        currentBooking.markCancelled(Instant.now());

        return bookingMapper.toResponse(currentBooking, bookingItems);
    }

    private MyBookingResponse toMyBookingResponse(Booking booking) {
        ShowTime showtime = booking.getShowtime();
        Movie movie = showtime.getMovie();
        Room room = showtime.getRoom();
        Cinema cinema = room.getCinema();
        return new MyBookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getTotalAmount(),
                showtime.getId(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                movie.getId(),
                movie.getTitle(),
                movie.getPosterUrl(),
                cinema.getId(),
                cinema.getName(),
                cinema.getAddress(),
                room.getId(),
                room.getName(),
                booking.getPaymentExpiresAt(),
                booking.getConfirmedAt(),
                booking.getCancelledAt(),
                booking.getCreatedAt()
        );
    }
}
