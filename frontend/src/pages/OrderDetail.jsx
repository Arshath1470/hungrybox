import { useCallback, useEffect, useState } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Container from '@mui/material/Container';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import OrderTracker from '../components/OrderTracker.jsx';
import PriceSummary from '../components/PriceSummary.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { formatDateTime, formatINR } from '../lib/format.js';
import { STATUS_COLORS, STATUS_LABELS, canCustomerCancel, isTerminal } from '../lib/orderStatus.js';

function pollInterval(status) {
  if (status === 'PENDING_PAYMENT') return 1500; // payment usually settles within a second or two
  return isTerminal(status) ? null : 5000;
}

export default function OrderDetail() {
  const { id } = useParams();
  const { authed } = useAuth();
  const [order, setOrder] = useState(null);
  const [invoice, setInvoice] = useState(null);
  const [error, setError] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(async () => {
    try {
      const latest = await authed(`/api/orders/${id}`);
      setOrder(latest);
      if (latest.status !== 'PENDING_PAYMENT') {
        authed(`/api/billing/invoices/order/${id}`).then(setInvoice).catch(() => setInvoice(null));
      }
    } catch (e) {
      setError(e.status === 404 ? 'We could not find that order.' : e.message);
    }
  }, [authed, id]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    const delay = order ? pollInterval(order.status) : null;
    if (!delay) return undefined;
    const timer = setTimeout(load, delay);
    return () => clearTimeout(timer);
  }, [order, load]);

  const cancel = async () => {
    setCancelling(true);
    try {
      setOrder(await authed(`/api/orders/${id}/cancel`, { method: 'POST' }));
    } catch (e) {
      setError(e.message);
    } finally {
      setCancelling(false);
    }
  };

  if (error && !order) {
    return (
      <Container maxWidth="md" sx={{ py: 6 }}>
        <Alert severity="error">{error}</Alert>
        <Button component={RouterLink} to="/orders" sx={{ mt: 2 }}>
          See all orders
        </Button>
      </Container>
    );
  }

  if (!order) {
    return (
      <Container maxWidth="md" sx={{ py: 6 }}>
        <Skeleton height={60} width="40%" />
        <Skeleton height={40} />
        <Skeleton height={200} />
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
      <Box sx={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h2" component="h1">
          Order #{order.id}
        </Typography>
        <Chip label={STATUS_LABELS[order.status]} color={STATUS_COLORS[order.status]} />
      </Box>
      <Typography color="text.secondary" sx={{ mt: 0.5 }}>
        Placed {formatDateTime(order.createdAt)}, delivering to {order.deliveryAddress}
      </Typography>

      <Box sx={{ my: 4 }}>
        {order.status === 'PAYMENT_FAILED' && (
          <Alert severity="error">
            Payment did not go through{order.statusReason ? `: ${order.statusReason}` : ''}. Nothing was charged.
          </Alert>
        )}
        {order.status === 'CANCELLED' && (
          <Alert severity="info">
            This order was cancelled.{invoice?.status === 'REFUNDED' ? ` ${formatINR(invoice.total)} has been refunded.` : ' A refund is being processed.'}
          </Alert>
        )}
        {order.status !== 'PAYMENT_FAILED' && order.status !== 'CANCELLED' && <OrderTracker status={order.status} />}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Paper sx={{ p: 3, border: 1, borderColor: 'divider' }}>
        {order.items.map((item) => (
          <Box key={item.menuItemId} sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5 }}>
            <Typography>
              {item.quantity} × {item.name}
            </Typography>
            <Typography sx={{ fontWeight: 600 }}>{formatINR(item.lineTotal)}</Typography>
          </Box>
        ))}
        <Box sx={{ mt: 2, pt: 2, borderTop: 1, borderColor: 'divider' }}>
          <PriceSummary subtotal={order.subtotal} deliveryFee={order.deliveryFee} tax={order.tax} total={order.total} />
        </Box>
        {invoice && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
            Invoice {invoice.invoiceNumber}, payment reference {invoice.paymentReference}
            {invoice.refundReference ? `, refund reference ${invoice.refundReference}` : ''}
          </Typography>
        )}
      </Paper>

      {canCustomerCancel(order.status) && (
        <Box sx={{ mt: 3 }}>
          <Button color="error" variant="outlined" onClick={cancel} disabled={cancelling}>
            {cancelling ? 'Cancelling…' : 'Cancel order and refund'}
          </Button>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            You can cancel until the kitchen starts cooking.
          </Typography>
        </Box>
      )}
    </Container>
  );
}
