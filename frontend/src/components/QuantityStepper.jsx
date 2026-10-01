import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import { MAX_QUANTITY } from '../lib/cart.js';

export default function QuantityStepper({ quantity, onAdd, onRemove, label }) {
  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        border: 1,
        borderColor: 'primary.main',
        borderRadius: 999,
        bgcolor: 'background.paper',
      }}
    >
      <IconButton size="small" color="primary" onClick={onRemove} aria-label={`Remove one ${label}`}>
        <RemoveIcon fontSize="small" />
      </IconButton>
      <Typography component="span" sx={{ minWidth: 22, textAlign: 'center', fontWeight: 700 }} aria-live="polite">
        {quantity}
      </Typography>
      <IconButton
        size="small"
        color="primary"
        onClick={onAdd}
        disabled={quantity >= MAX_QUANTITY}
        aria-label={`Add one more ${label}`}
      >
        <AddIcon fontSize="small" />
      </IconButton>
    </Box>
  );
}
