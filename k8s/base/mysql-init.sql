-- One MySQL instance, one schema per service. Each service owns its data
-- and only talks to the others over HTTP or Kafka, never through shared tables.
CREATE DATABASE IF NOT EXISTS hb_users;
CREATE DATABASE IF NOT EXISTS hb_restaurants;
CREATE DATABASE IF NOT EXISTS hb_orders;
CREATE DATABASE IF NOT EXISTS hb_billing;

GRANT ALL PRIVILEGES ON hb_users.*       TO 'hungrybox'@'%';
GRANT ALL PRIVILEGES ON hb_restaurants.* TO 'hungrybox'@'%';
GRANT ALL PRIVILEGES ON hb_orders.*      TO 'hungrybox'@'%';
GRANT ALL PRIVILEGES ON hb_billing.*     TO 'hungrybox'@'%';
FLUSH PRIVILEGES;
