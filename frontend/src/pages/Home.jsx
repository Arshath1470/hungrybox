import { useEffect, useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Container from '@mui/material/Container';
import InputAdornment from '@mui/material/InputAdornment';
import Skeleton from '@mui/material/Skeleton';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import SearchIcon from '@mui/icons-material/Search';
import StarRoundedIcon from '@mui/icons-material/StarRounded';
import { api } from '../api/client.js';
import { mealMoment } from '../lib/mealMoment.js';
import { colors } from '../theme.js';

function useDebounced(value, delay = 250) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(id);
  }, [value, delay]);
  return debounced;
}

function RestaurantRow({ restaurant }) {
  return (
    <Box
      component={RouterLink}
      to={`/restaurants/${restaurant.id}`}
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr auto', sm: '1fr 120px 90px' },
        alignItems: 'center',
        gap: 2,
        py: 2.25,
        px: { xs: 0, sm: 1 },
        borderBottom: 1,
        borderColor: 'divider',
        textDecoration: 'none',
        color: 'inherit',
        opacity: restaurant.open ? 1 : 0.5,
        '&:hover .hb-name, &:focus-visible .hb-name': { color: 'primary.main' },
        '&:focus-visible': { outline: `2px solid ${colors.turmeric}`, outlineOffset: 2, borderRadius: 1 },
      }}
    >
      <Box>
        <Typography className="hb-name" variant="h3" component="h3" sx={{ transition: 'color .15s' }}>
          {restaurant.name}
        </Typography>
        <Typography color="text.secondary">
          {restaurant.cuisine} in {restaurant.city}
          {!restaurant.open && ', closed right now'}
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, justifySelf: { xs: 'end', sm: 'start' } }}>
        <StarRoundedIcon sx={{ color: colors.turmeric }} fontSize="small" />
        <Typography sx={{ fontWeight: 700 }}>{Number(restaurant.rating).toFixed(1)}</Typography>
      </Box>
      <Typography
        color="text.secondary"
        sx={{ display: { xs: 'none', sm: 'block' }, textAlign: 'right', fontWeight: 600 }}
      >
        {restaurant.deliveryTimeMins} min
      </Typography>
    </Box>
  );
}

export default function Home() {
  const moment = useMemo(() => mealMoment(), []);
  const [query, setQuery] = useState('');
  const [cuisine, setCuisine] = useState(null);
  const [cuisines, setCuisines] = useState([]);
  const [restaurants, setRestaurants] = useState(null);
  const [error, setError] = useState(null);
  const debouncedQuery = useDebounced(query);

  useEffect(() => {
    api('/api/restaurants/cuisines').then(setCuisines).catch(() => setCuisines([]));
  }, []);

  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedQuery.trim()) params.set('q', debouncedQuery.trim());
    if (cuisine) params.set('cuisine', cuisine);
    let cancelled = false;
    setError(null);
    api(`/api/restaurants?${params}`)
      .then((list) => !cancelled && setRestaurants(list))
      .catch((e) => !cancelled && setError(e.message));
    return () => {
      cancelled = true;
    };
  }, [debouncedQuery, cuisine]);

  return (
    <>
      <Box sx={{ bgcolor: 'primary.main', color: '#fff', position: 'relative', overflow: 'hidden' }}>
        {/* A banana-leaf vein runs across the hero */}
        <Box
          aria-hidden
          sx={{
            position: 'absolute',
            inset: 0,
            backgroundImage: `repeating-linear-gradient(-28deg, transparent 0 46px, rgba(255,255,255,0.05) 46px 48px)`,
          }}
        />
        <Container maxWidth="lg" sx={{ position: 'relative', py: { xs: 6, md: 9 } }}>
          <Typography variant="h1" component="h1" sx={{ maxWidth: 760 }}>
            {moment.headline}
          </Typography>
          <Typography variant="subtitle1" sx={{ mt: 2, maxWidth: 560, color: 'rgba(255,255,255,0.85)', fontSize: '1.2rem' }}>
            {moment.sub}
          </Typography>
          <TextField
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search a restaurant or cuisine"
            aria-label="Search restaurants"
            sx={{
              mt: 4,
              maxWidth: 520,
              '& .MuiOutlinedInput-root': { bgcolor: '#fff', borderRadius: 999 },
            }}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              },
            }}
          />
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 4, md: 6 } }}>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 3 }} role="group" aria-label="Filter by cuisine">
          <Chip label="Everything" color={cuisine ? 'default' : 'primary'} onClick={() => setCuisine(null)} />
          {cuisines.map((c) => (
            <Chip
              key={c}
              label={c}
              color={cuisine === c ? 'primary' : 'default'}
              variant={cuisine === c ? 'filled' : 'outlined'}
              onClick={() => setCuisine(cuisine === c ? null : c)}
            />
          ))}
        </Box>

        {error && <Alert severity="error">{error}</Alert>}

        {!error && restaurants === null && (
          <Box>
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} height={72} />
            ))}
          </Box>
        )}

        {restaurants && restaurants.length === 0 && (
          <Typography color="text.secondary" sx={{ py: 4 }}>
            Nothing matches that search. Try a dish like dosa or biryani, or clear the cuisine filter.
          </Typography>
        )}

        {restaurants && restaurants.length > 0 && (
          <Box component="section" aria-label="Restaurants">
            {restaurants.map((r) => (
              <RestaurantRow key={r.id} restaurant={r} />
            ))}
          </Box>
        )}
      </Container>
    </>
  );
}
