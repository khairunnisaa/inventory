-- Sample Items
INSERT INTO items (id, name, description, base_price, stock_quantity, has_variants, version) VALUES
(1, 'T-Shirt', 'Basic cotton t-shirt', 100000, 10, TRUE, 0),
(2, 'Coffee Beans', '250g medium roast coffee beans', 150000, 20, FALSE, 0),
(3, 'Laptop', '15-inch laptop computer', 12000000, 5, FALSE, 0);

-- Sample Item Variants for T-Shirt
INSERT INTO item_variants (id, item_id, sku, name, price, stock_quantity, version) VALUES
(1, 1, 'TSHIRT-BLACK-M', 'Black - M', 110000, 5, 0),
(2, 1, 'TSHIRT-BLACK-L', 'Black - L', 110000, 3, 0),
(3, 1, 'TSHIRT-WHITE-M', 'White - M', 105000, 2, 0),
(4, 1, 'TSHIRT-WHITE-L', 'White - L', 105000, 4, 0);

-- Reset IDENTITY sequences to prevent primary key conflicts
ALTER TABLE items ALTER COLUMN id RESTART WITH 4;
ALTER TABLE item_variants ALTER COLUMN id RESTART WITH 5;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 1;
ALTER TABLE order_lines ALTER COLUMN id RESTART WITH 1;

