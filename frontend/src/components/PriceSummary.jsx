import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import Typography from '@mui/material/Typography';
import { formatINR } from '../lib/format.js';

function Row({ label, value, strong }) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', py: 0.4 }}>
      <Typography variant={strong ? 'subtitle1' : 'body2'} sx={{ fontWeight: strong ? 700 : 400 }}>
        {label}
      </Typography>
      <Typography variant={strong ? 'subtitle1' : 'body2'} sx={{ fontWeight: strong ? 700 : 500 }}>
        {value}
      </Typography>
    </Box>
  );
}

export default function PriceSummary({ subtotal, deliveryFee, tax, total }) {
  return (
    <Box>
      <Row label="Item total" value={formatINR(subtotal)} />
      <Row label="Delivery" value={Number(deliveryFee) === 0 ? 'Free' : formatINR(deliveryFee)} />
      <Row label="GST (5%)" value={formatINR(tax)} />
      <Divider sx={{ my: 1 }} />
      <Row label="To pay" value={formatINR(total)} strong />
    </Box>
  );
}
