import { Link as RouterLink } from 'react-router-dom';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';

export default function NotFound() {
  return (
    <Container maxWidth="sm" sx={{ py: 10 }}>
      <Typography variant="h2" component="h1">
        This page is off the menu
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 1 }}>
        The link may be old or mistyped.
      </Typography>
      <Button variant="contained" component={RouterLink} to="/" sx={{ mt: 3 }}>
        Back to restaurants
      </Button>
    </Container>
  );
}
