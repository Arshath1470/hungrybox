import { describe, expect, it } from 'vitest';
import { formatINR } from '../lib/format.js';
import { mealMoment } from '../lib/mealMoment.js';
import { canCustomerCancel, isTerminal, nextAdminStatus, stepIndex } from '../lib/orderStatus.js';

const at = (hour) => new Date(2026, 0, 15, hour, 30);

describe('mealMoment', () => {
  it.each([
    [7, 'tiffin'],
    [13, 'meals'],
    [17, 'snacks'],
    [23, 'late-night'],
    [2, 'late-night'],
  ])('at %i:30 it is %s', (hour, key) => {
    expect(mealMoment(at(hour)).key).toBe(key);
  });
});

describe('order status helpers (mirror OrderStatus.java)', () => {
  it('walks the kitchen steps in order', () => {
    expect(nextAdminStatus('CONFIRMED')).toBe('PREPARING');
    expect(nextAdminStatus('PREPARING')).toBe('OUT_FOR_DELIVERY');
    expect(nextAdminStatus('OUT_FOR_DELIVERY')).toBe('DELIVERED');
    expect(nextAdminStatus('PENDING_PAYMENT')).toBeNull();
    expect(nextAdminStatus('DELIVERED')).toBeNull();
  });

  it('only lets customers cancel confirmed orders', () => {
    expect(canCustomerCancel('CONFIRMED')).toBe(true);
    expect(canCustomerCancel('PREPARING')).toBe(false);
  });

  it('knows terminal states and tracker positions', () => {
    expect(isTerminal('CANCELLED')).toBe(true);
    expect(isTerminal('PREPARING')).toBe(false);
    expect(stepIndex('OUT_FOR_DELIVERY')).toBe(3);
  });
});

describe('formatINR', () => {
  it('uses Indian digit grouping', () => {
    expect(formatINR(123456.5)).toBe('₹1,23,456.5');
    expect(formatINR('40.00')).toBe('₹40');
  });
});
