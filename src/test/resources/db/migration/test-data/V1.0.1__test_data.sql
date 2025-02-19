
INSERT INTO auction_item (id, external_id, description, category, purchase_date, purchase_price, bidding_end_date, starting_price) VALUES
       ('76bce495-219d-4632-a0bb-3e2977b7ae83','b6579e11-d0ef-4a21-a597-58961ddb801c','Apple 96 W USB-C-virtalähde (MX0J2)', 'Computer accessories', '2023-10-06', '84.99',  NOW() + interval '3' month, '7.00'),
       ('b030b21b-73f9-40ff-8518-4a45f2c9b769','7f96e48a-97e0-4bd9-ac79-a565051fdfb6','Apple iPhone 15 Pro Max 512 Gt -puhelin, sinititaani (MU7F3)', 'Phone', '2023-10-23', '1748.99',  NOW() + interval '3' day, '150.00'),
       ('d1d018fe-cc1b-4f9c-9d53-bc8f5dd9b515','a79f1657-7ad2-4ebf-8a3f-0f6add0c993e','Apple MacBook Pro 16" 32 Gt, 512 Gt SSD -kannettava, tähtiharmaa', 'Computer', '2020-08-31', '3159.9',  NOW() + interval '7' day, '250.00'),
       ('f94a1a05-aca6-4957-9b31-b27e3ba06198','b7fc8a7a-4960-4d9a-8be1-3153d5ead480','Apple Magic Mouse 2 langaton laserhiiri, tähtiharmaa, MRME2', 'Computer accessories', '2021-06-02', '108.9',  NOW() + interval '2' month, '5.00'),
       ('ac63acfa-35bc-4ea4-aa2a-47470515596c','80a1a610-e73f-4637-9ca8-10d816f7f860','Belkin Power Bank 5K -varavirtalähde magneetilla, 5000 mAh, valkoinen', 'Phone accessories', '2024-06-06', '59.99',  NOW() + interval '4' month, '5.00'),
       ('ad0bc19f-79a6-45b7-978a-1b17fed94087','b4db23e1-e219-4775-b89b-7761bc63fdab','Lenovo ThinkVision T27p 27" 4K UHD', 'Display', '2022-09-28', '529.99',  NOW() + interval '3' month, '125.00'),
       ('4c36b5ec-eebc-4881-8e18-edc9c84a0b49','54fea623-fdbe-47e0-8404-be9cffa59cf2','Sony WF-1000XM5 langattomat vastamelunappikuulokkeet, hopea', 'Headphones', '2023-08-28', '329.99',NOW() - interval '1' month, '25.00');


INSERT INTO bid (id, fk_auction_item_id, bid_price, bidder_email, bid_time) VALUES
    ('75467def-b8cf-44dd-89a6-9fc0aa1a010f','b030b21b-73f9-40ff-8518-4a45f2c9b769', '5.00', 'bidder1@nitor.com','2025-02-16 13:30:00'),
    ('23c2869e-c8b3-4b72-b019-b457c1ad413c','b030b21b-73f9-40ff-8518-4a45f2c9b769', '5.00', 'bidder2@nitor.com','2025-02-16 13:31:00'),
    ('adaf2c80-7a00-4bd1-8afc-e89e09bf010a','b030b21b-73f9-40ff-8518-4a45f2c9b769', '5.00', 'bidder3@nitor.com','2025-02-16 13:32:00'),
    ('307a50ac-1feb-4aa4-8ea9-2f93fb7cf9f3','b030b21b-73f9-40ff-8518-4a45f2c9b769', '5.00', 'bidder2@nitor.com','2025-02-16 13:33:00'),
    ('7f0c311d-2f02-4562-a5e6-254908568f8b','b030b21b-73f9-40ff-8518-4a45f2c9b769', '5.00', 'bidder1@nitor.com','2025-02-16 13:34:00'),

    ('04ee6fa5-b429-4aec-81d4-73af0310548e','d1d018fe-cc1b-4f9c-9d53-bc8f5dd9b515', '5.00', 'bidder1@nitor.com','2025-02-16 13:30:00'),
    ('8fc96a91-aa08-41a9-9c60-4edd25efa89c','d1d018fe-cc1b-4f9c-9d53-bc8f5dd9b515', '5.00', 'bidder2@nitor.com','2025-02-16 13:31:00'),
    ('a0ad096e-78a6-4ff6-b798-cda6579f7b50','d1d018fe-cc1b-4f9c-9d53-bc8f5dd9b515', '5.00', 'bidder3@nitor.com','2025-02-16 13:32:00'),
    ('e4a693bb-2ce5-4c3b-9b46-0bec98e06c79','d1d018fe-cc1b-4f9c-9d53-bc8f5dd9b515', '5.00', 'bidder2@nitor.com','2025-02-16 13:33:00'),
    ('cf9e1c37-3647-4ad4-9539-23f592a32597','d1d018fe-cc1b-4f9c-9d53-bc8f5dd9b515', '5.00', 'bidder1@nitor.com','2025-02-16 13:34:00')



