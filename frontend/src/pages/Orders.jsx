import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Container from '@mui/material/Container';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import { useAuth } from '../context/AuthContext.jsx';
import { formatDateTime, formatINR } from '../lib/format.js';
import { STATUS_COLORS, STATUS_LABELS } from '../lib/orderStatus.js';
import { colors } from '../theme.js';

export default function Orders() {
  const { authed } = useAuth();
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    authed('/api/orders').then(setOrders).catch((e) => setError(e.message));
  }, [authed]);

  return (
    <Container maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
      <Typography variant="h2" component="h1" sx={{ mb: 3 }}>
        Your orders
      </Typography>

      {error && <Alert severity="error">{error}</Alert>}
      {!error && orders === null && [0, 1, 2].map((i) => <Skeleton key={i} height={72} />)}

      {orders && orders.length === 0 && (
        <Box>
          <Typography color="text.secondary">You have not ordered anything yet.</Typography>
          <Button variant="contained" component={RouterLink} to="/" sx={{ mt: 2 }}>
            Find something to eat
          </Button>
        </Box>
      )}

      {orders?.map((order) => (
        <Box
          key={order.id}
          component={RouterLink}
          to={`/orders/${order.id}`}
          sx={{
            display: 'grid',
            gridTemplateColumns: '1fr auto',
            gap: 1,
            py: 2,
            borderBottom: 1,
            borderColor: 'divider',
            textDecoration: 'none',
            color: 'inherit',
            '&:focus-visible': { outline: `2px solid ${colors.turmeric}`, outlineOffset: 2 },
          }}
        >
          <Box>
            <Typography variant="h4" component="h2">
              Order #{order.id}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {formatDateTime(order.createdAt)}, {order.items.reduce((n, i) => n + i.quantity, 0)} items,{' '}
              {formatINR(order.total)}
            </Typography>
          </Box>
          <Chip
            label={STATUS_LABELS[order.status]}
            color={STATUS_COLORS[order.status]}
            size="small"
            sx={{ alignSelf: 'center' }}
          />
        </Box>
      ))}
    </Container>
  );
}
