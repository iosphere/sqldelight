/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.types

import com.google.common.truth.Subject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.resolution.query.QueryResults
import com.squareup.sqldelight.resolution.query.Table
import com.squareup.sqldelight.resolution.query.Value
import com.squareup.sqldelight.util.parse
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.junit.Test
import java.io.File

class ExtensionsTests {
  private val testFile = File("src/test/data/ExtensionTestData.sq")
  private val parsed = parse(testFile)
  private val symbolTable = SymbolTable() + SymbolTable(parsed, testFile, testFile.path)
  private val status = SqlDelightValidator()
      .validate(testFile.path, parsed, symbolTable) as Status.ValidationStatus.Validated

  @Test
  fun selectTest() {
    assertThat(status.queries.withName("select_all"))
        .hasColumn("geometry", SqliteType.BLOB, 0)
        .hasSize(0, 1)
  }

  private fun assertThat(queryResults: QueryResults) = QueryResultsSubject(queryResults)

  private class QueryResultsSubject(
      val queryResults: QueryResults
  ): Subject<QueryResultsSubject, QueryResults>(Truth.THROW_ASSERTION_ERROR, queryResults) {
    fun hasTable(tableName: String, vararg indices: Int): QueryResultsSubject {
      var index = 0
      val (tableIndex, table) = queryResults.results
          .map { result ->
            val pair = index to result
            index += result.size()
            return@map pair
          }
          .filter { it.second is Table && it.second.name == tableName }
          .first()
      assertThat(table).isNotNull()
      tableIndex.rangeTo(tableIndex + table.size() - 1).forEachIndexed { index, valueIndex ->
        assertThat(indices[index]).isEqualTo(valueIndex)
      }
      return this
    }

    fun hasColumn(columnName: String, type: SqliteType, index: Int): QueryResultsSubject {
      var currentIndex = 0
      val (columnIndex, column) = queryResults.results
          .map { result ->
            val pair = currentIndex to result
            currentIndex += result.size()
            return@map pair
          }
          .filter { it.second is Value && it.second.name == columnName }
          .first()
      assertThat(column).isNotNull()
      assertThat(column.javaType).isEqualTo(type.defaultType)
      assertThat(columnIndex).isEqualTo(index)
      return this
    }

    fun hasSize(tableSize: Int, columnSize: Int): QueryResultsSubject {
      assertThat(queryResults.results.filterIsInstance<Table>().size).isEqualTo(tableSize)
      assertThat(queryResults.results.filterIsInstance<Value>().size).isEqualTo(columnSize)
      return this
    }
  }

  private fun List<QueryResults>.withName(name: String) = first { it.name == name }
}
