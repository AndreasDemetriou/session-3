package ru.sbt.jschool.session3.problem1;

import java.util.*;

/**
 */
public class AccountServiceImpl implements AccountService {
    protected FraudMonitoring fraudMonitoring;

    Map<Long, Account> accounts= new HashMap<>();
    //Set<Payment> payments = new HashSet<>();
    Set<Long> paymentsId = new HashSet<>();

    public AccountServiceImpl(FraudMonitoring fraudMonitoring) {
        this.fraudMonitoring = fraudMonitoring;
    }

    @Override public Result create(long clientID, long accountID, float initialBalance, Currency currency) {
        if(fraudMonitoring.check(clientID)) return Result.FRAUD;
        if(accounts.containsKey(accountID)) return Result.ALREADY_EXISTS;
        accounts.put(accountID, new Account(clientID,accountID,currency,initialBalance));
        return Result.OK;
    }

    @Override public List<Account> findForClient(long clientID) {
        List<Account> result = new ArrayList<>();
        for( Long key: accounts.keySet() ){
            if(accounts.get(key).getClientID() == clientID)
                result.add(accounts.get(key));
        }
        if(result.size()!=0) return result;
        else {
            return Collections.EMPTY_LIST;
        }
    }

    @Override public Account find(long accountID) {
        if(accounts.containsKey(accountID)) return accounts.get(accountID);
        return null;
    }

    @Override
    public Result doPayment(Payment payment) {
        long tmpRecipientID = payment.getRecipientID();
        long tmpPayerID = payment.getPayerID();
        if (paymentsId.contains(payment.getOperationID()))
            return Result.ALREADY_EXISTS;
        if (!accounts.containsKey(payment.getPayerAccountID()) || (this.findForClient(tmpPayerID).equals(Collections.EMPTY_LIST)))
            return Result.PAYER_NOT_FOUND;
        if ((!accounts.containsKey(payment.getRecipientAccountID())) || (this.findForClient(tmpRecipientID).equals(Collections.EMPTY_LIST)))
            return Result.RECIPIENT_NOT_FOUND;
        paymentsId.add(payment.getOperationID());
        if (accounts.get(payment.getPayerAccountID()).getCurrency() == accounts.get(payment.getRecipientAccountID()).getCurrency()) {
            float newPayersBalance = accounts.get(payment.getPayerAccountID()).getBalance() - payment.getAmount();
            accounts.replace(payment.getPayerAccountID(), new Account(payment.getPayerID(), payment.getPayerAccountID(), accounts.get(payment.getPayerAccountID()).getCurrency(), newPayersBalance));
            float newRecipientBalance = accounts.get(payment.getRecipientAccountID()).getBalance() + payment.getAmount();
            accounts.replace(payment.getRecipientAccountID(), new Account(payment.getRecipientID(), payment.getRecipientAccountID(), accounts.get(payment.getRecipientAccountID()).getCurrency(), newRecipientBalance));
        } else doPaymentWithCurrency(payment);
        return Result.OK;
    }

    private void doPaymentWithCurrency(Payment payment) {
        float newPayersBalance = accounts.get(payment.getPayerAccountID()).getBalance() - payment.getAmount();
        accounts.replace(payment.getPayerAccountID(), new Account(payment.getPayerID(), payment.getPayerAccountID(), accounts.get(payment.getPayerAccountID()).getCurrency(), newPayersBalance));
        float newRecipientBalance = accounts.get(payment.getRecipientAccountID()).getBalance() + accounts.get(payment.getPayerAccountID()).getCurrency().to(payment.getAmount(), accounts.get(payment.getRecipientAccountID()).getCurrency());
        accounts.replace(payment.getRecipientAccountID(), new Account(payment.getRecipientID(), payment.getRecipientAccountID(), accounts.get(payment.getRecipientAccountID()).getCurrency(), newRecipientBalance));
    }
}
