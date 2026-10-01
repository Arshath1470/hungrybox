export const STATUS_LABELS = {
  PENDING_PAYMENT: 'Processing payment',
  CONFIRMED: 'Confirmed',
  PREPARING: 'Being prepared',
  OUT_FOR_DELIVERY: 'On the way',
  DELIVERED: 'Delivered',
  PAYMENT_FAILED: 'Payment failed',
  CANCELLED: 'Cancelled',
};

export const STATUS_COLORS = {
  PENDING_PAYMENT: 'default',
  CONFIRMED: 'primary',
  PREPARING: 'secondary',
  OUT_FOR_DELIVERY: 'secondary',
  DELIVERED: 'success',
  PAYMENT_FAILED: 'error',
  CANCELLED: 'default',
};

/** The happy path, in order. Failure states are shown separately. */
export const TRACKING_STEPS = ['PENDING_PAYMENT', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'];

const TERMINAL = new Set(['DELIVERED', 'PAYMENT_FAILED', 'CANCELLED']);

export function isTerminal(status) {
  return TERMINAL.has(status);
}

export function stepIndex(status) {
  return TRACKING_STEPS.indexOf(status);
}

/** The next kitchen/delivery step an admin can move an order to, or null. Mirrors OrderStatus.java. */
export function nextAdminStatus(status) {
  switch (status) {
    case 'CONFIRMED':
      return 'PREPARING';
    case 'PREPARING':
      return 'OUT_FOR_DELIVERY';
    case 'OUT_FOR_DELIVERY':
      return 'DELIVERED';
    default:
      return null;
  }
}

export function canCustomerCancel(status) {
  return status === 'CONFIRMED';
}
