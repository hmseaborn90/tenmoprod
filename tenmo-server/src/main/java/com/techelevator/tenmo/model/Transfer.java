package com.techelevator.tenmo.model;

import java.math.BigDecimal;

public class Transfer {
    private int transferId;
    private int transfer_type_id;

    private int transfer_status_id;
    private int account_from_id;
    private int account_to_id;
    private BigDecimal amount;

    public Transfer() {
    }

    public Transfer(int transferId, int transfer_type_id, int transfer_status_id, int account_from_id, int account_to_id, BigDecimal amount) {
        this.transferId = transferId;
        this.transfer_type_id = transfer_type_id;
        this.transfer_status_id = transfer_status_id;
        this.account_from_id = account_from_id;
        this.account_to_id = account_to_id;
        this.amount = amount;
    }

    public int getTransferId() {
        return transferId;
    }

    public void setTransferId(int transferId) {
        this.transferId = transferId;
    }

    public int getTransfer_type_id() {
        return transfer_type_id;
    }

    public void setTransfer_type_id(int transfer_type_id) {
        this.transfer_type_id = transfer_type_id;
    }

    public int getTransfer_status_id() {
        return transfer_status_id;
    }

    public void setTransfer_status_id(int transfer_status_id) {
        this.transfer_status_id = transfer_status_id;
    }

    public int getAccount_from_id() {
        return account_from_id;
    }

    public void setAccount_from_id(int account_from_id) {
        this.account_from_id = account_from_id;
    }

    public int getAccount_to_id() {
        return account_to_id;
    }

    public void setAccount_to_id(int account_to_id) {
        this.account_to_id = account_to_id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
