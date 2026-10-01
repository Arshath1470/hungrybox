import { useState } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useAuth } from '../context/AuthContext.jsx';

/** Sign-in and registration share one screen; `mode` decides which form is shown. */
export default function AuthPage({ mode }) {
  const isRegister = mode === 'register';
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phone: '' });
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      if (isRegister) {
        await register({
          fullName: form.fullName,
          email: form.email,
          password: form.password,
          phone: form.phone || null,
        });
      } else {
        await login(form.email, form.password);
      }
      navigate(location.state?.from ?? '/', { replace: true });
    } catch (e) {
      setError(e.message);
      setFieldErrors(e.fieldErrors ?? {});
      setSubmitting(false);
    }
  };

  return (
    <Container maxWidth="xs" sx={{ py: { xs: 6, md: 10 } }}>
      <Typography variant="h2" component="h1">
        {isRegister ? 'Create your account' : 'Welcome back'}
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 1, mb: 4 }}>
        {isRegister ? 'Order from kitchens across Madurai and beyond.' : 'Sign in to order and track deliveries.'}
      </Typography>

      <Box component="form" onSubmit={submit} noValidate>
        <Stack spacing={2}>
          {isRegister && (
            <TextField
              label="Full name"
              value={form.fullName}
              onChange={set('fullName')}
              autoComplete="name"
              required
              error={Boolean(fieldErrors.fullName)}
              helperText={fieldErrors.fullName}
            />
          )}
          <TextField
            label="Email"
            type="email"
            value={form.email}
            onChange={set('email')}
            autoComplete="email"
            required
            error={Boolean(fieldErrors.email)}
            helperText={fieldErrors.email}
          />
          <TextField
            label="Password"
            type="password"
            value={form.password}
            onChange={set('password')}
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            required
            error={Boolean(fieldErrors.password)}
            helperText={fieldErrors.password ?? (isRegister ? 'At least 8 characters' : undefined)}
          />
          {isRegister && (
            <TextField
              label="Phone (optional)"
              value={form.phone}
              onChange={set('phone')}
              autoComplete="tel"
              error={Boolean(fieldErrors.phone)}
              helperText={fieldErrors.phone}
            />
          )}
          {error && Object.keys(fieldErrors).length === 0 && <Alert severity="error">{error}</Alert>}
          <Button type="submit" variant="contained" size="large" disabled={submitting}>
            {submitting ? 'Just a moment…' : isRegister ? 'Create account' : 'Sign in'}
          </Button>
        </Stack>
      </Box>

      <Typography sx={{ mt: 3 }}>
        {isRegister ? 'Already have an account? ' : 'New to HungryBox? '}
        <Link component={RouterLink} to={isRegister ? '/login' : '/register'} state={location.state}>
          {isRegister ? 'Sign in' : 'Create an account'}
        </Link>
      </Typography>
    </Container>
  );
}
