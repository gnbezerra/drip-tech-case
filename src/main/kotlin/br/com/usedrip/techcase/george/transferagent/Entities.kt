package br.com.usedrip.techcase.george.transferagent

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant

@Entity
class Bank(
    var name: String,
    @Column(unique = true) var code: String,
    @Id @GeneratedValue var id: Long? = null,
    @CreationTimestamp @Column(updatable = false) var createdAt: Instant? = null,
    @UpdateTimestamp var updatedAt: Instant? = null,
)

@Entity
class Customer(
    var fullName: String,
    @Column(unique = true) var cpf: String,
    @Id @GeneratedValue var id: Long? = null,
    @CreationTimestamp @Column(updatable = false) var createdAt: Instant? = null,
    @UpdateTimestamp var updatedAt: Instant? = null,
)

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["bank_id", "branch", "accountNumber"])])
class Account(
    @ManyToOne(optional = false) var bank: Bank,
    @ManyToOne(optional = false) var customer: Customer,
    var branch: String,
    var accountNumber: String,
    @Column(scale = 2) var money: BigDecimal = BigDecimal.ZERO,
    @Id @GeneratedValue var id: Long? = null,
    @CreationTimestamp @Column(updatable = false) var createdAt: Instant? = null,
    @UpdateTimestamp var updatedAt: Instant? = null,
)

@Entity
class TransferLog(
    @ManyToOne(optional = false) var sourceAccount: Account,
    @ManyToOne(optional = false) var destinationAccount: Account,
    @Column(scale = 2) var amount: BigDecimal,
    @Column(scale = 2) var commission: BigDecimal = BigDecimal.ZERO,
    @Suppress("unused") var performedAt: Instant = Instant.now(),
    @Id @GeneratedValue var id: Long? = null,
)
