const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

export function formatINR(amount) {
  const value = typeof amount === 'string' ? Number(amount) : amount;
  if (value === null || value === undefined || Number.isNaN(value)) return '';
  return inr.format(value);
}

const dateTime = new Intl.DateTimeFormat('en-IN', {
  day: 'numeric',
  month: 'short',
  hour: 'numeric',
  minute: '2-digit',
});

export function formatDateTime(iso) {
  if (!iso) return '';
  return dateTime.format(new Date(iso));
}
