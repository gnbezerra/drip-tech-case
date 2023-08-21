package br.com.usedrip.techcase.george.transferagent

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.math.BigDecimal
import java.text.DecimalFormat

@Tag(name = "5. API Info")
@RestController
class ApiInfoController {
    @Operation(summary = "Show API info. Currently only shows the Swagger docs URL.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "406", description = "\"Accept\" header was not \"*/*\" or \"application/json\"")
        ]
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun info(): ApiInfoDTO {
        val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
        return ApiInfoDTO(swaggerUrl = "$baseUrl/swagger-ui.html")
    }
}

@Tag(name = "1. Bank operations")
@RestController
@Validated
@RequestMapping("/bank")
class BankController @Autowired constructor(val bankRepository: BankRepository, val modelMapper: ModelMapper) {
    @Operation(summary = "List all banks")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "406", description = "\"Accept\" header was not \"*/*\" or \"application/json\"")
        ]
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAll(): Iterable<BankDTO> {
        val banks = bankRepository.findAll()
        return modelMapper.map(banks, BankDTO::class.java)
    }

    @Operation(summary = "Save a new bank in the database")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Bank created"),
            ApiResponse(responseCode = "400", description = "Invalid data sent in the POST body"),
            ApiResponse(
                responseCode = "406",
                description = "\"Accept\" header was not \"*/*\" or \"application/json\""
            ),
            ApiResponse(responseCode = "409", description = "Database already has a bank with the same code"),
            ApiResponse(responseCode = "415", description = "\"Content-Type\" header was not \"application/json\""),
        ]
    )
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(code = HttpStatus.CREATED)
    fun save(@RequestBody @Valid bankDTO: BankDTO): BankDTO {
        if (bankRepository.existsByCode(bankDTO.code)) {
            throw BankCodeAlreadyExistsException("Bank with code ${bankDTO.code} already exists. Bank codes are unique.")
        }
        val bank = bankRepository.save(modelMapper.map(bankDTO, Bank::class.java))
        return modelMapper.map(bank, BankDTO::class.java)
    }
}

@Tag(name = "2. Customer operations")
@RestController
@Validated
@RequestMapping("/customer")
class CustomerController @Autowired constructor(
    val customerRepository: CustomerRepository,
    val modelMapper: ModelMapper,
) {
    @Operation(summary = "List all customers")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "406", description = "\"Accept\" header was not \"*/*\" or \"application/json\"")
        ]
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAll(): Iterable<CustomerDTO> {
        val customers = customerRepository.findAll()
        return modelMapper.map(customers, CustomerDTO::class.java)
    }

    @Operation(summary = "Save a new customer in the database")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Customer created"),
            ApiResponse(responseCode = "400", description = "Invalid data sent in the POST body"),
            ApiResponse(
                responseCode = "406",
                description = "\"Accept\" header was not \"*/*\" or \"application/json\""
            ),
            ApiResponse(responseCode = "409", description = "Database already has a customer with the same CPF"),
            ApiResponse(responseCode = "415", description = "\"Content-Type\" header was not \"application/json\""),
        ]
    )
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(code = HttpStatus.CREATED)
    fun save(@RequestBody @Valid customerDTO: CustomerDTO): CustomerDTO {
        if (customerRepository.existsByCpf(customerDTO.cpf)) {
            throw CPFAlreadyExistsException("Customer with CPF ${customerDTO.cpf} already exists")
        }
        val customer = customerRepository.save(modelMapper.map(customerDTO, Customer::class.java))
        return modelMapper.map(customer, CustomerDTO::class.java)
    }
}

@Tag(name = "3. Account operations")
@RestController
@Validated
@RequestMapping("/account")
class AccountController @Autowired constructor(
    val accountRepository: AccountRepository,
    val bankRepository: BankRepository,
    val customerRepository: CustomerRepository,
    val modelMapper: ModelMapper,
) {
    @Operation(summary = "List all accounts")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "406", description = "\"Accept\" header was not \"*/*\" or \"application/json\"")
        ]
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findAll(): Iterable<AccountDTO> {
        val accounts = accountRepository.findAll()
        return modelMapper.map(accounts, AccountDTO::class.java)
    }

    @Operation(summary = "Save a new account in the database. An account must be related to an existing bank and customer, use the bank and customer endpoints to have both in the database before calling this endpoint. For the purposes of this test, all accounts hold R\$ and there's no conversion of different currencies.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Account created"),
            ApiResponse(responseCode = "400", description = "Invalid data sent in the POST body"),
            ApiResponse(
                responseCode = "406",
                description = "\"Accept\" header was not \"*/*\" or \"application/json\""
            ),
            ApiResponse(
                responseCode = "409",
                description = "Database already has an account with the same bank code, branch and account number combination"
            ),
            ApiResponse(responseCode = "415", description = "\"Content-Type\" header was not \"application/json\""),
            ApiResponse(
                responseCode = "422",
                description = "Bank and/or customer related to this account were not found in the database"
            ),
        ]
    )
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(code = HttpStatus.CREATED)
    fun save(@RequestBody @Valid accountRequestDTO: AccountRequestDTO): AccountDTO {
        val bankCode = accountRequestDTO.bankCode
        val cpf = accountRequestDTO.customerCpf
        val branch = accountRequestDTO.branch
        val accountNumber = accountRequestDTO.accountNumber

        if (accountRepository.existsByBankCodeAndBranchAndAccountNumber(bankCode, branch, accountNumber)) {
            throw AccountAlreadyExistsException(
                "Account with bank code $bankCode, branch $branch and account number $accountNumber already exists."
            )
        }

        val bank = bankRepository.findByCode(bankCode)
            ?: throw BankNotFoundException("Bank with code $bankCode not found.")
        val customer = customerRepository.findByCpf(cpf)
            ?: throw CustomerNotFoundException("Customer with CPF $cpf not found.")

        var account = modelMapper.map(accountRequestDTO, Account::class.java)
        account.bank = bank
        account.customer = customer
        account = accountRepository.save(account)

        return modelMapper.map(account, AccountDTO::class.java)
    }
}

