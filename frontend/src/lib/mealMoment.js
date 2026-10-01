// The home page headline follows the day the way a Madurai street does:
// tiffin in the morning, meals at noon, tea and snacks at dusk, parotta late at night.
const moments = [
  {
    key: 'tiffin',
    from: 5,
    to: 11,
    headline: 'Idli, dosa and a strong filter coffee.',
    sub: 'Morning tiffin from the messes that open before sunrise.',
  },
  {
    key: 'meals',
    from: 11,
    to: 16,
    headline: 'Lunch is on the leaf.',
    sub: 'Biryani, Chettinad curries and full meals, brought to your door.',
  },
  {
    key: 'snacks',
    from: 16,
    to: 20,
    headline: 'Tea time needs something crisp.',
    sub: 'Vadas, bajjis and a cold jigarthanda for the walk home.',
  },
];

const lateNight = {
  key: 'late-night',
  headline: 'Late-night parotta, delivered.',
  sub: 'The city never sleeps, and neither do these kitchens.',
};

export function mealMoment(date = new Date()) {
  const hour = date.getHours();
  return moments.find((m) => hour >= m.from && hour < m.to) ?? lateNight;
}
