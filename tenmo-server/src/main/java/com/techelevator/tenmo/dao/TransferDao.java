package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.*;
import com.techelevator.tenmo.model.TransferDetailsDto;

import java.math.BigDecimal;
import java.util.List;

public interface TransferDao {
    List<TransferDto> getTransfers(int userId);

    TransferDetailsDto getTransferDetails(int transferId);

    TransferDetailsDto sendRequest(int accountFrom, int accountTo, BigDecimal ammount);

    List<TransferPendingDto> getPendingTransfers(int userId);
    void handleTransferRequest(int transferId, boolean approve, int userId);

    void sendTeBucks(int fromUserId, int toUserId, BigDecimal amount);

    Transfer getTransferById(int transferId);
}
