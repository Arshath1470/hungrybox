import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { STATUS_LABELS, TRACKING_STEPS, stepIndex } from '../lib/orderStatus.js';

/**
 * Horizontal tracker for the happy path. Completed steps are filled leaf green,
 * the current step pulses turmeric while the kitchen works on it.
 */
export default function OrderTracker({ status }) {
  const current = stepIndex(status);
  return (
    <Box
      component="ol"
      aria-label="Order progress"
      sx={{
        listStyle: 'none',
        p: 0,
        m: 0,
        display: 'grid',
        gridTemplateColumns: `repeat(${TRACKING_STEPS.length}, 1fr)`,
        gap: { xs: 0.5, sm: 1 },
      }}
    >
      {TRACKING_STEPS.map((step, index) => {
        const done = index < current || status === 'DELIVERED';
        const active = index === current && status !== 'DELIVERED';
        return (
          <Box component="li" key={step} aria-current={active ? 'step' : undefined}>
            <Box
              sx={{
                height: 8,
                borderRadius: 999,
                bgcolor: done ? 'primary.main' : active ? 'secondary.main' : 'divider',
                '@keyframes hbPulse': { '0%,100%': { opacity: 1 }, '50%': { opacity: 0.45 } },
                animation: active ? 'hbPulse 1.6s ease-in-out infinite' : 'none',
                '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
              }}
            />
            <Typography
              variant="body2"
              sx={{
                mt: 0.75,
                fontWeight: active ? 700 : 500,
                color: done || active ? 'text.primary' : 'text.secondary',
                fontSize: { xs: '0.72rem', sm: '0.85rem' },
                lineHeight: 1.2,
              }}
            >
              {STATUS_LABELS[step]}
            </Typography>
          </Box>
        );
      })}
    </Box>
  );
}
