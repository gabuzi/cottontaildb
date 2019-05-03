package ch.unibas.dmi.dbis.cottontail.execution.tasks.entity.projection

import ch.unibas.dmi.dbis.cottontail.database.entity.Entity
import ch.unibas.dmi.dbis.cottontail.database.general.query
import ch.unibas.dmi.dbis.cottontail.execution.tasks.basics.ExecutionTask
import ch.unibas.dmi.dbis.cottontail.model.basics.ColumnDef
import ch.unibas.dmi.dbis.cottontail.model.recordset.Recordset
import ch.unibas.dmi.dbis.cottontail.model.values.BooleanValue

import com.github.dexecutor.core.task.Task

/**
 * A [Task] used during query execution. It takes a single [Entity] and checks if it contains any entries.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
internal class EntityExistsProjectionTask(val entity: Entity): ExecutionTask("EntityExistsProjectionTask[${entity.name}]") {

    /** The cost of this [EntityExistsProjectionTask] is constant */
    override val cost = 0.25f

    /**
     * Executes this [EntityExistsProjectionTask]
     */
    override fun execute(): Recordset {
        assertNullaryInput()

        val column = arrayOf(ColumnDef.withAttributes("${entity.fqn}.exists(*)", "BOOLEAN"))
        return this.entity.Tx(true).query {
            val recordset = Recordset(column)
            recordset.addRowUnsafe(arrayOf(BooleanValue(it.count() > 0)))
            recordset
        } ?: Recordset(column)
    }
}