package br.com.usedrip.techcase.george.transferagent

import javax.naming.ServiceUnavailableException

class AccountAlreadyExistsException(message: String? = null) : Exception(message)

class AccountNotFoundException(message: String? = null) : Exception(message)

class BankCodeAlreadyExistsException(message: String? = null) : Exception(message)

class BankNotFoundException(message: String? = null) : Exception(message)

class CPFAlreadyExistsException(message: String? = null) : Exception(message)

class CustomerNotFoundException(message: String? = null) : Exception(message)

class InsufficientFundsException(message: String? = null) : Exception(message)

class OperationNotAllowedException(message: String? = null) : Exception(message)

class TransferServiceFailureException(explanation: String? = null) :
    ServiceUnavailableException(explanation)
