# Fluent Query JPA Kotlin Examples

Example project using fluent-query with Kotlin and JPA Entities  

See more example in: [Fluent Query](https://github.com/naskarlab/fluent-query)

## Features

* Configuration over code: independence business code of the infrastructure code;
* Intrusive-less: zero or minimum changes for your code;
* Glue code: it’s only a small and simple classes set;
* Fluent Builder: code complete is your friend!


## Examples

### Entity

Simple Entity defining database columns names 

```

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
	var minBalance: Double? = null
)
			
```

### Using native sql

You can generate native sql queries using your domain classes: 

```

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
fun testInsert() {
    // Arrange
    val expected = "insert into TB_CUSTOMER (CD_CUSTOMER, DS_NAME, VL_MIN_BALANCE) values (:p0, :p1, :p2)"

    // Act
    val result = InsertBuilder()
        .into(Customer::class.java)
        .value { it.id }.set(1L)
        .value { it.name }.set("teste")
        .value { it.minBalance }.set(10.2)
        .to(NativeSQLInsertInto(mc))

    val actual = result.sql()

    // Assert
    Assert.assertEquals(expected, actual)
    Assert.assertEquals(result.params()["p0"], 1L)
    Assert.assertEquals(result.params()["p1"], "teste")
    Assert.assertEquals(result.params()["p2"], 10.2)
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
			
```

### Using the DAO class of the fluent-query-jpa

You can to use the DAO class, which already has all operations: 

```

@Before
fun setup() {
    val factory = Persistence.createEntityManagerFactory(unitName)
    em = factory.createEntityManager()

    val mc = MetamodelConvention()
    mc.addAll(em)

    dao = DAOImpl()
    dao!!.setEm(em)
    dao!!.setConvention(mc)
}

@Test
fun testInsertSelectDao() {
    // Arrange
    val expected = Customer(id = 1, name = "test", regionCode = 25, minBalance = 100.0)

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
                .and { it.minBalance }.ge(100.0)
    )

    // Assert
    Assert.assertEquals(1, actual.size)
    Assert.assertEquals(expected.id, actual[0].id)
    Assert.assertEquals(expected.name, actual[0].name)
    Assert.assertEquals(expected.regionCode, actual[0].regionCode)
    Assert.assertEquals(expected.minBalance, actual[0].minBalance)
}
			
```

## Advanced Concepts

### Defining Specifications Queries

Specification are smalls pieces of the predicate or WHERE-part which 
you can compose others queries   

```
@Test
fun testSpecification() {
    // Arrange
    val expected = Customer(id = 2, name = "test2", regionCode = 35, minBalance = 200.0)

    // Act
    dao!!.insert(expected)

    // needed define returnType with Unit due bug: https://youtrack.jetbrains.com/issue/KT-40269

    val nameStartsWithTSpec: (Query<Customer>) -> Unit = { q ->
        q.and { it.name }.like("t%")
    }

    val minBalance200Spec: (Query<Customer>) -> Unit = { q ->
        q.and { it.minBalance }.ge(200.0)
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
```

### Operator In 

```
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
			
```

## Usage with Gradle

```

allprojects {
    repositories {
        ...
        maven(url = "https://jitpack.io")
        ...
    }
}

...
implementation("com.github.naskarlab:fluent-query-jpa-metamodel:0.0.1")
implementation("com.github.naskarlab:fluent-query-jpa:0.3.1")
...

// or last versions

...
implementation("com.github.naskarlab:fluent-query-jpa-metamodel:master-SNAPSHOT")
implementation("com.github.naskarlab:fluent-query-jpa:master-SNAPSHOT")
...

```

