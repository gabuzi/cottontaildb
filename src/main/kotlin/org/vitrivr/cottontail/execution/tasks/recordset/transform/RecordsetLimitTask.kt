package org.vitrivr.cottontail.execution.tasks.recordset.transform

import com.github.dexecutor.core.task.Task
import com.github.dexecutor.core.task.TaskExecutionException
import org.vitrivr.cottontail.execution.tasks.basics.ExecutionTask
import org.vitrivr.cottontail.model.recordset.Recordset

/**
 * A [Task] used during query execution. It takes a [Recordset] as input, skips <i>skip</i> rows and then copies a maximum of <i>limit</i>
 * rows o a new [Recordset], which is then returned. Produces a n x limit [Recordset] at max.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class RecordsetLimitTask(val limit: Long, val skip: Long = 0) : ExecutionTask("RecordsetLimitTask") {
    override fun execute(): Recordset {
        assertUnaryInput()

        /* Get records from parent task. */
        val parent = this.first()
                ?: throw TaskExecutionException("LIMIT could not be executed because parent task has failed.")

        /* Calculate actual bounds for LIMIT and SKIP. */
        val actualSkip = this.skip.coerceAtLeast(0L)
        val actualLimit = this.limit.coerceAtLeast(0L).coerceAtMost(parent.rowCount - actualSkip)

        /* Limit the recordset. */
        val recordset = Recordset(parent.columns)
        parent.forEachIndexed { i, r ->
            if (i in actualSkip until actualSkip + actualLimit) {
                recordset.addRowUnsafe(r.tupleId, r.values)
            } else if (i > actualLimit) {
                return@forEachIndexed
            }
        }

        /* Make assertion and return recordset. */
        assert(recordset.rowCount <= limit)
        return recordset
    }
}