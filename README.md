Kotlin SQL Library
==================

_Exposed_ is a prototype for a lightweight SQL library written over JDBC driver for [Kotlin](https://github.com/JetBrains/kotlin) language.
It does have two layers of database access: typesafe SQL wrapping DSL and lightweight data access objects

## SQL DSL sample:
```kotlin
object Users : Table() {
    val id = varchar("id", 10).primaryKey() // PKColumn<String>
    val name = varchar("name", length = 50) // Column<String>
    val cityId = (integer("city_id") references Cities.id).nullable() // Column<Int?>
}

object Cities : Table() {
    val id = integer("id").autoIncrement().primaryKey() // PKColumn<Int>
    val name = varchar("name", 50) // Column<String>
}

fun main(args: Array<String>) {
    val db = Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")

    db.transaction {
        create (Cities, Users)

        val saintPetersburgId = Cities.insert {
            it[name] = "St. Petersburg"
        } get Cities.id

        val munichId = Cities.insert {
            it[name] = "Munich"
        } get Cities.id

        Cities.insert {
            it[name] = "Prague"
        }

        Users.insert {
            it[id] = "andrey"
            it[name] = "Andrey"
            it[cityId] = saintPetersburgId
        }

        Users.insert {
            it[id] = "sergey"
            it[name] = "Sergey"
            it[cityId] = munichId
        }

        Users.insert {
            it[id] = "eugene"
            it[name] = "Eugene"
            it[cityId] = munichId
        }

        Users.insert {
            it[id] = "alex"
            it[name] = "Alex"
            it[cityId] = null
        }

        Users.insert {
            it[id] = "smth"
            it[name] = "Something"
            it[cityId] = null
        }

        Users.update({Users.id eq "alex"}) {
            it[name] = "Alexey"
        }

        Users.deleteWhere{Users.name like "%thing"}

        println("All cities:")

        for (city in Cities.selectAll()) {
            println("${city[Cities.id]}: ${city[Cities.name]}")
        }

        println("Manual join:")
        (Users join Cities).slice(Users.name, Cities.name).
            select {(Users.id.eq("andrey") or Users.name.eq("Sergey")) and
                    Users.id.eq("sergey") and Users.cityId.eq(Cities.id)}.forEach {
            println("${it[Users.name]} lives in ${it[Cities.name]}")
        }

        println("Join with foreign key:")


        (Users join Cities).slice(Users.name, Users.cityId, Cities.name).
                select {Cities.name.eq("St. Petersburg") or Users.cityId.isNull()}.forEach {
            if (it[Users.cityId] != null) {
                println("${it[Users.name]} lives in ${it[Cities.name]}")
            }
            else {
                println("${it[Users.name]} lives nowhere")
            }
        }

        println("Functions and group by:")

        ((Cities join Users).slice(Cities.name, Users.id.count()).selectAll() groupBy Cities.name).forEach {
            val cityName = it[Cities.name]
            val userCount = it[Users.id.count()]

            if (userCount > 0) {
                println("$userCount user(s) live(s) in $cityName")
            } else {
                println("Nobody lives in $cityName")
            }
        }

        drop (Users, Cities)

    }
}

```

Outputs:

    SQL: CREATE TABLE IF NOT EXISTS Cities (id INT AUTO_INCREMENT PRIMARY KEY NOT NULL, name VARCHAR(50) NOT NULL)
    SQL: CREATE TABLE IF NOT EXISTS Users (id VARCHAR(10) PRIMARY KEY NOT NULL, name VARCHAR(50) NOT NULL, city_id INT NULL)
    SQL: ALTER TABLE Users ADD FOREIGN KEY (city_id) REFERENCES Cities(id)
    SQL: INSERT INTO Cities (name) VALUES ('St. Petersburg')
    SQL: INSERT INTO Cities (name) VALUES ('Munich')
    SQL: INSERT INTO Cities (name) VALUES ('Prague')
    SQL: INSERT INTO Users (id, name, city_id) VALUES ('andrey', 'Andrey', 1)
    SQL: INSERT INTO Users (id, name, city_id) VALUES ('sergey', 'Sergey', 2)
    SQL: INSERT INTO Users (id, name, city_id) VALUES ('eugene', 'Eugene', 2)
    SQL: INSERT INTO Users (id, name, city_id) VALUES ('alex', 'Alex', NULL)
    SQL: INSERT INTO Users (id, name, city_id) VALUES ('smth', 'Something', NULL)
    SQL: UPDATE Users SET name='Alexey' WHERE Users.id = 'alex'
    SQL: DELETE FROM Users WHERE Users.name LIKE '%thing'
    All cities:
    SQL: SELECT Cities.id, Cities.name FROM Cities
    1: St. Petersburg
    2: Munich
    3: Prague
    Manual join:
    SQL: SELECT Users.name, Cities.name FROM Users INNER JOIN Cities ON Cities.id = Users.city_id WHERE ((Users.id = 'andrey') or (Users.name = 'Sergey')) and Users.id = 'sergey' and Users.city_id = Cities.id
    Sergey lives in Munich
    Join with foreign key:
    SQL: SELECT Users.name, Users.city_id, Cities.name FROM Users INNER JOIN Cities ON Cities.id = Users.city_id WHERE (Cities.name = 'St. Petersburg') or (Users.city_id IS NULL)
    Andrey lives in St. Petersburg
    Functions and group by:
    SQL: SELECT Cities.name, COUNT(Users.id) FROM Cities INNER JOIN Users ON Cities.id = Users.city_id GROUP BY Cities.name
    1 user(s) live(s) in St. Petersburg
    2 user(s) live(s) in Munich
    SQL: DROP TABLE Users
    SQL: DROP TABLE Cities

## DAO sample
```kotlin
object Users : IdTable() {
    val name = varchar("name", 50).index()
    val city = reference("city", Cities)
    val age = integer("age")
}

object Cities: IdTable() {
    val name = varchar("name", 50)
}

class User(id: EntityID) : Entity(id) {
    companion object : EntityClass<User>(Users)

    var name by Users.name
    var city by City referencedOn Users.city
    var age by Users.age
}

class City(id: EntityID) : Entity(id) {
    companion object : EntityClass<City>(Cities)

    var name by Cities.name
    val users by User referrersOn Users.city
}

fun main(args: Array<String>) {
    val db = Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")

    db.transaction {
        logger.addLogger(StdOutSqlLogger())

        create (Cities, Users)

        val stPete = City.new {
            name = "St. Petersburg"
        }

        val munich = City.new {
            name = "Munich"
        }

        User.new {
            name = "a"
            city = stPete
            age = 5
        }

        User.new {
            name = "b"
            city = stPete
            age = 27
        }

        User.new {
            name = "c"
            city = munich
            age = 42
        }

        println("Cities: ${City.all().joinToString {it.name}}")
        println("Users in ${stPete.name}: ${stPete.users.joinToString {it.name}}")
        println("Adults: ${User.find { Users.age greaterEq 18 }.joinToString {it.name}}")
    }
}
```

Outputs:
    SQL: CREATE TABLE IF NOT EXISTS Cities (id INT AUTO_INCREMENT PRIMARY KEY NOT NULL, name VARCHAR(50) NOT NULL)
    SQL: CREATE TABLE IF NOT EXISTS Users (id INT AUTO_INCREMENT PRIMARY KEY NOT NULL, name VARCHAR(50) NOT NULL, city INT NOT NULL, age INT NOT NULL)
    SQL: CREATE INDEX Users_name ON Users (name)
    SQL: ALTER TABLE Users ADD FOREIGN KEY (city) REFERENCES Cities(id)
    SQL: INSERT INTO Cities (name) VALUES ('St. Petersburg'),('Munich')
    SQL: SELECT Cities.id, Cities.name FROM Cities
    Cities: St. Petersburg, Munich
    SQL: INSERT INTO Users (name, city, age) VALUES ('a', 1, 5),('b', 1, 27),('c', 2, 42)
    SQL: SELECT Users.id, Users.name, Users.city, Users.age FROM Users WHERE Users.city = 1
    Users in St. Petersburg: a, b
    SQL: SELECT Users.id, Users.name, Users.city, Users.age FROM Users WHERE Users.age >= 18
    Adults: b, c

