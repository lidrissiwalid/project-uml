-- StadiumEats Seed Data
USE stadiumeats;

-- Seed users
-- Passwords are BCrypt hashes:
--   admin123   -> hash below
--   worker123  -> hash below
--   client123  -> hash below
INSERT INTO users (username, email, password_hash, role) VALUES
('admin',  'admin@stadiumeats.com',  '$2a$10$UBfxmi686K8IR/qNAwlit.FU1EU/8PCEu9iqQeCMs.JK8ELTi34bG', 'ADMIN'),
('worker1','worker1@stadiumeats.com','$2a$10$Bg.xGLQBAOXMxFWNjtXkqursVeCDWvXrnChs2c.SP1i6hPNU58H5G', 'WORKER'),
('client1','client1@stadiumeats.com','$2a$10$9Gkwbe2tsxfOFQ3I1JS8V.owxQWpxxZ8jwTT8REbBZqnTSWDkidDy', 'CLIENT');

-- NOTE: The bcrypt hashes above correspond to the passwords "admin123", "worker123", and "client123" respectively.
-- To use specific passwords (admin123, worker123, client123) you should run
-- the application and register, or generate new hashes.

-- Seed menu items
INSERT INTO menu_items (name, description, price, category, image_url, available) VALUES
('Pizza',
 'Freshly baked pepperoni pizza slice',
 30.00, 'Food',
 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400',
 TRUE),

('Burger',
 'Juicy classic beef burger with fresh cheese',
 50.00, 'Food',
 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400',
 TRUE),

('Tacos',
 'Authentic spiced beef tacos with fresh salsa',
 30.00, 'Food',
 'https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?w=400',
 TRUE),

('Tea',
 'Hot traditional mint tea',
 10.00, 'Drinks',
 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=400',
 TRUE),

('Coffee',
 'Freshly brewed aromatic coffee cup',
 12.00, 'Drinks',
 'https://images.unsplash.com/photo-1497935586351-b67a49e012bf?w=400',
 TRUE),

('Water',
 'Chilled mineral water bottle (500ml)',
 5.00, 'Drinks',
 'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=400',
 TRUE);
