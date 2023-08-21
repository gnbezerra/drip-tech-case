package br.com.usedrip.techcase.george.transferagent

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import uk.co.jemos.podam.common.PodamStrategyValue
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "Bank data. It uses standard Brazil COMPE code (3 digits) for the bank code. ISPB and SWIFT codes are not supported by now.")
data class BankDTO(
    @field:Schema(description = "Bank name", example = "Itaú")
    @field:NotBlank(message = "Name is required")
    var name: String,

    @field:Schema(description = "COMPE code for the bank, must be a string with 3 digits", example = "001")
    @field:NotNull(message = "Bank code is required")
    @field:Pattern(regexp = "[0-9]{3}", message = "Bank code must be a number with 3 digits")
    @field:PodamStrategyValue(BankCodeStrategy::class)
    var code: String,

    @field:Schema(description = "Internal ID of the bank", readOnly = true, example = "1")
    @field:Null(message = "Field id is not allowed")
    var id: Long? = null,

    @field:Schema(description = "Date and time when the record was created", readOnly = true)
    @field:Null(message = "Field createdAt is not allowed")
    var createdAt: Instant? = null,

    @field:Schema(description = "Date and time when the record was last updated", readOnly = true)
    @field:Null(message = "Field updatedAt is not allowed")
    var updatedAt: Instant? = null,
) {
    constructor() : this("", "")
}

@Schema(description = "Customer data. Name and CPF were used as example data, as this is not the focus of the test.")
data class CustomerDTO(
    @field:Schema(description = "Full name of the customer", example = "George Nicolau Bezerra")
    @field:NotBlank(message = "Full name is required")
    var fullName: String,

    @field:Schema(description = "CPF of the customer with 11 digits, no dots or dashes", example = "12345678901")
    @field:NotNull(message = "CPF is required")
    @field:Pattern(regexp = "[0-9]{11}", message = "CPF must be a number with 11 digits, with no dashes or dots")
    @field:PodamStrategyValue(CPFStrategy::class)
    var cpf: String,

    @field:Schema(description = "Internal ID of the customer", readOnly = true, example = "1")
    @field:Null(message = "Field id is not allowed")
    var id: Long? = null,

    @field:Schema(description = "Date and time when the record was created", readOnly = true)
    @field:Null(message = "Field createdAt is not allowed")
    var createdAt: Instant? = null,

    @field:Schema(description = "Date and time when the record was last updated", readOnly = true)
    @field:Null(message = "Field updatedAt is not allowed")
    var updatedAt: Instant? = null,
) {
    constructor() : this("", "")
}

@Schema(description = "Model for saving a new account. An account must be related to an existing bank and customer. For the purposes of this test, all accounts hold R\$ and there's no conversion of different currencies.")
data class AccountRequestDTO(
    @field:Schema(
        description = "COMPE code for the bank holding the account, must be a string with 3 digits",
        example = "001"
    )
    @field:NotNull(message = "Bank code is required")
    @field:Pattern(regexp = "[0-9]{3}", message = "Bank code must be a number with 3 digits")
    @field:PodamStrategyValue(BankCodeStrategy::class)
    var bankCode: String,

    @field:Schema(
        description = "CPF of the customer that is owner of the account, with 11 digits, no dots or dashes",
        example = "12345678901"
    )
    @field:NotNull(message = "CPF is required")
    @field:Pattern(regexp = "[0-9]{11}", message = "CPF must be a number with 11 digits, with no dashes or dots")
    @field:PodamStrategyValue(CPFStrategy::class)
    var customerCpf: String,

    @field:Schema(description = "Branch of the bank that has this account", example = "1234")
    @field:NotBlank(message = "Branch is required")
    var branch: String,

    @field:Schema(
        description = "Account \"number\". Regardless of the name, it can have dashes and letters as well.",
        example = "98765-X"
    )
    @field:NotBlank(message = "Account number is required")
    var accountNumber: String,

    @field:Schema(description = "How much money (in R$) this account has. Default is 0.00.", example = "1500.00")
    @field:Digits(integer = 100, fraction = 2, message = "Money can only have up to 2 decimal places")
    @field:PositiveOrZero(message = "Money cannot be negative")
    var money: BigDecimal? = BigDecimal.ZERO,
) {
    constructor() : this("", "", "", "")
}

