import { describe, expect, it } from 'vitest';
import {
  MAX_QUANTITY,
  amountToFreeDelivery,
  cartCount,
  cartReducer,
  emptyCart,
  estimateTotals,
  wouldReplaceCart,
} from '../lib/cart.js';

const tiffins = { id: 1, name: 'Temple City Tiffins' };
const biryani = { id: 2, name: 'Vaigai Biryani House' };
const idli = { id: 10, name: 'Ghee Podi Idli', price: 90, veg: true };
const dosa = { id: 11, name: 'Masala Dosa', price: '110.00', veg: true };
const mutton = { id: 20, name: 'Mutton Biryani', price: 320, veg: false };

const add = (state, item, restaurant) => cartReducer(state, { type: 'add', item, restaurant });

describe('cartReducer', () => {
  it('adds items and increments quantity', () => {
    let cart = add(emptyCart, idli, tiffins);
    cart = add(cart, idli, tiffins);
    cart = add(cart, dosa, tiffins);
    expect(cartCount(cart)).toBe(3);
    expect(cart.items[10].quantity).toBe(2);
    expect(cart.items[11].price).toBe(110);
  });

  it('starts a fresh cart when items come from another restaurant', () => {
    const cart = add(add(emptyCart, idli, tiffins), mutton, biryani);
    expect(cart.restaurantId).toBe(2);
    expect(Object.keys(cart.items)).toEqual(['20']);
    expect(wouldReplaceCart(cart, 1)).toBe(true);
    expect(wouldReplaceCart(cart, 2)).toBe(false);
  });

  it('removes items and empties back to the initial cart', () => {
    let cart = add(add(emptyCart, idli, tiffins), idli, tiffins);
    cart = cartReducer(cart, { type: 'remove', itemId: 10 });
    expect(cart.items[10].quantity).toBe(1);
    cart = cartReducer(cart, { type: 'remove', itemId: 10 });
    expect(cart).toEqual(emptyCart);
  });

  it('caps quantity per item', () => {
    let cart = emptyCart;
    for (let i = 0; i < MAX_QUANTITY + 5; i += 1) cart = add(cart, idli, tiffins);
    expect(cart.items[10].quantity).toBe(MAX_QUANTITY);
  });

  it('ignores a corrupt saved cart', () => {
    expect(cartReducer(emptyCart, { type: 'restore', cart: null })).toEqual(emptyCart);
    expect(cartReducer(emptyCart, { type: 'restore', cart: { nonsense: true } })).toEqual(emptyCart);
  });
});

describe('estimateTotals (mirrors PricingCalculator.java)', () => {
  it('charges delivery and GST on small orders', () => {
    const cart = add(add(add(emptyCart, idli, tiffins), idli, tiffins), dosa, tiffins);
    expect(estimateTotals(cart)).toEqual({ subtotal: 290, deliveryFee: 40, tax: 14.5, total: 344.5 });
    expect(amountToFreeDelivery(cart)).toBe(210);
  });

  it('makes delivery free from 500', () => {
    const cart = add(add(emptyCart, mutton, biryani), { ...mutton, id: 21, price: 180 }, biryani);
    expect(estimateTotals(cart)).toEqual({ subtotal: 500, deliveryFee: 0, tax: 25, total: 525 });
    expect(amountToFreeDelivery(cart)).toBe(0);
  });

  it('is all zero for an empty cart', () => {
    expect(estimateTotals(emptyCart).total).toBe(0);
  });
});
