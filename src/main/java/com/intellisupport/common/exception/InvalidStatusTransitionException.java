package com.intellisupport.common.exception;

import com.intellisupport.common.enums.ErrorCode;
import com.intellisupport.common.enums.TicketStatus;

/**
 * Thrown when attempting an illegal ticket status lifecycle transition.
 */
public class InvalidStatusTransitionException extends AppException {

    public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus newStatus) {
        super(ErrorCode.INVALID_STATUS_TRANSITION,
                String.format("Cannot transition ticket status from '%s' to '%s'", currentStatus, newStatus));
    }

    public InvalidStatusTransitionException(String message) {
        super(ErrorCode.INVALID_STATUS_TRANSITION, message);
    }
}
