import Box from '@mui/material/Box';
import { colors } from '../theme.js';

/**
 * The FSSAI food mark printed on Indian menus and packaging:
 * a green square with a dot for vegetarian, a brown square with a triangle for non-vegetarian.
 */
export default function VegMark({ veg, size = 14 }) {
  const color = veg ? colors.veg : colors.nonVeg;
  const inner = Math.round(size * 0.45);
  return (
    <Box
      role="img"
      aria-label={veg ? 'Vegetarian' : 'Non-vegetarian'}
      title={veg ? 'Vegetarian' : 'Non-vegetarian'}
      sx={{
        width: size,
        height: size,
        flex: 'none',
        border: `1.5px solid ${color}`,
        borderRadius: '2px',
        display: 'grid',
        placeItems: 'center',
        bgcolor: '#fff',
      }}
    >
      {veg ? (
        <Box sx={{ width: inner, height: inner, borderRadius: '50%', bgcolor: color }} />
      ) : (
        <Box
          sx={{
            width: 0,
            height: 0,
            borderLeft: `${inner / 2 + 0.5}px solid transparent`,
            borderRight: `${inner / 2 + 0.5}px solid transparent`,
            borderBottom: `${inner}px solid ${color}`,
          }}
        />
      )}
    </Box>
  );
}
