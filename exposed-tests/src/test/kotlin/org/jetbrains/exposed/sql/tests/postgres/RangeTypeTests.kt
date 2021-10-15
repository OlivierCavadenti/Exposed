package org.jetbrains.exposed.sql.tests.postgres

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.tests.DatabaseTestsBase
import org.jetbrains.exposed.sql.tests.TestDB
import org.jetbrains.exposed.sql.tests.h2.H2Tests
import org.jetbrains.exposed.sql.tests.shared.assertEquals
import org.junit.Test

class RangeTypeTests : DatabaseTestsBase() {

    @Test
    fun insertInH2() {
        withDb(TestDB.POSTGRESQL) {
            SchemaUtils.drop(Int4RangeTable)
            SchemaUtils.create(Int4RangeTable)
            Int4RangeTable.insert {
                it[intValues] = (1..20)
            }
            assertEquals((1..20), Int4RangeTable.select {
                Int4RangeTable.intValues.eq(1..20)
            }.single()[Int4RangeTable.intValues])
        }
    }

    @Test
    fun insertInRange() {
        withDb(TestDB.POSTGRESQL) {
            SchemaUtils.drop(Int4RangeTable)
            SchemaUtils.create(Int4RangeTable)
            Int4RangeTable.insert {
                it[intValues] = (1..20)
            }
            assertEquals((1..20), Int4RangeTable.select {
                Int4RangeTable.intValues.overlaps((3..35))
            }.single()[Int4RangeTable.intValues])

            assertEquals((1..20), Int4RangeTable.select {
                Int4RangeTable.intValues.include(6)
            }.single()[Int4RangeTable.intValues])
        }
    }

    @Test
    fun insertInRange3() {
        withDb(TestDB.POSTGRESQL) {
            SchemaUtils.drop(Int4RangeTable)
            SchemaUtils.create(Int4RangeTable)
            Int4RangeTable.insert {
                it[intValues] = (5..20)
            }
            val column = Int4RangeTable.intValues.lower()
            assertEquals(5, Int4RangeTable.slice(column).selectAll().single()[column])
        }
    }

    object Int4RangeTable : Table("Int4RangeTable") {
        val intValues = int4range("int_values")
    }
}
