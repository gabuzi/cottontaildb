package org.vitrivr.cottontail.database.queries.planning.nodes.basics

import org.vitrivr.cottontail.database.entity.Entity
import org.vitrivr.cottontail.database.queries.planning.QueryPlannerContext
import org.vitrivr.cottontail.database.queries.planning.basics.AbstractNodeExpression
import org.vitrivr.cottontail.database.queries.planning.basics.NodeExpression
import org.vitrivr.cottontail.database.queries.planning.cost.Cost
import org.vitrivr.cottontail.database.queries.planning.cost.Costs
import org.vitrivr.cottontail.execution.tasks.basics.ExecutionStage
import org.vitrivr.cottontail.execution.tasks.entity.fetch.EntityFetchColumnsTask
import org.vitrivr.cottontail.execution.tasks.recordset.projection.*
import org.vitrivr.cottontail.model.basics.ColumnDef
import org.vitrivr.cottontail.model.exceptions.QueryException
import org.vitrivr.cottontail.utilities.name.Name

/**
 * Formalizes a [ProjectionNodeExpression] operation in the Cottontail DB query execution engine.
 *
 * @author Ralph Gasser
 * @version 1.1
 */
data class ProjectionNodeExpression(val type: ProjectionType = ProjectionType.SELECT, val entity: Entity, val columns: Array<ColumnDef<*>>, val fields: Map<Name, Name?>) : AbstractNodeExpression() {

    /**
     * The type of [ProjectionNodeExpression]
     */
    enum class ProjectionType {
        SELECT, SELECT_DISTINCT, COUNT, COUNT_DISTINCT, EXISTS, SUM, MAX, MIN, MEAN
    }

    init {
        /* Sanity check. */
        when (type) {
            ProjectionType.SELECT -> if (columns.isEmpty()) {
                throw QueryException.QuerySyntaxException("Projection of type $type must specify at least one column.")
            }
            ProjectionType.SELECT_DISTINCT -> if (columns.isEmpty()) {
                throw QueryException.QuerySyntaxException("Projection of type $type must specify at least one column.")
            }
            ProjectionType.MAX -> if (columns.isEmpty()) {
                throw QueryException.QuerySyntaxException("Projection of type $type must specify a column.")
            } else if (!columns.first().type.numeric) {
                throw QueryException.QueryBindException("Projection of type $type can only be applied on a numeric column, which ${columns.first().name} is not.")
            }
            ProjectionType.MIN -> if (columns.isEmpty()) {
                throw QueryException.QuerySyntaxException("Projection of type $type must specify a column.")
            } else if (!columns.first().type.numeric) {
                throw QueryException.QueryBindException("Projection of type $type can only be applied to a numeric column, which ${columns.first().name} is not.")
            }
            ProjectionType.SUM -> if (columns.isEmpty()) {
                throw QueryException.QuerySyntaxException("Projection of type $type must specify a column.")
            } else if (!columns.first().type.numeric) {
                throw QueryException.QueryBindException("Projection of type $type can only be applied to a numeric column, which ${columns.first().name} is not.")
            }
            ProjectionType.MEAN -> if (columns.isEmpty()) {
                throw QueryException.QuerySyntaxException("Projection of type $type must specify a column.")
            } else if (!columns.first().type.numeric) {
                throw QueryException.QueryBindException("Projection of type $type can only be applied to a numeric column, which ${columns.first().name} is not.")
            }
            else -> {}
        }
    }

    override val output: Long
        get() = this.parents.firstOrNull()?.output ?: 0L

    override val cost: Cost
        get() = Cost(
            this.output * this.columns.size * Costs.DISK_ACCESS_READ,
            this.output * this.fields.size * Costs.MEMORY_ACCESS_READ,
            (this.output * this.columns.map { it.physicalSize }.sum()).toFloat()
        )

    override fun copy(): NodeExpression = ProjectionNodeExpression(this.type, this.entity, this.columns, this.fields)

    override fun toStage(context: QueryPlannerContext): ExecutionStage = when (this.type) {
        ProjectionType.SELECT -> {
            val fetch = ExecutionStage(ExecutionStage.MergeType.ONE, this.parents.first().toStage(context)).addTask(EntityFetchColumnsTask(this.entity, this.columns))
            ExecutionStage(ExecutionStage.MergeType.ONE, fetch).addTask(RecordsetSelectProjectionTask(this.fields))
        }
        ProjectionType.SELECT_DISTINCT -> {
            val fetch = ExecutionStage(ExecutionStage.MergeType.ONE, this.parents.first().toStage(context)).addTask(EntityFetchColumnsTask(this.entity, this.columns))
            val project = ExecutionStage(ExecutionStage.MergeType.ONE, fetch).addTask(RecordsetSelectProjectionTask(this.fields))
            ExecutionStage(ExecutionStage.MergeType.ONE, project).addTask(RecordsetDistinctTask())
        }
        ProjectionType.COUNT -> {
            ExecutionStage(ExecutionStage.MergeType.ONE, this.parents.first().toStage(context)).addTask(RecordsetCountProjectionTask())
        }
        ProjectionType.COUNT_DISTINCT -> {
            val fetch = ExecutionStage(ExecutionStage.MergeType.ONE, this.parents.first().toStage(context)).addTask(EntityFetchColumnsTask(this.entity, this.columns))
            val distinct = ExecutionStage(ExecutionStage.MergeType.ONE, fetch).addTask(RecordsetDistinctTask())
            ExecutionStage(ExecutionStage.MergeType.ONE, distinct).addTask(RecordsetCountProjectionTask())
        }
        ProjectionType.SUM -> {
            val fetch = ExecutionStage(ExecutionStage.MergeType.ONE, this.parents.first().toStage(context)).addTask(EntityFetchColumnsTask(this.entity, this.columns))
            ExecutionStage(ExecutionStage.MergeType.ONE, fetch).addTask(RecordsetSumProjectionTask(this.columns, this.fields))
        }
        ProjectionType.MAX -> {
            val fetch = ExecutionStage(ExecutionStage.MergeType.ONE, this.parents.first().toStage(context)).addTask(EntityFetchColumnsTask(this.entity, this.columns))
            ExecutionStage(ExecutionStage.MergeType.ONE, fetch).addTask(RecordsetMaxProjectionTask(this.columns, this.fields))
        }
        ProjectionType.MIN -> {
            val fetch = ExecutionStage(ExecutionStage.MergeType.ONE, this.parents.first().toStage(context)).addTask(EntityFetchColumnsTask(this.entity, this.columns))
            ExecutionStage(ExecutionStage.MergeType.ONE, fetch).addTask(RecordsetMinProjectionTask(this.columns, this.fields))
        }
        ProjectionType.MEAN -> {
            val fetch = ExecutionStage(ExecutionStage.MergeType.ONE, this.parents.first().toStage(context)).addTask(EntityFetchColumnsTask(entity, this.columns))
            ExecutionStage(ExecutionStage.MergeType.ONE, fetch).addTask(RecordsetMeanProjectionTask(this.columns, this.fields))
        }
        ProjectionType.EXISTS -> ExecutionStage(ExecutionStage.MergeType.ONE, this.parents.first().toStage(context)).addTask(RecordsetExistsProjectionTask())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProjectionNodeExpression

        if (type != other.type) return false
        if (!columns.contentEquals(other.columns)) return false
        if (fields != other.fields) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + columns.contentHashCode()
        result = 31 * result + fields.hashCode()
        return result
    }
}