package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {

    @Query("""
            select bi from BookingItem bi
            join fetch bi.showSeat
            where bi.booking.id = :bookingId
            order by bi.seatRow, bi.seatNumber, bi.id
            """)
    List<BookingItem> findAllByBookingId(@Param("bookingId") UUID bookingId);
}
