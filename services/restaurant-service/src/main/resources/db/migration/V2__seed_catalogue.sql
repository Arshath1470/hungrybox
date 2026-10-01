-- Demo catalogue. Restaurant names are fictional.
INSERT INTO restaurants (name, cuisine, city, rating, delivery_time_mins, is_open) VALUES
    ('Temple City Tiffins',     'South Indian', 'Madurai',   4.6, 25, TRUE),
    ('Vaigai Biryani House',    'Biryani',      'Madurai',   4.4, 35, TRUE),
    ('Chettinad Spice Route',   'Chettinad',    'Madurai',   4.5, 40, TRUE),
    ('Midnight Parotta Point',  'Street Food',  'Madurai',   4.3, 20, TRUE),
    ('Green Bowl Kitchen',      'Healthy',      'Bengaluru', 4.2, 30, TRUE);

INSERT INTO menu_items (restaurant_id, name, description, category, price, is_veg, is_available) VALUES
    ((SELECT id FROM restaurants WHERE name = 'Temple City Tiffins'), 'Ghee Podi Idli', 'Mini idlis tossed in gunpowder and ghee', 'Tiffin', 90.00, TRUE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Temple City Tiffins'), 'Masala Dosa', 'Crisp dosa with potato masala, sambar and three chutneys', 'Tiffin', 110.00, TRUE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Temple City Tiffins'), 'Pongal Vada Combo', 'Ven pongal with a crisp medu vada', 'Tiffin', 120.00, TRUE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Temple City Tiffins'), 'Filter Coffee', 'Strong decoction, frothed the Madurai way', 'Beverages', 40.00, TRUE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Temple City Tiffins'), 'Jigarthanda', 'Milk, almond gum, sarsaparilla syrup and ice cream', 'Beverages', 80.00, TRUE, TRUE),

    ((SELECT id FROM restaurants WHERE name = 'Vaigai Biryani House'), 'Mutton Biryani', 'Seeraga samba rice, slow-cooked mutton', 'Biryani', 320.00, FALSE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Vaigai Biryani House'), 'Chicken Biryani', 'Dum-cooked with house masala', 'Biryani', 240.00, FALSE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Vaigai Biryani House'), 'Veg Biryani', 'Seasonal vegetables and fried onions', 'Biryani', 180.00, TRUE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Vaigai Biryani House'), 'Bread Halwa', 'Rich wedding-style dessert', 'Desserts', 70.00, TRUE, TRUE),

    ((SELECT id FROM restaurants WHERE name = 'Chettinad Spice Route'), 'Chettinad Chicken', 'Pepper-forward curry with roasted spices', 'Mains', 280.00, FALSE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Chettinad Spice Route'), 'Kothu Parotta', 'Shredded parotta with egg and salna', 'Mains', 160.00, FALSE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Chettinad Spice Route'), 'Mushroom Pepper Fry', 'Dry roast with curry leaves', 'Starters', 190.00, TRUE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Chettinad Spice Route'), 'Appam with Stew', 'Two appams with coconut vegetable stew', 'Mains', 150.00, TRUE, TRUE),

    ((SELECT id FROM restaurants WHERE name = 'Midnight Parotta Point'), 'Bun Parotta', 'Fluffy layered parotta with salna', 'Parotta', 60.00, TRUE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Midnight Parotta Point'), 'Kari Dosa', 'Egg dosa topped with minced mutton', 'Specials', 180.00, FALSE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Midnight Parotta Point'), 'Chicken 65', 'Crispy, spicy and fried to order', 'Starters', 170.00, FALSE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Midnight Parotta Point'), 'Rose Milk', 'Chilled and sweet', 'Beverages', 50.00, TRUE, TRUE),

    ((SELECT id FROM restaurants WHERE name = 'Green Bowl Kitchen'), 'Millet Buddha Bowl', 'Foxtail millet, roasted veggies, tahini', 'Bowls', 260.00, TRUE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Green Bowl Kitchen'), 'Grilled Paneer Salad', 'Greens, paneer, mint yogurt dressing', 'Salads', 240.00, TRUE, TRUE),
    ((SELECT id FROM restaurants WHERE name = 'Green Bowl Kitchen'), 'Cold Pressed Juice', 'Beet, carrot, ginger', 'Beverages', 120.00, TRUE, TRUE);
