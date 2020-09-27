package com.naskar.fluent.query.kotlin.example.domain

import java.math.BigDecimal
import javax.persistence.*

@Entity
@Table(name = "TB_ACCOUNT")
class Account(
    @Id
    @Column(name = "CD_ACCOUNT")
    var id: Long? = null,

    @Column(name = "NU_ACCOUNT")
    var accountNumber: String? = null,

    @Column(name = "VL_BALANCE")
    var balance: BigDecimal? = null,

    @ManyToOne
    @JoinColumn(name = "CD_CUSTOMER")
    var customer: Customer? = null
)

