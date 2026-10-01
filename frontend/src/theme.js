import { createTheme } from '@mui/material/styles';

// Palette drawn from a South Indian meal: banana leaf, turmeric, chilli and steel tumblers.
export const colors = {
  leaf: '#1F5E3A',
  leafDeep: '#143F27',
  turmeric: '#E8A317',
  chilli: '#B3261E',
  steel: '#D9DFDA',
  canvas: '#F2F5F1',
  ink: '#17301F',
  muted: '#58695D',
  veg: '#1E8E3E',
  nonVeg: '#8B3A1A',
};

const display = '"Baloo Thambi 2", "Mukta", system-ui, sans-serif';

export const theme = createTheme({
  palette: {
    primary: { main: colors.leaf, dark: colors.leafDeep, contrastText: '#FFFFFF' },
    secondary: { main: colors.turmeric, contrastText: colors.ink },
    error: { main: colors.chilli },
    success: { main: colors.veg },
    background: { default: colors.canvas, paper: '#FFFFFF' },
    text: { primary: colors.ink, secondary: colors.muted },
    divider: colors.steel,
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: '"Mukta", system-ui, sans-serif',
    fontSize: 15,
    h1: { fontFamily: display, fontWeight: 800, fontSize: 'clamp(2.4rem, 6vw, 4rem)', lineHeight: 1.02, letterSpacing: '-0.01em' },
    h2: { fontFamily: display, fontWeight: 700, fontSize: 'clamp(1.8rem, 4vw, 2.5rem)', lineHeight: 1.1 },
    h3: { fontFamily: display, fontWeight: 700, fontSize: '1.5rem', lineHeight: 1.2 },
    h4: { fontFamily: display, fontWeight: 700, fontSize: '1.25rem', lineHeight: 1.25 },
    h5: { fontFamily: display, fontWeight: 700, fontSize: '1.1rem' },
    subtitle1: { fontSize: '1.05rem', lineHeight: 1.5 },
    body1: { lineHeight: 1.55 },
    button: { textTransform: 'none', fontWeight: 600, fontSize: '0.98rem' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { borderRadius: 999, paddingInline: 18 } },
    },
    MuiAppBar: { defaultProps: { elevation: 0, color: 'inherit' } },
    MuiPaper: { defaultProps: { elevation: 0 } },
    MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
    MuiTextField: { defaultProps: { fullWidth: true } },
  },
});
