// Pure cart logic so it can be unit tested without React.
// A cart holds items from exactly one restaurant, like every Indian delivery app.

export const MAX_QUANTITY = 20;

// Mirrors PricingCalculator.java. The server recalculates on checkout; this is only an estimate.
const FREE_DELIVERY_THRESHOLD = 500;
const DELIVERY_FEE = 40;
const GST_RATE = 0.05;

export const emptyCart = { restaurantId: null, restaurantName: null, items: {} };

export function cartReducer(state, action) {
  switch (action.type) {
    case 'add': {
      const { item, restaurant } = action;
      const sameRestaurant = state.restaurantId === null || state.restaurantId === restaurant.id;
      const base = sameRestaurant ? state : emptyCart;
      const current = base.items[item.id]?.quantity ?? 0;
      if (current >= MAX_QUANTITY) return state;
      return {
        restaurantId: restaurant.id,
        restaurantName: restaurant.name,
        items: {
          ...base.items,
          [item.id]: {
            id: item.id,
            name: item.name,
            price: Number(item.price),
            veg: item.veg,
            quantity: current + 1,
          },
        },
      };
    }
    case 'remove': {
      const existing = state.items[action.itemId];
      if (!existing) return state;
      const items = { ...state.items };
      if (existing.quantity <= 1) {
        delete items[action.itemId];
      } else {
        items[action.itemId] = { ...existing, quantity: existing.quantity - 1 };
      }
      return Object.keys(items).length === 0 ? emptyCart : { ...state, items };
    }
    case 'clear':
      return emptyCart;
    case 'restore':
      return action.cart && action.cart.items ? action.cart : emptyCart;
    default:
      return state;
  }
}

export function cartLines(cart) {
  return Object.values(cart.items);
}

export function cartCount(cart) {
  return cartLines(cart).reduce((sum, line) => sum + line.quantity, 0);
}

export function quantityOf(cart, itemId) {
  return cart.items[itemId]?.quantity ?? 0;
}

export function wouldReplaceCart(cart, restaurantId) {
  return cart.restaurantId !== null && cart.restaurantId !== restaurantId;
}

const round2 = (n) => Math.round((n + Number.EPSILON) * 100) / 100;

export function estimateTotals(cart) {
  const subtotal = round2(cartLines(cart).reduce((sum, l) => sum + l.price * l.quantity, 0));
  if (subtotal === 0) return { subtotal: 0, deliveryFee: 0, tax: 0, total: 0 };
  const deliveryFee = subtotal >= FREE_DELIVERY_THRESHOLD ? 0 : DELIVERY_FEE;
  const tax = round2(subtotal * GST_RATE);
  return { subtotal, deliveryFee, tax, total: round2(subtotal + deliveryFee + tax) };
}

export function amountToFreeDelivery(cart) {
  const { subtotal } = estimateTotals(cart);
  return subtotal > 0 && subtotal < FREE_DELIVERY_THRESHOLD ? round2(FREE_DELIVERY_THRESHOLD - subtotal) : 0;
}
