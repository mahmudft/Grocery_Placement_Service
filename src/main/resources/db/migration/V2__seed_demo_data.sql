INSERT INTO product (sku, name, price_cents, stock) VALUES
    ('MILK-1L',     'Whole milk 1L',          109, 100),
    ('BREAD-WHOLE', 'Whole wheat bread',      189,  60),
    ('EGGS-10',     'Free-range eggs (10)',   329,  40),
    ('APPLE-1KG',   'Elstar apples 1kg',      249,  80),
    ('BANANA-1KG',  'Bananas 1kg',            159,  80),
    ('GOUDA-400G',  'Young Gouda cheese 400g',549,  30),
    ('PASTA-500G',  'Spaghetti 500g',          99, 120),
    ('COFFEE-500G', 'Ground coffee 500g',     699,   5);   -- scarce on purpose: easy to demo "out of stock"

-- Slots for the next 3 days in store-local time (Europe/Amsterdam): 08-10 ... 18-20, capacity 5 each.
INSERT INTO delivery_slot (starts_at, ends_at, capacity)
SELECT (date_trunc('day', now() AT TIME ZONE 'Europe/Amsterdam') + make_interval(days => d, hours => h))
           AT TIME ZONE 'Europe/Amsterdam',
       (date_trunc('day', now() AT TIME ZONE 'Europe/Amsterdam') + make_interval(days => d, hours => h + 2))
           AT TIME ZONE 'Europe/Amsterdam',
       5
FROM generate_series(1, 3) AS d,
     generate_series(8, 18, 2) AS h;
