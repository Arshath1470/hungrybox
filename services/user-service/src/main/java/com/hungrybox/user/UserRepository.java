package com.hungrybox.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final String COLUMNS =
            "id, full_name, email, password_hash, phone, address, role, created_at";

    private static final RowMapper<User> ROW_MAPPER = UserRepository::mapRow;

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(String fullName, String email, String passwordHash, String phone, String role) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO users (full_name, email, password_hash, phone, role)
                VALUES (:fullName, :email, :passwordHash, :phone, :role)
                """,
                new MapSqlParameterSource()
                        .addValue("fullName", fullName)
                        .addValue("email", email)
                        .addValue("passwordHash", passwordHash)
                        .addValue("phone", phone)
                        .addValue("role", role),
                keys, new String[] {"id"});
        return keys.getKey().longValue();
    }

    public Optional<User> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM users WHERE id = :id",
                new MapSqlParameterSource("id", id), ROW_MAPPER).stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return jdbc.query("SELECT " + COLUMNS + " FROM users WHERE email = :email",
                new MapSqlParameterSource("email", email), ROW_MAPPER).stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = :email",
                new MapSqlParameterSource("email", email), Integer.class);
        return count != null && count > 0;
    }

    public void updateProfile(long id, String fullName, String phone, String address) {
        jdbc.update("""
                UPDATE users SET full_name = :fullName, phone = :phone, address = :address
                WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("fullName", fullName)
                        .addValue("phone", phone)
                        .addValue("address", address));
    }

    public List<User> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM users ORDER BY id", new MapSqlParameterSource(), ROW_MAPPER);
    }

    private static User mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new User(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("role"),
                createdAt == null ? null : createdAt.toInstant());
    }
}
