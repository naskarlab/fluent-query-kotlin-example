package com.naskar.fluent.query.kotlin.example

import com.naskar.fluent.query.kotlin.example.domain.Customer
import com.naskar.fluentquery.InsertBuilder
import com.naskar.fluentquery.QueryBuilder
import com.naskar.fluentquery.converters.NativeSQL
import com.naskar.fluentquery.converters.NativeSQLInsertInto
import com.naskar.fluentquery.metamodel.conventions.MetamodelConvention
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.persistence.EntityManager
import javax.persistence.Persistence
import java.math.BigDecimal

@RunWith(Parameterized::class)
class MetamodelMappingTest(private val unitName: String) {

    private var mc: MetamodelConvention? = null
    private var em: EntityManager? = null

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
        mc = MetamodelConvention()
        mc!!.addAll(em)
    }

    @After
    fun tearDown() {
        if (em != null) {
            em!!.close()
        }
    }

    @Test
    fun testSelect() {
        // Arrange
        val expected = "select e0.* from TB_CUSTOMER e0"

        // Act
        val actual = QueryBuilder()
                .from(Customer::class.java)
                .to(NativeSQL(mc))
                .sql()

        // Assert
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testMapping() {
        // Arrange

        // Act
        val result = QueryBuilder()
                .from(Customer::class.java)
                .where { it.id }.eq(1L)
                .and { it.name }.like("r%")
                .select { it.id }
                .select { it.name }
                .to(NativeSQL(mc))
        val q = em!!.createNativeQuery(result.sqlValues())
        for (i in result.values().indices) {
            q.setParameter(i + 1, result.values()[i])
        }

        // Assert
        Assert.assertTrue(q.resultList.isEmpty())
    }

    @Test
    fun testInsert() {
        // Arrange
        val expected = "insert into TB_CUSTOMER (CD_CUSTOMER, DS_NAME, VL_MIN_BALANCE) values (:p0, :p1, :p2)"

        // Act
        val result = InsertBuilder()
            .into(Customer::class.java)
            .value { it.id }.set(1L)
            .value { it.name }.set("test")
            .value { it.minBalance }.set(BigDecimal("10.2"))
            .to(NativeSQLInsertInto(mc))

        val actual = result.sql()

        // Assert
        Assert.assertEquals(expected, actual)
        Assert.assertEquals(result.params()["p0"], 1L)
        Assert.assertEquals(result.params()["p1"], "test")
        Assert.assertEquals(result.params()["p2"], BigDecimal("10.2"))
    }

}
