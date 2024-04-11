package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParser
import ch.pontius.kiar.ingester.processors.ProcessingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption


/**
 * A [Source] for a single Microsoft Excel file. This is, for example, used by museumPlus import.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ExcelFileSource(private val file: Path, private val config: EntityMapping, private val deleteFileWhenDone: Boolean = true): Source<SolrInputDocument> {
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = flow {
        Files.newInputStream(this@ExcelFileSource.file, StandardOpenOption.READ).use { input ->
            val workbook: Workbook = XSSFWorkbook(input)
            val sheet: Sheet = workbook.getSheetAt(0)
            val map = mutableListOf<Pair<ValueParser<*>,Int>>()
            var first = true
            for (row in sheet) {
                if (first) {
                    for (attribute in this@ExcelFileSource.config.attributes) {
                        val index = row.indexOfFirst { it.stringCellValue == attribute.source }
                        if (index > -1) {
                            map.add(attribute.newParser() to index)
                        } else if (attribute.required) {
                            throw IllegalStateException("Row with name '${attribute.source}' is missing.")
                        }
                    }
                    first = false
                } else {
                    val doc = SolrInputDocument()
                    for ((parser, cellIndex) in map) {
                        val cell = row.getCell(cellIndex)
                        parser.parse(when (cell.cellType) {
                            CellType.STRING -> cell.stringCellValue
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            else -> null /* TODO: Can formulas be parsed? */
                        }?.trim() ?: continue, doc)
                    }

                    /* Emit document. */
                    emit(doc)
                }
            }
        }
    }.onCompletion {
        if (deleteFileWhenDone) {
            Files.deleteIfExists(this@ExcelFileSource.file)
        }
    }.flowOn(Dispatchers.IO)
}