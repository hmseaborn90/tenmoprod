package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.exception.DaoException;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferDetailsDto;
import com.techelevator.tenmo.model.TransferDto;
import com.techelevator.tenmo.model.TransferPendingDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcTransferDao implements TransferDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTransferDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public List<TransferDto> getTransfers(int userId) {
        List<TransferDto> transfers = new ArrayList<>();
        String sql = "SELECT transfer_id, afu.username as account_from, atu.username as account_to, amount " +
                "FROM transfer " +
                "JOIN account af ON transfer.account_from = af.account_id " +
                "JOIN tenmo_user afu ON af.user_id = afu.user_id " +
                "JOIN account at ON transfer.account_to = at.account_id " +
                "JOIN tenmo_user atu ON at.user_id = atu.user_id " +
                "WHERE af.user_id = ? OR atu.user_id = ?;";
        try {
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId, userId);
            while (results.next()) {
                TransferDto transfer = mapRowToTransfer(results);
                transfers.add(transfer);
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
        return transfers;
    }

    @Override
    public TransferDetailsDto getTransferDetails(int transferId) {
        TransferDetailsDto transferDetail = null;
        String sql = "SELECT transfer_id, transfer_type_desc, transfer_status_desc, afu.username as account_from, atu.username as account_to, amount " +
                "FROM transfer " +
                "JOIN transfer_status USING(transfer_status_id) " +
                "JOIN transfer_type USING(transfer_type_id) " +
                "JOIN account af ON transfer.account_from = af.account_id " +
                "JOIN tenmo_user afu ON af.user_id = afu.user_id " +
                "JOIN account at ON transfer.account_to = at.account_id " +
                "JOIN tenmo_user atu ON at.user_id = atu.user_id " +
                "WHERE transfer_id = ?;";
        try {
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, transferId);
            if (results.next()) {
                transferDetail = mapRowToTransferDetails(results);
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
        return transferDetail;
    }


    @Override
    public TransferDetailsDto sendRequest(int accountFrom, int accountTo, BigDecimal amount) {
        TransferDetailsDto newRequest;
        String sql = "INSERT INTO transfer (transfer_type_id, transfer_status_id, account_from, account_to, amount) " +
                "VALUES(1,1,?,?,?) " +
                "RETURNING transfer_id";
        try {
            int newRequestID = jdbcTemplate.queryForObject(sql, int.class, accountFrom, accountTo, amount);
            newRequest = getTransferDetails(newRequestID);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (DataIntegrityViolationException e) {
            throw new DaoException("Data integrity violation", e);
        }
        return newRequest;
    }

    @Override
    public List<TransferPendingDto> getPendingTransfers(int userId) {
        List<TransferPendingDto> pendingRequests = new ArrayList<>();
        String sql = "SELECT transfer_id, afu.username as account_from, amount\n" +
                "FROM transfer AS t\n" +
                "JOIN account af ON t.account_from = af.account_id\n" +
                "JOIN tenmo_user afu ON af.user_id = afu.user_id\n" +
                "JOIN transfer_status AS ts\n" +
                "    ON t.transfer_status_id = ts.transfer_status_id\n" +
                "WHERE ts.transfer_status_desc = 'Pending'\n" +
                "AND account_to = ?;";
        try {
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId);
            while (results.next()) {
                TransferPendingDto pendingRequest = mapRowToTransferPendingDto(results);
                pendingRequests.add(pendingRequest);
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
        return pendingRequests;
    }

    public void sendTeBucks(int fromUserId, int toUserId, BigDecimal amount) {

        String getAccountIdSql = "SELECT account_id FROM account WHERE user_id = ?";
        String insertTransferSql = "INSERT INTO transfer (transfer_type_id, transfer_status_id,account_from, account_to, amount) VALUES (?, ?, ?, ?, ?)";
        String updateAccountSql = "UPDATE account SET balance = balance + ? WHERE account_id = ?";
        String checkBalanceSql = "SELECT balance FROM account WHERE account_id = ?";

        if (fromUserId == toUserId) {
            throw new DaoException("Cannot send money to yourself.");
        }
        try {
            int fromAccountId;
            SqlRowSet fromAccountResult = jdbcTemplate.queryForRowSet(getAccountIdSql, fromUserId);
            if (fromAccountResult.next()) {
                fromAccountId = fromAccountResult.getInt("account_id");

            } else {
                throw new DaoException("Sender account not found.");
            }


            int toAccountId;
            SqlRowSet toAccountResult = jdbcTemplate.queryForRowSet(getAccountIdSql, toUserId);
            if (toAccountResult.next()) {
                toAccountId = toAccountResult.getInt("account_id");
            } else {
                throw new DaoException("Recipient account not found.");
            }
            BigDecimal currentBalance;
            SqlRowSet balanceResult = jdbcTemplate.queryForRowSet(checkBalanceSql, fromAccountId);
            if (balanceResult.next()) {
                currentBalance = balanceResult.getBigDecimal("balance");
            }else{
                throw new DaoException("Sender's balance not found.");
            }


            if (currentBalance.compareTo(amount) < 0) {
                throw new DaoException("Insufficient balance to create the transfer.");
            }

            // Create a new transfer record approved and send type by default
            jdbcTemplate.update(insertTransferSql,2, 2, fromAccountId, toAccountId, amount);

            // Update the balances of both accounts
            jdbcTemplate.update(updateAccountSql, amount.negate(), fromAccountId);
            jdbcTemplate.update(updateAccountSql, amount, toAccountId);

        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (DataIntegrityViolationException e) {
            throw new DaoException("Data integrity violation: " +  e.getMessage());
        }
    }

    public void handleTransferRequest(int transferId, boolean approve, int userId) {

        String getTransferSql = "SELECT account_from, account_to, amount, transfer_status_id FROM transfer WHERE transfer_id = ?";
        String updateTransferSql = "UPDATE transfer SET transfer_status_id = ? WHERE transfer_id = ?";
        String updateAccountSql = "UPDATE account SET balance = balance + ? WHERE account_id = ?";
        String checkBalanceSql = "SELECT balance FROM account WHERE account_id = ?";
        String getUserAccountIdSql = "SELECT account_id from account WHERE user_id = ?";


        try {

            SqlRowSet userAccountResult = jdbcTemplate.queryForRowSet(getUserAccountIdSql, userId);
            if (!userAccountResult.next()) {
                throw new DaoException("User account not found.");
            }
            int userAccountId = userAccountResult.getInt("account_id");
            SqlRowSet transferResult = jdbcTemplate.queryForRowSet(getTransferSql, transferId);
            if (transferResult.next()) {
                int accountFrom = transferResult.getInt("account_from");
                int accountTo = transferResult.getInt("account_to");
                BigDecimal amount = transferResult.getBigDecimal("amount");
                int currentStatusId = transferResult.getInt("transfer_status_id");
                int newStatusId = approve ? 2 : 3;

                if (accountTo != userAccountId){
                    throw new DaoException("Cannnnnont approve a transfer that is not requested for you");
                }
                if (currentStatusId == 2) {
                    throw new DaoException("The transfer request has already been approved.");
                }
                jdbcTemplate.update(updateTransferSql, newStatusId, transferId);
                if (approve) {
                    SqlRowSet balanceResult = jdbcTemplate.queryForRowSet(checkBalanceSql, accountFrom);
                    if (balanceResult.next()) {
                        BigDecimal currentBalance = balanceResult.getBigDecimal("balance");
                        if (currentBalance.compareTo(amount) >= 0) {

                            jdbcTemplate.update(updateAccountSql, amount, accountFrom);

                            jdbcTemplate.update(updateAccountSql, amount.negate(), accountTo);
                        } else {
                            throw new DaoException("Insufficient balance to approve the transfer.");
                        }
                    }
                }
            } else {
                throw new DaoException("Transfer not found.");
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (DataIntegrityViolationException e) {
            throw new DaoException("Data integrity violation", e);
        }
    }

    public Transfer getTransferById(int transferId){
        Transfer transfer = new Transfer();
        String sql = "SELECT * FROM transfer where transfer_id = ?";
        try {
            SqlRowSet result = jdbcTemplate.queryForRowSet(sql, transferId);
            if (result.next()) {
                transfer.setTransferId(result.getInt("transfer_id"));
                transfer.setTransfer_type_id(result.getInt("transfer_type_id"));
                transfer.setTransfer_status_id(result.getInt("transfer_status_id"));
                transfer.setAccount_from_id(result.getInt("account_from"));
                transfer.setAccount_to_id(result.getInt("account_to"));
                transfer.setAmount(result.getBigDecimal("amount"));
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
        return transfer;
    }


    private TransferDto mapRowToTransfer(SqlRowSet rs) {
        TransferDto transfer = new TransferDto();

        transfer.setTransferId(rs.getInt("transfer_id"));
        transfer.setAccountFrom(rs.getString("account_from"));
        transfer.setAccountTo(rs.getString("account_to"));
        transfer.setAmount(rs.getBigDecimal("amount"));

        return transfer;
    }

    private TransferPendingDto mapRowToTransferPendingDto(SqlRowSet rs) {
        TransferPendingDto transfer = new TransferPendingDto();

        transfer.setTransferId(rs.getInt("transfer_id"));
        transfer.setAccountFrom(rs.getString("account_from"));
        transfer.setAmount(rs.getBigDecimal("amount"));

        return transfer;
    }

    private TransferDetailsDto mapRowToTransferDetails(SqlRowSet rs) {
        TransferDetailsDto transferDetail = new TransferDetailsDto();
        transferDetail.setTranferId(rs.getInt("transfer_id"));
        transferDetail.setTransferType(rs.getString("transfer_type_desc"));
        transferDetail.setTransferStatus(rs.getString("transfer_status_desc"));
        transferDetail.setAccountFrom(rs.getString("account_from"));
        transferDetail.setAccountTo(rs.getString("account_to"));
        transferDetail.setAmount(rs.getBigDecimal("amount"));
        return transferDetail;
    }
}
