package com.example.library_management.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IdGeneratorService {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public String generate(String name, String prefix) {

        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Integer value;

        List<Integer> result = jdbcTemplate.query(
                "SELECT current_value FROM id_sequences WHERE name = ? AND date_key = ? FOR UPDATE",
                (rs, rowNum) -> rs.getInt("current_value"),
                name, date
        );

        if (result.isEmpty()) {
            value = 1;

            jdbcTemplate.update(
                    "INSERT INTO id_sequences(name, date_key, current_value) VALUES (?, ?, ?)",
                    name, date, value
            );
        } else {
            value = result.get(0) + 1;

            jdbcTemplate.update(
                    "UPDATE id_sequences SET current_value = ? WHERE name = ? AND date_key = ?",
                    value, name, date
            );
        }
        if (prefix.equals("#")) {
           return prefix + String.format("%05d", value);
        }
        return prefix + date + "-" + String.format("%05d", value);
    }
}
