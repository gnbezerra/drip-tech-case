package br.com.usedrip.techcase.george.transferagent

import org.springframework.data.repository.CrudRepository

interface BankRepository : CrudRepository<Bank, Long> {
    fun existsByCode(code: String): Boolean

    fun findByCode(code: String): Bank?
}

interface CustomerRepository : CrudRepository<Customer, Long> {
    fun existsByCpf(cpf: String): Boolean

    fun findByCpf(cpf: String): Customer?
}

interface AccountRepository : CrudRepository<Account, Long> {
    fun existsByBankCodeAndBranchAndAccountNumber(bankCode: String, branch: String, accountNumber: String): Boolean

    fun findByBankCodeAndBranchAndAccountNumber(bankCode: String, branch: String, accountNumber: String): Account?
}

interface TransferLogRepository : CrudRepository<TransferLog, Long>
