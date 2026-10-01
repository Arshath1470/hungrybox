import { Link as RouterLink, NavLink, Outlet, useNavigate } from 'react-router-dom';
import AppBar from '@mui/material/AppBar';
import Badge from '@mui/material/Badge';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import IconButton from '@mui/material/IconButton';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import LogoutIcon from '@mui/icons-material/Logout';
import ShoppingBagOutlinedIcon from '@mui/icons-material/ShoppingBagOutlined';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { cartCount } from '../lib/cart.js';
import { colors } from '../theme.js';

const navLinkSx = {
  color: 'text.secondary',
  fontWeight: 600,
  textDecoration: 'none',
  whiteSpace: 'nowrap',
  px: { xs: 0.75, sm: 1 },
  py: 0.5,
  borderRadius: 1,
  '&.active': { color: 'primary.main' },
  '&:hover': { color: 'text.primary' },
  '&:focus-visible': { outline: `2px solid ${colors.turmeric}`, outlineOffset: 2 },
};

export default function Layout() {
  const { user, isAdmin, logout } = useAuth();
  const { cart } = useCart();
  const navigate = useNavigate();
  const count = cartCount(cart);
  const signOut = () => {
    logout();
    navigate('/');
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="sticky" sx={{ bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider' }}>
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ gap: { xs: 1, sm: 2 } }}>
            <Box
              component={RouterLink}
              to="/"
              sx={{ display: 'flex', alignItems: 'center', gap: 1, textDecoration: 'none', color: 'inherit', mr: 'auto' }}
            >
              <Box component="img" src="/favicon.svg" alt="" sx={{ width: 32, height: 32 }} />
              <Typography variant="h4" component="span" sx={{ color: 'primary.main', display: { xs: 'none', sm: 'inline' } }}>
                HungryBox
              </Typography>
            </Box>

            <Box component="nav" aria-label="Main" sx={{ display: 'flex', gap: { xs: 0, sm: 1 } }}>
              <Box component={NavLink} to="/" end sx={navLinkSx}>
                Eat
              </Box>
              {user && (
                <Box component={NavLink} to="/orders" sx={navLinkSx}>
                  Orders
                </Box>
              )}
              {isAdmin && (
                <Box component={NavLink} to="/admin" sx={navLinkSx}>
                  Kitchen desk
                </Box>
              )}
            </Box>

            <IconButton component={RouterLink} to="/checkout" aria-label={`Cart, ${count} items`} color="primary">
              <Badge badgeContent={count} color="secondary">
                <ShoppingBagOutlinedIcon />
              </Badge>
            </IconButton>

            {user ? (
              <>
                <Button variant="text" onClick={signOut} sx={{ display: { xs: 'none', sm: 'inline-flex' } }}>
                  Sign out
                </Button>
                <IconButton onClick={signOut} aria-label="Sign out" sx={{ display: { xs: 'inline-flex', sm: 'none' } }}>
                  <LogoutIcon />
                </IconButton>
              </>
            ) : (
              <Button variant="contained" component={RouterLink} to="/login">
                Sign in
              </Button>
            )}
          </Toolbar>
        </Container>
      </AppBar>

      <Box component="main" sx={{ flex: 1 }}>
        <Outlet />
      </Box>

      <Box component="footer" sx={{ borderTop: 1, borderColor: 'divider', py: 3 }}>
        <Container maxWidth="lg">
          <Typography variant="body2" color="text.secondary">
            HungryBox is a portfolio project. Payments are simulated and no real food is delivered.
          </Typography>
        </Container>
      </Box>
    </Box>
  );
}
