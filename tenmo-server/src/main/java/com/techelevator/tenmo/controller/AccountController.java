package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.AccountDao;
import com.techelevator.tenmo.dao.UserDao;
import com.techelevator.tenmo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/account/")
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private AccountDao accountDao;
    private UserDao userDao;

    @Autowired
    public AccountController(AccountDao accountDao, UserDao userDao) {
        this.accountDao = accountDao;
        this.userDao = userDao;

    }

    @GetMapping("balance")
    @PreAuthorize("hasRole('USER')")
    public BigDecimal getBalance(Principal principal){
//        System.out.println(principal.toString());
//        User user = userDao.getUserByUsername(principal.getName());
        return accountDao.getBalance(principal.getName());

    }

    @GetMapping("users")
    public List<User> getUsersNeCurrent(Principal principal) {
//        User user = userDao.getUserByUsername(principal.getName());
        return accountDao.getUsers(principal.getName());
    }


}