@Tag(name = "4. Transfer operations")
@RestController
@Validated
@RequestMapping("/transfer")
class TransferController
@Autowired
constructor(
    val accountRepository: AccountRepository,
    val modelMapper: ModelMapper,
    val transferLogRepository: TransferLogRepository,
    val transferService: TransferService,
) {
    companion object {
        val INTER_BANK_TRANSFER_COMMISSION = BigDecimal("5.00")
    }

    @Operation(summary = "Perform a transfer from one account to another. The source account must have enough funds for the transfer. Inter-bank transfers have a commission, check the INTER_BANK_TRANSFER_COMMISSION constant.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transfer successful"),
            ApiResponse(responseCode = "400", description = "Invalid data sent in the POST body"),
            ApiResponse(
                responseCode = "400",
                description = "Operation could not be completed because of lack of funds in the source account"
            ),
            ApiResponse(responseCode = "400", description = "Attempt of transfer from one account to the same account"),
            ApiResponse(
                responseCode = "406",
                description = "\"Accept\" header was not \"*/*\" or \"application/json\""
            ),
            ApiResponse(responseCode = "415", description = "\"Content-Type\" header was not \"application/json\""),
            ApiResponse(
                responseCode = "422",
                description = "One or both of the accounts related to this transfer were not found in the database"
            ),
        ]
    )
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Transactional
    fun transfer(@RequestBody @Valid request: TransferRequestDTO): TransferLogDTO {
        val sourceAccount = fetchAccount(request.sourceAccount)
        val destinationAccount = fetchAccount(request.destinationAccount)

        if (sourceAccount.id == destinationAccount.id) {
            throw OperationNotAllowedException("Transfers from one account to the same account are not allowed")
        }

        if (sourceAccount.money < request.amount) {
            throw InsufficientFundsException(
                "Transfer of R$ ${
                    DecimalFormat("#,##0.00").format(request.amount)
                } requested, but source account has R$ ${
                    DecimalFormat("#,##0.00").format(sourceAccount.money)
                }"
            )
        }

        val isSameBankTransfer = sourceAccount.bank.id == destinationAccount.bank.id
        val commission = if (isSameBankTransfer) BigDecimal.ZERO else INTER_BANK_TRANSFER_COMMISSION

        if (sourceAccount.money < request.amount + commission) {
            throw InsufficientFundsException(
                "Account does not have enough funds to pay the inter-bank R$ ${
                    DecimalFormat("#,##0.00").format(commission)
                } commission"
            )
        }

        // perform the operation with the most chance for retries/failures first, so a rollback is less impactful
        transferService.transfer(
            sourceAccount = sourceAccount,
            moneyDecreaseOnSource = request.amount + commission,
            destinationAccount = destinationAccount,
            moneyIncreaseOnDestination = request.amount
        )

        sourceAccount.money -= (request.amount + commission)
        destinationAccount.money += request.amount
        accountRepository.saveAll(listOf(sourceAccount, destinationAccount))

        var transferLog = TransferLog(sourceAccount, destinationAccount, request.amount, commission)
        transferLog = transferLogRepository.save(transferLog)

        return modelMapper.map(transferLog, TransferLogDTO::class.java)
    }

    private fun fetchAccount(accountIdentificationDTO: AccountIdentificationDTO): Account {
        val bankCode = accountIdentificationDTO.bankCode
        val branch = accountIdentificationDTO.branch
        val accountNumber = accountIdentificationDTO.accountNumber

        return accountRepository.findByBankCodeAndBranchAndAccountNumber(bankCode, branch, accountNumber)
            ?: throw AccountNotFoundException(
                "Account not found for bank code $bankCode, branch $branch and account number $accountNumber"
            )
    }
}
