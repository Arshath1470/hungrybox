import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import OrderTracker from '../components/OrderTracker.jsx';
import VegMark from '../components/VegMark.jsx';

describe('VegMark', () => {
  it('labels veg and non-veg for screen readers', () => {
    render(
      <>
        <VegMark veg />
        <VegMark veg={false} />
      </>,
    );
    expect(screen.getByRole('img', { name: 'Vegetarian' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Non-vegetarian' })).toBeInTheDocument();
  });
});

describe('OrderTracker', () => {
  it('marks the current step', () => {
    render(<OrderTracker status="PREPARING" />);
    const current = screen.getByText('Being prepared').closest('li');
    expect(current).toHaveAttribute('aria-current', 'step');
    expect(screen.getAllByRole('listitem')).toHaveLength(5);
  });
});
