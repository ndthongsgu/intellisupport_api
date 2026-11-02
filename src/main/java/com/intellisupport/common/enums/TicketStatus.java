package com.intellisupport.common.enums;

import java.util.Set;

/**
 * Status lifecycle of a support ticket.
 * Implements state transition validation according to business rules.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_CUSTOMER,
    RESOLVED,
    CLOSED;

    /**
     * Checks if transitioning from the current status to the target status is allowed.
     *
     * @param targetStatus The desired next status
     * @return true if the transition is allowed, false otherwise
     */
    public boolean canTransitionTo(TicketStatus targetStatus) {
        if (targetStatus == null || this == targetStatus) {
            return false;
        }

        return switch (this) {
            case OPEN -> Set.of(IN_PROGRESS, CLOSED).contains(targetStatus);
            case IN_PROGRESS -> Set.of(WAITING_CUSTOMER, RESOLVED, CLOSED).contains(targetStatus);
            case WAITING_CUSTOMER -> Set.of(IN_PROGRESS, RESOLVED, CLOSED).contains(targetStatus);
            case RESOLVED -> Set.of(IN_PROGRESS, CLOSED).contains(targetStatus);
            case CLOSED -> Set.of(IN_PROGRESS).contains(targetStatus); // Re-open
        };
    }
}
