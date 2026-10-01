package com.hungrybox.restaurant;

import com.hungrybox.restaurant.RestaurantDtos.MenuItem;
import com.hungrybox.restaurant.RestaurantDtos.Restaurant;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RestaurantRepository {

    private static final String RESTAURANT_COLUMNS =
            "id, name, cuisine, city, rating, delivery_time_mins, is_open";
    private static final String MENU_COLUMNS =
            "id, restaurant_id, name, description, category, price, is_veg, is_available";

    private static final RowMapper<Restaurant> RESTAURANT_MAPPER = RestaurantRepository::mapRestaurant;
    private static final RowMapper<MenuItem> MENU_MAPPER = RestaurantRepository::mapMenuItem;

    private final NamedParameterJdbcTemplate jdbc;

    public RestaurantRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Restaurant> search(String query, String cuisine, String city) {
        StringBuilder sql = new StringBuilder("SELECT " + RESTAURANT_COLUMNS + " FROM restaurants WHERE 1 = 1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE :q OR LOWER(cuisine) LIKE :q)");
            params.addValue("q", "%" + query.trim().toLowerCase() + "%");
        }
        if (cuisine != null && !cuisine.isBlank()) {
            sql.append(" AND LOWER(cuisine) = :cuisine");
            params.addValue("cuisine", cuisine.trim().toLowerCase());
        }
        if (city != null && !city.isBlank()) {
            sql.append(" AND LOWER(city) = :city");
            params.addValue("city", city.trim().toLowerCase());
        }
        sql.append(" ORDER BY is_open DESC, rating DESC, name");
        return jdbc.query(sql.toString(), params, RESTAURANT_MAPPER);
    }

    public Optional<Restaurant> findById(long id) {
        return jdbc.query("SELECT " + RESTAURANT_COLUMNS + " FROM restaurants WHERE id = :id",
                new MapSqlParameterSource("id", id), RESTAURANT_MAPPER).stream().findFirst();
    }

    public List<MenuItem> menuFor(long restaurantId) {
        return jdbc.query("SELECT " + MENU_COLUMNS + " FROM menu_items WHERE restaurant_id = :id ORDER BY category, name",
                new MapSqlParameterSource("id", restaurantId), MENU_MAPPER);
    }

    public List<MenuItem> menuItemsByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query("SELECT " + MENU_COLUMNS + " FROM menu_items WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", new ArrayList<>(ids)), MENU_MAPPER);
    }

    public Optional<MenuItem> findMenuItem(long id) {
        return jdbc.query("SELECT " + MENU_COLUMNS + " FROM menu_items WHERE id = :id",
                new MapSqlParameterSource("id", id), MENU_MAPPER).stream().findFirst();
    }

    public List<String> cuisines() {
        return jdbc.queryForList("SELECT DISTINCT cuisine FROM restaurants ORDER BY cuisine",
                new MapSqlParameterSource(), String.class);
    }

    public long insertRestaurant(String name, String cuisine, String city, BigDecimal rating, int deliveryTimeMins) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO restaurants (name, cuisine, city, rating, delivery_time_mins, is_open)
                VALUES (:name, :cuisine, :city, :rating, :deliveryTimeMins, TRUE)
                """,
                new MapSqlParameterSource()
                        .addValue("name", name)
                        .addValue("cuisine", cuisine)
                        .addValue("city", city)
                        .addValue("rating", rating)
                        .addValue("deliveryTimeMins", deliveryTimeMins),
                keys, new String[] {"id"});
        return keys.getKey().longValue();
    }

    public long insertMenuItem(long restaurantId, String name, String description, String category,
                               BigDecimal price, boolean veg) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO menu_items (restaurant_id, name, description, category, price, is_veg, is_available)
                VALUES (:restaurantId, :name, :description, :category, :price, :veg, TRUE)
                """,
                new MapSqlParameterSource()
                        .addValue("restaurantId", restaurantId)
                        .addValue("name", name)
                        .addValue("description", description)
                        .addValue("category", category)
                        .addValue("price", price)
                        .addValue("veg", veg),
                keys, new String[] {"id"});
        return keys.getKey().longValue();
    }

    public int setAvailability(long menuItemId, boolean available) {
        return jdbc.update("UPDATE menu_items SET is_available = :available WHERE id = :id",
                new MapSqlParameterSource().addValue("id", menuItemId).addValue("available", available));
    }

    private static Restaurant mapRestaurant(ResultSet rs, int rowNum) throws SQLException {
        return new Restaurant(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("cuisine"),
                rs.getString("city"),
                rs.getBigDecimal("rating"),
                rs.getInt("delivery_time_mins"),
                rs.getBoolean("is_open"));
    }

    private static MenuItem mapMenuItem(ResultSet rs, int rowNum) throws SQLException {
        return new MenuItem(
                rs.getLong("id"),
                rs.getLong("restaurant_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getBigDecimal("price"),
                rs.getBoolean("is_veg"),
                rs.getBoolean("is_available"));
    }
}