@Schema(description = "Account data. For the purposes of this test, all accounts hold R\$ and there's no conversion of different currencies.")
data class AccountDTO(
    @field:Schema(description = "Bank holding the account")
    var bank: BankDTO,

    @field:Schema(description = "Customer who owns the account")
    var customer: CustomerDTO,

    @field:Schema(description = "Branch of the bank that has this account", example = "1234")
    var branch: String,

    @field:Schema(
        description = "Account \"number\". Regardless of the name, it can have dashes and letters as well.",
        example = "98765-X"
    )
    var accountNumber: String,

    @field:Schema(description = "How much money (in R$) this account has", example = "1500.00")
    var money: BigDecimal,

    @field:Schema(description = "Internal ID of the account", readOnly = true, example = "1")
    var id: Long? = null,

    @field:Schema(description = "Date and time when the record was created", readOnly = true)
    var createdAt: Instant? = null,

    @field:Schema(description = "Date and time when the record was last updated", readOnly = true)
    var updatedAt: Instant? = null,
) {
    constructor() : this(BankDTO(), CustomerDTO(), "", "", BigDecimal.ZERO)
}

@Schema(description = "Model for making a transfer from one account to another.")
data class TransferRequestDTO(
    @field:Schema(description = "The account where the money will come from")
    @field:NotNull(message = "Source account field is required")
    @field:Valid
    var sourceAccount: AccountIdentificationDTO,

    @field:Schema(description = "The account where the money will go to")
    @field:NotNull(message = "Destination account field is required")
    @field:Valid
    var destinationAccount: AccountIdentificationDTO,

    @field:Schema(description = "The amount of money (in R$) to transfer", example = "500.00")
    @field:NotNull(message = "Amount field is required")
    @field:Digits(integer = 100, fraction = 2, message = "Amount can only have up to 2 decimal places")
    @field:Positive(message = "Amount must be greater than zero")
    var amount: BigDecimal,
) {
    constructor() : this(AccountIdentificationDTO(), AccountIdentificationDTO(), BigDecimal.ZERO)
}

@Schema(description = "Model with data that can uniquely identify an account.")
data class AccountIdentificationDTO(
    @field:Schema(
        description = "COMPE code for the bank holding the account, must be a string with 3 digits",
        example = "001"
    )
    @field:NotNull(message = "Bank code is required")
    @field:Pattern(regexp = "[0-9]{3}", message = "Bank code must be a number with 3 digits")
    @field:PodamStrategyValue(BankCodeStrategy::class)
    var bankCode: String,

    @field:Schema(description = "Branch of the bank that has this account", example = "1234")
    @field:NotBlank(message = "Branch is required")
    var branch: String,

    @field:Schema(
        description = "Account \"number\". Regardless of the name, it can have dashes and letters as well.",
        example = "98765-X"
    )
    @field:NotBlank(message = "Account number is required")
    var accountNumber: String,
) {
    constructor() : this("", "", "")
}

@Schema(description = "Log of a successful transfer transaction")
data class TransferLogDTO(
    @field:Schema(description = "Account from where the money left")
    var sourceAccount: AccountDTO,

    @field:Schema(description = "Account to where the money went")
    var destinationAccount: AccountDTO,

    @field:Schema(description = "The money amount transferred, in R\$", example = "450.00")
    var amount: BigDecimal,

    @field:Schema(description = "Commission charged for the transfer transaction, in R\$", example = "5.00")
    var commission: BigDecimal,

    @field:Schema(description = "Date and time when the transfer happened")
    var performedAt: Instant,

    @field:Schema(description = "Internal ID of the transfer", readOnly = true, example = "1")
    var id: Long? = null,
) {
    constructor() : this(AccountDTO(), AccountDTO(), BigDecimal.ZERO, BigDecimal.ZERO, Instant.now())
}

@Schema(description = "API info. Currently only has the Swagger docs URL.")
data class ApiInfoDTO(
    @field:Schema(description = "URL for the Swagger docs", example = "http://localhost:8080/swagger-ui.html")
    val swaggerUrl: String,
)
