package com.savingtransaction.app.controllers;

import com.savingtransaction.app.models.documents.Transaction;
import com.savingtransaction.app.models.dto.SavingAccount;
import com.savingtransaction.app.services.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
@Slf4j
@RestController
@RequestMapping("/transactionSavingAccount")
public class TransactionController {
    @Autowired
    TransactionService service;

    @GetMapping("list")
    public Flux<Transaction> findAll(){
        return service.findAll();
    }

    @GetMapping("/find/{id}")
    public Mono<Transaction> findById(@PathVariable String id){
        return service.findById(id);
    }

    @GetMapping("/buscar/{id}")
    public Mono<SavingAccount> findByIdSaving(@PathVariable String id){
        return service.findSavingAccountById(id);
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<Transaction>> create(@RequestBody Transaction savingAccountTransaction){

        return service.countMovements(savingAccountTransaction.getSavingAccount().getId()) // N° Movimientos actuales
                .flatMap(cnt -> {
                    log.info("id Cuenta Controlador: "+savingAccountTransaction.getSavingAccount().getId());
                    return service.findSavingAccountById(savingAccountTransaction.getSavingAccount().getId()) // Cuenta Bancaria
                            .filter(sa -> sa.getLimitTransactions() > cnt)
                            .flatMap(sa -> {
                                switch (savingAccountTransaction.getTypeTransaction()){
                                    case DEPOSIT: sa.setBalance(sa.getBalance() + savingAccountTransaction.getTransactionAmount()); break;
                                    case DRAFT: sa.setBalance(sa.getBalance() - savingAccountTransaction.getTransactionAmount()); break;
                                }
                                if(cnt >= sa.getFreeTransactions() ){
                                    sa.setBalance(sa.getBalance() - sa.getCommissionTransactions());
                                    savingAccountTransaction.setCommissionAmount(sa.getCommissionTransactions());
                                }else{
                                    savingAccountTransaction.setCommissionAmount(0.0);
                                }

                                return service.updateSavingAccount(sa)
                                        .flatMap(saveAcc -> {
                                            savingAccountTransaction.setSavingAccount(saveAcc);
                                            savingAccountTransaction.setTransactionDate(LocalDateTime.now());
                                            return service.create(savingAccountTransaction);
                                        });
                            }).map(sat ->new ResponseEntity<>(sat , HttpStatus.CREATED) );
                }).defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @PutMapping("/update")
    public Mono<ResponseEntity<Transaction>> update(@RequestBody Transaction transaction) {

        return service.findSavingAccountById(transaction.getSavingAccount().getId())
                .flatMap(sa -> {
                    return service.findById(transaction.getId())
                            .flatMap(sat -> {
                                switch (transaction.getTypeTransaction()){
                                    case DEPOSIT: sa.setBalance(sa.getBalance() - sat.getTransactionAmount() + transaction.getTransactionAmount());
                                        return service.updateSavingAccount(sa).flatMap(saUpdate -> {
                                            transaction.setSavingAccount(saUpdate);
                                            transaction.setTransactionDate(LocalDateTime.now());
                                            return service.update(transaction);
                                        });
                                    case DRAFT: sa.setBalance(sa.getBalance() + sat.getTransactionAmount() - transaction.getTransactionAmount());
                                        return service.updateSavingAccount(sa).flatMap(saUpdate -> {
                                            transaction.setSavingAccount(saUpdate);
                                            transaction.setTransactionDate(LocalDateTime.now());
                                            return service.update(transaction);
                                        });
                                    default: return Mono.empty();
                                }
                            });
                })
                .map(sat ->new ResponseEntity<>(sat , HttpStatus.CREATED))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @DeleteMapping("/delete/{id}")
    public Mono<ResponseEntity<String>> delete(@PathVariable String id) {
        return service.delete(id)
                .filter(deleteCustomer -> deleteCustomer)
                .map(deleteCustomer -> new ResponseEntity<>("Transaction Deleted", HttpStatus.ACCEPTED))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
