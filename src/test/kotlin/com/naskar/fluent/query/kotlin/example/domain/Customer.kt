package com.naskar.fluent.query.kotlin.example.domain

import java.math.BigDecimal
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "TB_CUSTOMER")
class Customer(
	@Id
	@Column(name = "CD_CUSTOMER")
	var id: Long? = null,

	@Column(name = "DS_NAME")
	var name: String? = null,

	@Column(name = "NU_REGION_CODE")
	var regionCode: Long? = null,

	@Column(name = "VL_MIN_BALANCE")
	var minBalance: BigDecimal? = null,
)