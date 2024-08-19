package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface AccountDao {
    BigDecimal getBalance(String username);

    List<User> getUsers(String username);

    int getAccountByUserId(int id);
}
