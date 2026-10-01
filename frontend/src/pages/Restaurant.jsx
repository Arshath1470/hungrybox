import { useEffect, useMemo, useState } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import StarRoundedIcon from '@mui/icons-material/StarRounded';
import { api } from '../api/client.js';
import QuantityStepper from '../components/QuantityStepper.jsx';
import VegMark from '../components/VegMark.jsx';
import { useCart } from '../context/CartContext.jsx';
import { amountToFreeDelivery, cartCount, cartLines, estimateTotals, quantityOf, wouldReplaceCart } from '../lib/cart.js';
import { formatINR } from '../lib/format.js';
import { colors } from '../theme.js';

function groupByCategory(menu) {
  const groups = new Map();
  for (const item of menu) {
    if (!groups.has(item.category)) groups.set(item.category, []);
    groups.get(item.category).push(item);
  }
  return [...groups.entries()];
}

function MenuItemRow({ item, restaurant, quantity, onAdd, onRemove }) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: '1fr auto',
        gap: 2,
        py: 2,
        borderBottom: 1,
        borderColor: 'divider',
        opacity: item.available ? 1 : 0.5,
      }}
    >
      <Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <VegMark veg={item.veg} />
          <Typography variant="h5" component="h4">
            {item.name}
          </Typography>
        </Box>
        <Typography sx={{ fontWeight: 700, mt: 0.5 }}>{formatINR(item.price)}</Typography>
        {item.description && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, maxWidth: 520 }}>
            {item.description}
          </Typography>
        )}
      </Box>
      <Box sx={{ alignSelf: 'center' }}>
        {!item.available || !restaurant.open ? (
          <Typography variant="body2" color="text.secondary">
            {item.available ? 'Closed' : 'Sold out'}
          </Typography>
        ) : quantity > 0 ? (
          <QuantityStepper quantity={quantity} label={item.name} onAdd={onAdd} onRemove={onRemove} />
        ) : (
          <Button variant="outlined" onClick={onAdd} aria-label={`Add ${item.name}`}>
            Add
          </Button>
        )}
      </Box>
    </Box>
  );
}

function CartPanel({ cart }) {
  const lines = cartLines(cart);
  const totals = estimateTotals(cart);
  const toFree = amountToFreeDelivery(cart);
  return (
    <Paper sx={{ p: 3, border: 1, borderColor: 'divider', position: { md: 'sticky' }, top: { md: 88 } }}>
      <Typography variant="h4" component="h2">
        Your order
      </Typography>
      {lines.length === 0 ? (
        <Typography color="text.secondary" sx={{ mt: 1 }}>
          Add a dish to start your order.
        </Typography>
      ) : (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            From {cart.restaurantName}
          </Typography>
          {lines.map((line) => (
            <Box key={line.id} sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, py: 0.5 }}>
              <Typography variant="body2">
                {line.quantity} × {line.name}
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {formatINR(line.price * line.quantity)}
              </Typography>
            </Box>
          ))}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 2, pt: 2, borderTop: 1, borderColor: 'divider' }}>
            <Typography sx={{ fontWeight: 700 }}>Estimated total</Typography>
            <Typography sx={{ fontWeight: 700 }}>{formatINR(totals.total)}</Typography>
          </Box>
          {toFree > 0 && (
            <Typography variant="body2" sx={{ mt: 1, color: colors.leaf }}>
              Add {formatINR(toFree)} more for free delivery.
            </Typography>
          )}
          <Button variant="contained" fullWidth size="large" component={RouterLink} to="/checkout" sx={{ mt: 2 }}>
            Go to checkout
          </Button>
        </>
      )}
    </Paper>
  );
}

