package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.exception.DaoException;
import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.User;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcAccountDao implements AccountDao{

    private final JdbcTemplate jdbcTemplate;


    public JdbcAccountDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BigDecimal getBalance(String username) {
        BigDecimal balance = null;
        String sql = "SELECT balance FROM account " +
                "JOIN tenmo_user USING(user_id) " +
                "WHERE username = ?";
        try {
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, username);
            Account account = new Account();
            if(results.next()){
                balance = results.getBigDecimal("balance");
                account.setBalance(balance);
            }
        } catch (CannotGetJdbcConnectionException e){
            throw new DaoException("Unable to connect to server or database", e);
        }
        return balance;
    }

    @Override
    public List<User> getUsers(String username) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username " +
                "FROM tenmo_user " +
                "WHERE username != ?";

        try{
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, username);
            while(results.next()){
                User user = mapRowToUser(results);
                users.add(user);
            }
        }catch (CannotGetJdbcConnectionException e){
            throw new DaoException("Unable to connect to server or database", e);
        }
        return users;
    }

    @Override
    public int getAccountByUserId(int userId) {
        int accountId = 0;
        String sql = "SELECT account_id FROM account WHERE user_id = ?";
        try {
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId);
            if(results.next()){
                accountId = results.getInt("account_id");
            }
        } catch (CannotGetJdbcConnectionException e){
            throw new DaoException("Unable to connect to server or database", e);
        }
        return accountId;
    }

    private User mapRowToUser(SqlRowSet results){
        User user = new User();
        user.setId(results.getInt("user_id"));
        user.setUsername(results.getString("username"));
        return user;
    }

}
