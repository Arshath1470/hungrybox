import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Paper from '@mui/material/Paper';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import PriceSummary from '../components/PriceSummary.jsx';
import QuantityStepper from '../components/QuantityStepper.jsx';
import VegMark from '../components/VegMark.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { cartLines, estimateTotals } from '../lib/cart.js';
import { formatINR } from '../lib/format.js';

export default function Checkout() {
  const { cart, add, remove, clear } = useCart();
  const { user, authed } = useAuth();
  const navigate = useNavigate();
  const [address, setAddress] = useState(user?.address ?? '');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const lines = cartLines(cart);

  if (lines.length === 0) {
    return (
      <Container maxWidth="sm" sx={{ py: 8 }}>
        <Typography variant="h2" component="h1">
          Your cart is empty
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 1 }}>
          Pick a restaurant and add a few dishes to get started.
        </Typography>
        <Button variant="contained" component={RouterLink} to="/" sx={{ mt: 3 }}>
          Browse restaurants
        </Button>
      </Container>
    );
  }

  const restaurant = { id: cart.restaurantId, name: cart.restaurantName };
  const totals = estimateTotals(cart);

  const placeOrder = async (event) => {
    event.preventDefault();
    if (!address.trim()) {
      setError('Add a delivery address so the rider knows where to go.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const order = await authed('/api/orders', {
        method: 'POST',
        body: {
          restaurantId: cart.restaurantId,
          deliveryAddress: address.trim(),
          items: lines.map((l) => ({ menuItemId: l.id, quantity: l.quantity })),
        },
      });
      clear();
      navigate(`/orders/${order.id}`, { replace: true });
    } catch (e) {
      setError(e.message);
      setSubmitting(false);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 4, md: 6 } }}>
      <Typography variant="h2" component="h1" sx={{ mb: 1 }}>
        Checkout
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 4 }}>
        Ordering from{' '}
        <Box component={RouterLink} to={`/restaurants/${cart.restaurantId}`} sx={{ color: 'primary.main', fontWeight: 600 }}>
          {cart.restaurantName}
        </Box>
      </Typography>

      <Box
        component="form"
        onSubmit={placeOrder}
        sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 360px' }, gap: { xs: 4, md: 6 }, alignItems: 'start' }}
      >
        <Box>
          {lines.map((line) => (
            <Box
              key={line.id}
              sx={{ display: 'grid', gridTemplateColumns: '1fr auto auto', alignItems: 'center', gap: 2, py: 1.5, borderBottom: 1, borderColor: 'divider' }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <VegMark veg={line.veg} />
                <Typography sx={{ fontWeight: 600 }}>{line.name}</Typography>
              </Box>
              <QuantityStepper
                quantity={line.quantity}
                label={line.name}
                onAdd={() => add(line, restaurant)}
                onRemove={() => remove(line.id)}
              />
              <Typography sx={{ minWidth: 72, textAlign: 'right', fontWeight: 600 }}>
                {formatINR(line.price * line.quantity)}
              </Typography>
            </Box>
          ))}

          <TextField
            label="Delivery address"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            multiline
            minRows={2}
            sx={{ mt: 4 }}
            required
            slotProps={{ htmlInput: { maxLength: 255 } }}
          />
        </Box>

        <Paper sx={{ p: 3, border: 1, borderColor: 'divider' }}>
          <PriceSummary {...totals} />
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Prices are confirmed by the restaurant when you place the order.
          </Typography>
          {error && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {error}
            </Alert>
          )}
          <Button type="submit" variant="contained" size="large" fullWidth disabled={submitting} sx={{ mt: 2 }}>
            {submitting ? 'Placing order…' : `Place order and pay ${formatINR(totals.total)}`}
          </Button>
        </Paper>
      </Box>
    </Container>
  );
}
