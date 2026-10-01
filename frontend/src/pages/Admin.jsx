import { useCallback, useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Container from '@mui/material/Container';
import Link from '@mui/material/Link';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useAuth } from '../context/AuthContext.jsx';
import { formatDateTime, formatINR } from '../lib/format.js';
import { STATUS_COLORS, STATUS_LABELS, nextAdminStatus } from '../lib/orderStatus.js';
import { colors } from '../theme.js';

const REFRESH_MS = 5000;

function Figure({ label, value, tone }) {
  return (
    <Box sx={{ borderLeft: 3, borderColor: tone ?? 'primary.main', pl: 2 }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h3" component="p">
        {value}
      </Typography>
    </Box>
  );
}

function RevenueBars({ days }) {
  const max = Math.max(1, ...days.map((d) => Number(d.revenue)));
  if (days.length === 0) {
    return (
      <Typography color="text.secondary">No paid orders in the last 7 days yet.</Typography>
    );
  }
  return (
    <Box
      role="img"
      aria-label={`Paid revenue for the last ${days.length} days`}
      sx={{ display: 'flex', alignItems: 'flex-end', gap: 1.5, height: 160, pt: 2 }}
    >
      {days.map((d) => (
        <Box key={d.date} sx={{ flex: 1, minWidth: 0, textAlign: 'center' }}>
          <Typography variant="caption" sx={{ fontWeight: 700 }}>
            {formatINR(d.revenue)}
          </Typography>
          <Box
            sx={{
              mt: 0.5,
              height: `${Math.max(4, (Number(d.revenue) / max) * 110)}px`,
              bgcolor: colors.turmeric,
              borderRadius: '4px 4px 0 0',
            }}
          />
          <Typography variant="caption" color="text.secondary">
            {new Date(d.date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
          </Typography>
        </Box>
      ))}
    </Box>
  );
}

export default function Admin() {
  const { authed } = useAuth();
  const [summary, setSummary] = useState(null);
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState(null);
  const [busyOrder, setBusyOrder] = useState(null);

  const refresh = useCallback(async () => {
    try {
      const [s, o] = await Promise.all([authed('/api/billing/summary'), authed('/api/orders/all')]);
      setSummary(s);
      setOrders(o);
      setError(null);
    } catch (e) {
      setError(e.message);
    }
  }, [authed]);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, REFRESH_MS);
    return () => clearInterval(timer);
  }, [refresh]);

  const move = async (order, status) => {
    setBusyOrder(order.id);
    try {
      await authed(`/api/orders/${order.id}/status`, { method: 'PATCH', body: { status } });
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusyOrder(null);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: { xs: 4, md: 6 } }}>
      <Typography variant="h2" component="h1">
        Kitchen desk
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 4 }}>
        Live billing and order flow. Refreshes every few seconds.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {summary && (
        <Box component="section" aria-label="Billing summary" sx={{ mb: 6 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(4, 1fr)' }, gap: 3 }}>
            <Figure label="Net revenue" value={formatINR(summary.netRevenue)} />
            <Figure label="Paid orders" value={summary.paidCount} />
            <Figure label="Failed payments" value={summary.failedCount} tone="error.main" />
            <Figure label={`Refunded (${summary.refundedCount})`} value={formatINR(summary.refunded)} tone="secondary.main" />
          </Box>
          <Typography variant="h4" component="h2" sx={{ mt: 5 }}>
            Last 7 days
          </Typography>
          <RevenueBars days={summary.last7Days} />
        </Box>
      )}

      <Typography variant="h4" component="h2" sx={{ mb: 2 }}>
        Recent orders
      </Typography>
      <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 2, bgcolor: 'background.paper' }}>
        <Table size="small" aria-label="Recent orders">
          <TableHead>
            <TableRow>
              <TableCell>Order</TableCell>
              <TableCell>Placed</TableCell>
              <TableCell align="right">Total</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Next step</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {orders.length === 0 && (
              <TableRow>
                <TableCell colSpan={5}>
                  <Typography color="text.secondary" sx={{ py: 2 }}>
                    Orders appear here as soon as customers check out.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
            {orders.map((order) => {
              const next = nextAdminStatus(order.status);
              return (
                <TableRow key={order.id} hover>
                  <TableCell>
                    <Link component={RouterLink} to={`/orders/${order.id}`} sx={{ fontWeight: 600 }}>
                      #{order.id}
                    </Link>
                  </TableCell>
                  <TableCell>{formatDateTime(order.createdAt)}</TableCell>
                  <TableCell align="right">{formatINR(order.total)}</TableCell>
                  <TableCell>
                    <Chip size="small" label={STATUS_LABELS[order.status]} color={STATUS_COLORS[order.status]} />
                  </TableCell>
                  <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                    {order.status === 'CONFIRMED' && (
                      <Button
                        size="small"
                        color="error"
                        onClick={() => move(order, 'CANCELLED')}
                        disabled={busyOrder === order.id}
                        sx={{ mr: 1 }}
                      >
                        Cancel
                      </Button>
                    )}
                    {next && (
                      <Button size="small" variant="contained" onClick={() => move(order, next)} disabled={busyOrder === order.id}>
                        Mark {STATUS_LABELS[next].toLowerCase()}
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Container>
  );
}
