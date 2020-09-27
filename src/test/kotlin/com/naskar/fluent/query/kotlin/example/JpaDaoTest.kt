package com.naskar.fluent.query.kotlin.example

import com.naskar.fluent.query.kotlin.example.domain.Account
import com.naskar.fluent.query.kotlin.example.domain.Customer
import com.naskar.fluentquery.Query
import com.naskar.fluentquery.jpa.dao.impl.DAOImpl
import com.naskar.fluentquery.metamodel.conventions.MetamodelConvention
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.math.BigDecimal
import javax.persistence.EntityManager
import javax.persistence.Persistence

@RunWith(Parameterized::class)
class JpaDaoTest(private val unitName: String) {

    private var em: EntityManager? = null
    private var dao: DAOImpl? = null

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<String> {
            return listOf("test-openjpa", "test-eclipselink", "test-hibernate")
        }
    }

    @Before
    fun setup() {
        val factory = Persistence.createEntityManagerFactory(unitName)
        em = factory.createEntityManager()

        val mc = MetamodelConvention()
        mc.addAll(em)

        dao = DAOImpl()
        dao!!.setEm(em)
        dao!!.setConvention(mc)

        em!!.transaction.begin()
    }

    @After
    fun tearDown() {
        if (em != null) {
            em!!.transaction.rollback()
            em!!.close()
        }
    }

    @Test
    fun testInsertSelectDao() {
        // Arrange
        val expected = Customer(id = 1, name = "test", regionCode = 25, minBalance = BigDecimal("100.0"))

        // Act
        // INSERT INTO TB_CUSTOMER (CD_CUSTOMER, VL_MIN_BALANCE, DS_NAME, NU_REGION_CODE)
        // VALUES (?, ?, ?, ?)
        dao!!.insert(expected)

        /*
            select e0.* from TB_CUSTOMER e0
            where e0.DS_NAME like ?
              and e0.NU_REGION_CODE = ?
              and e0.VL_MIN_BALANCE >= ?
         */
        val actual = dao!!.list(
            dao!!.query(Customer::class.java)
                .where { it.name }.like("t%")
                .and { it.regionCode }.eq(25)
                .and { it.minBalance }.ge(BigDecimal("100.0"))
        )

        // Assert
        Assert.assertEquals(1, actual.size)
        Assert.assertEquals(expected.id, actual[0].id)
        Assert.assertEquals(expected.name, actual[0].name)
        Assert.assertEquals(expected.regionCode, actual[0].regionCode)
        Assert.assertEquals(expected.minBalance, actual[0].minBalance)
    }

    @Test
    fun testSpecification() {
        // Arrange
        val expected = Customer(id = 2, name = "test2", regionCode = 35, minBalance = BigDecimal("200.0"))

        // Act
        dao!!.insert(expected)

        // needed define returnType with Unit due bug: https://youtrack.jetbrains.com/issue/KT-40269

        val nameStartsWithTSpec: (Query<Customer>) -> Unit = { q ->
            q.and { it.name }.like("t%")
        }

        val minBalance200Spec: (Query<Customer>) -> Unit = { q ->
            q.and { it.minBalance }.ge(BigDecimal("200.0"))
        }

        // select e0.* from TB_CUSTOMER e0
        // where (e0.DS_NAME like ?)
        //   and (e0.VL_MIN_BALANCE >= ?)
        val actual = dao!!.list(
            dao!!.query(Customer::class.java)
                .whereSpec(nameStartsWithTSpec)
                .andSpec(minBalance200Spec)
        )

        // Assert
        Assert.assertEquals(1, actual.size)
        Assert.assertEquals(expected.id, actual[0].id)
        Assert.assertEquals(expected.name, actual[0].name)
        Assert.assertEquals(expected.regionCode, actual[0].regionCode)
        Assert.assertEquals(expected.minBalance, actual[0].minBalance)
    }

    @Test
    fun testIn() {
        // Arrange
        val expectedCustomer = Customer(id = 3, name = "test3", regionCode = 35, minBalance = BigDecimal("200.0"))

        val expectedAccount0 = Account(id = 1, accountNumber = "ABB-1234", balance = BigDecimal("300.0"),
            customer = expectedCustomer)

        val expectedAccount1 = Account(id = 2, accountNumber = "ACC-2222", balance = BigDecimal("400.0"),
            customer = expectedCustomer)

        dao!!.insert(expectedCustomer)
        dao!!.insert(expectedAccount0)
        dao!!.insert(expectedAccount1)

        // Act
        val actual = dao!!.list(
            dao!!.query(Account::class.java)
                .where { it.accountNumber }.like("A%")
                .and { it.customer  }.`in`(Customer::class.java) { query, account ->
                    query.select { it.id }
                        .where { it.name }.like("t%")
                        .and { it.minBalance }.le(account.balance)
                }
                .orderBy { it.id }.asc()
        )

        // Assert
        Assert.assertEquals(2, actual.size)

        Assert.assertEquals(expectedAccount0.id, actual[0].id)
        Assert.assertEquals(expectedAccount0.accountNumber, actual[0].accountNumber)
        Assert.assertEquals(expectedAccount0.balance, actual[0].balance)
        Assert.assertEquals(expectedAccount0.customer!!.id, actual[0].customer!!.id)

        Assert.assertEquals(expectedAccount1.id, actual[1].id)
        Assert.assertEquals(expectedAccount1.accountNumber, actual[1].accountNumber)
        Assert.assertEquals(expectedAccount1.balance, actual[1].balance)
        Assert.assertEquals(expectedAccount1.customer!!.id, actual[1].customer!!.id)
    }

}