export default function Restaurant() {
  const { id } = useParams();
  const restaurantId = Number(id);
  const { cart, add, remove, clear } = useCart();
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState(null);
  const [pendingItem, setPendingItem] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    api(`/api/restaurants/${restaurantId}`)
      .then((d) => !cancelled && setDetail(d))
      .catch((e) => !cancelled && setError(e.status === 404 ? 'This restaurant does not exist.' : e.message));
    return () => {
      cancelled = true;
    };
  }, [restaurantId]);

  const groups = useMemo(() => (detail ? groupByCategory(detail.menu) : []), [detail]);
  const count = cartCount(cart);

  if (error) {
    return (
      <Container maxWidth="lg" sx={{ py: 6 }}>
        <Alert severity="error">{error}</Alert>
        <Button component={RouterLink} to="/" sx={{ mt: 2 }}>
          See all restaurants
        </Button>
      </Container>
    );
  }

  if (!detail) {
    return (
      <Container maxWidth="lg" sx={{ py: 6 }}>
        <Skeleton width="50%" height={64} />
        <Skeleton width="30%" />
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} height={90} />
        ))}
      </Container>
    );
  }

  const { restaurant } = detail;
  const handleAdd = (item) => {
    if (wouldReplaceCart(cart, restaurant.id)) {
      setPendingItem(item);
    } else {
      add(item, restaurant);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 4, md: 6 }, pb: { xs: count > 0 ? 12 : 4, md: 6 } }}>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h2" component="h1">
          {restaurant.name}
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mt: 1, color: 'text.secondary' }}>
          <Typography>
            {restaurant.cuisine} in {restaurant.city}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}>
            <StarRoundedIcon sx={{ color: colors.turmeric }} fontSize="small" />
            <Typography sx={{ fontWeight: 700, color: 'text.primary' }}>{Number(restaurant.rating).toFixed(1)}</Typography>
          </Box>
          <Typography>Delivers in about {restaurant.deliveryTimeMins} minutes</Typography>
        </Box>
        {!restaurant.open && (
          <Alert severity="info" sx={{ mt: 2 }}>
            This kitchen is closed right now. You can browse the menu, but ordering is paused.
          </Alert>
        )}
      </Box>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 340px' }, gap: { xs: 4, md: 6 }, alignItems: 'start' }}>
        <Box>
          {groups.map(([category, items]) => (
            <Box component="section" key={category} sx={{ mb: 4 }} aria-labelledby={`cat-${category}`}>
              <Typography id={`cat-${category}`} variant="h3" component="h2" sx={{ mb: 1 }}>
                {category}
              </Typography>
              {items.map((item) => (
                <MenuItemRow
                  key={item.id}
                  item={item}
                  restaurant={restaurant}
                  quantity={cart.restaurantId === restaurant.id ? quantityOf(cart, item.id) : 0}
                  onAdd={() => handleAdd(item)}
                  onRemove={() => remove(item.id)}
                />
              ))}
            </Box>
          ))}
        </Box>
        <Box sx={{ display: { xs: 'none', md: 'block' } }}>
          <CartPanel cart={cart} />
        </Box>
      </Box>

      {count > 0 && (
        <Box
          sx={{
            display: { xs: 'block', md: 'none' },
            position: 'fixed',
            left: 16,
            right: 16,
            bottom: 16,
            zIndex: 10,
          }}
        >
          <Button variant="contained" size="large" fullWidth component={RouterLink} to="/checkout" sx={{ py: 1.5 }}>
            View cart ({count} {count === 1 ? 'item' : 'items'}, {formatINR(estimateTotals(cart).total)})
          </Button>
        </Box>
      )}

      <Dialog open={Boolean(pendingItem)} onClose={() => setPendingItem(null)}>
        <DialogTitle>Start a new cart?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Your cart has dishes from {cart.restaurantName}. Adding {pendingItem?.name} will remove them.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPendingItem(null)}>Keep current cart</Button>
          <Button
            variant="contained"
            onClick={() => {
              clear();
              add(pendingItem, restaurant);
              setPendingItem(null);
            }}
          >
            Start new cart
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
