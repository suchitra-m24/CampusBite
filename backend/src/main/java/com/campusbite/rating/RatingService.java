package com.campusbite.rating;

import com.campusbite.common.exception.BadRequestException;
import com.campusbite.common.exception.ConflictException;
import com.campusbite.common.exception.ResourceNotFoundException;
import com.campusbite.order.Order;
import com.campusbite.order.OrderRepository;
import com.campusbite.order.OrderStatus;
import com.campusbite.rating.dto.RatingDto;
import com.campusbite.rating.dto.RatingRequest;
import com.campusbite.rating.dto.RatingSummaryDto;
import com.campusbite.user.Role;
import com.campusbite.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final OrderRepository orderRepository;

    public RatingService(
            RatingRepository ratingRepository,
            OrderRepository orderRepository) {

        this.ratingRepository = ratingRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Submit a rating for an order.
     *
     * A student can rate only their own order and only after
     * the order has been picked up.
     */
    @Transactional
    public RatingDto submit(
            Long orderId,
            User requester,
            RatingRequest request) {

        Order order = findOrderOwnedBy(orderId, requester);

        // Rating is allowed only after pickup
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new BadRequestException(
                    "You can only rate an order after it's been picked up"
            );
        }

        // Prevent duplicate ratings
        if (ratingRepository.existsByOrderId(orderId)) {
            throw new ConflictException(
                    "This order has already been rated"
            );
        }

        // Create rating
        Rating rating = new Rating(
                order,
                request.stars(),
                request.comment()
        );

        // Save and convert to DTO
        Rating savedRating = ratingRepository.save(rating);

        return RatingDto.from(savedRating);
    }

    /**
     * Get the rating for a particular order.
     *
     * The requester must either own the order or be staff/admin.
     */
    public RatingDto getForOrder(
            Long orderId,
            User requester) {

        Order order = findOrderOwnedBy(orderId, requester);

        return ratingRepository
                .findByOrderId(order.getId())
                .map(RatingDto::from)
                .orElse(null);
    }

    /**
     * Get rating summary for admin analytics.
     *
     * RatingSummaryDto expects:
     *     (double average, long count)
     */
    public RatingSummaryDto getAdminSummary() {

        long count = ratingRepository.countAll();

        Double average = ratingRepository.averageStars();

        return new RatingSummaryDto(
                average == null ? 0.0 : average,
                count
        );
    }

    /**
     * Find an order and verify that the requester has permission
     * to access it.
     *
     * The requester can access the order if:
     * - They are the student who owns the order, OR
     * - They are STAFF, OR
     * - They are ADMIN.
     */
    private Order findOrderOwnedBy(
            Long orderId,
            User requester) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found")
                );

        boolean isOwner =
                order.getStudent()
                        .getId()
                        .equals(requester.getId());

        boolean isStaffOrAdmin =
                requester.getRole() == Role.STAFF
                        || requester.getRole() == Role.ADMIN;

        if (!isOwner && !isStaffOrAdmin) {
            throw new BadRequestException(
                    "You do not have access to this order"
            );
        }

        return order;
    }
}