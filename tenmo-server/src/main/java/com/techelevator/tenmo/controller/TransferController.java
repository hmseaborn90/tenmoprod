package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.AccountDao;
import com.techelevator.tenmo.dao.TransferDao;
import com.techelevator.tenmo.dao.UserDao;
import com.techelevator.tenmo.exception.DaoException;
import com.techelevator.tenmo.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(path = "/transfer")
@PreAuthorize("isAuthenticated()")
public class TransferController {

    private TransferDao transferDao;
    private UserDao userDao;
    private AccountDao accountDao;

   @Autowired
    public TransferController(TransferDao transferDao, UserDao userDao, AccountDao accountDao) {
        this.transferDao = transferDao;
        this.userDao = userDao;
        this.accountDao = accountDao;
    }

    @GetMapping
    public List<TransferDto> getTransfers(Principal principal){
       User user = userDao.getUserByUsername(principal.getName());
       return transferDao.getTransfers(user.getId());
    }

    @GetMapping("/pending")
    public List<TransferPendingDto> getPending(Principal principal) {
        User user = userDao.getUserByUsername(principal.getName());
        int accountTo = accountDao.getAccountByUserId(user.getId());

        List<TransferPendingDto> pendingTransfers = transferDao.getPendingTransfers(accountTo);

        return pendingTransfers;
    }

    @GetMapping(path = "/{id}")
    public TransferDetailsDto getTransferDetails(@PathVariable int id){
       return transferDao.getTransferDetails(id);
    }


    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> sendTeBucks(Principal principal, @RequestBody TransferRequestDto transferRequestDto){
        User user = userDao.getUserByUsername(principal.getName());
    try{
       transferDao.sendTeBucks(user.getId(), transferRequestDto.getUserTo(), transferRequestDto.getAmount());
        return ResponseEntity.status(201).body("Transfer approved");
    } catch (DaoException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
    @PostMapping("/request")
    public ResponseEntity<String> postTransferRequest(Principal principal, @RequestBody TransferRequestDto transferRequestDto){
        User user = userDao.getUserByUsername(principal.getName());
        int accountFrom = accountDao.getAccountByUserId(user.getId());
        int accountTo = accountDao.getAccountByUserId(transferRequestDto.getUserTo());
       transferDao.sendRequest(accountFrom, accountTo, transferRequestDto.getAmount());
       return ResponseEntity.status(201).body("Transfer request sent to user: " + transferRequestDto.getUserTo());

    }
    @PutMapping("/{transferId}/approve")
    public ResponseEntity<String> approveTransferRequest(
            @PathVariable int transferId,
            Principal principal) {
        User user = userDao.getUserByUsername(principal.getName());
        int userId = user.getId();

        try {

            Transfer transfer = transferDao.getTransferById(transferId);
            if (transfer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transfer not found.");
            }


            if (accountDao.getBalance(user.getUsername()).compareTo(transfer.getAmount()) >= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient balance to perform request.");
            }


            transferDao.handleTransferRequest(transferId, true, userId);
            return ResponseEntity.ok("Transfer request approved.");
        } catch (DaoException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{transferId}/reject")
    public ResponseEntity<String> rejectTransferRequest(
            @PathVariable int transferId,
            Principal principal) {
        User user = userDao.getUserByUsername(principal.getName());
        int userId = user.getId();

        try {
            transferDao.handleTransferRequest(transferId, false, userId);
            return ResponseEntity.ok("Transfer request rejected.");
        } catch (DaoException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
