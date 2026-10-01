import { Navigate, useLocation } from 'react-router-dom';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import { useAuth } from '../context/AuthContext.jsx';

export default function RequireAuth({ children, admin = false }) {
  const { user, checking, isAdmin } = useAuth();
  const location = useLocation();

  if (checking) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 10 }}>
        <CircularProgress aria-label="Loading your account" />
      </Box>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (admin && !isAdmin) {
    return <Navigate to="/" replace />;
  }
  return children;
}
