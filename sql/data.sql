-- StadiumEats Seed Data
USE stadiumeats;

-- Seed users
-- Passwords are BCrypt hashes:
--   admin123   -> hash below
--   worker123  -> hash below
--   client123  -> hash below
INSERT INTO users (username, email, password_hash, role) VALUES
('admin',  'admin@stadiumeats.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
('worker1','worker1@stadiumeats.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'WORKER'),
('client1','client1@stadiumeats.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENT');

-- NOTE: The bcrypt hash above corresponds to the password "password123"
-- To use specific passwords (admin123, worker123, client123) you should run
-- the application and register, or generate new hashes.

-- Seed menu items
INSERT INTO menu_items (name, description, price, category, image_url, available) VALUES
('Classic Burger',
 'Juicy beef patty with lettuce, tomato, and our signature sauce',
 12.99, 'Burgers',
 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400',
 TRUE),

('Loaded Nachos',
 'Crispy tortilla chips with melted cheese, jalapeños, sour cream, and salsa',
 9.99, 'Snacks',
 'https://images.unsplash.com/photo-1513456852971-30c0b8199d4d?w=400',
 TRUE),

('Stadium Hot Dog',
 'All-beef frankfurter in a soft bun with mustard and ketchup',
 6.99, 'Hot Dogs',
 'https://images.unsplash.com/photo-1619740455993-9d16a2b3cf30?w=400',
 TRUE),

('Craft Beer (500ml)',
 'Ice-cold premium lager, perfectly refreshing for game day',
 7.50, 'Drinks',
 'https://images.unsplash.com/photo-1535958636474-b021ee887b13?w=400',
 TRUE),

('Pepperoni Pizza Slice',
 'Hand-stretched dough, tomato sauce, mozzarella and premium pepperoni',
 8.99, 'Pizza',
 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400',
 TRUE);
