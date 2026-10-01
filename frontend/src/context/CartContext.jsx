import { createContext, useContext, useEffect, useMemo, useReducer } from 'react';
import { cartReducer, emptyCart } from '../lib/cart.js';

const CART_KEY = 'hungrybox.cart';
const CartContext = createContext(null);

function loadCart() {
  try {
    const saved = JSON.parse(localStorage.getItem(CART_KEY));
    return cartReducer(emptyCart, { type: 'restore', cart: saved });
  } catch {
    return emptyCart;
  }
}

export function CartProvider({ children }) {
  const [cart, dispatch] = useReducer(cartReducer, undefined, loadCart);

  useEffect(() => {
    try {
      localStorage.setItem(CART_KEY, JSON.stringify(cart));
    } catch {
      // ignore: cart just won't survive a reload
    }
  }, [cart]);

  const value = useMemo(
    () => ({
      cart,
      add: (item, restaurant) => dispatch({ type: 'add', item, restaurant }),
      remove: (itemId) => dispatch({ type: 'remove', itemId }),
      clear: () => dispatch({ type: 'clear' }),
    }),
    [cart],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) throw new Error('useCart must be used inside CartProvider');
  return context;
}
