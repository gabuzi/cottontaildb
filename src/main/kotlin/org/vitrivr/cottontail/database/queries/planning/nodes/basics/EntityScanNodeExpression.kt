package org.vitrivr.cottontail.database.queries.planning.nodes.basics

import org.vitrivr.cottontail.database.entity.Entity
import org.vitrivr.cottontail.database.queries.planning.QueryPlannerContext
import org.vitrivr.cottontail.database.queries.planning.basics.AbstractNodeExpression
import org.vitrivr.cottontail.database.queries.planning.basics.NodeExpression
import org.vitrivr.cottontail.database.queries.planning.cost.Cost
import org.vitrivr.cottontail.database.queries.planning.cost.Costs
import org.vitrivr.cottontail.execution.tasks.basics.ExecutionStage
import org.vitrivr.cottontail.execution.tasks.entity.source.EntityLinearScanTask
import org.vitrivr.cottontail.execution.tasks.entity.source.EntitySampleTask
import org.vitrivr.cottontail.model.basics.ColumnDef

/**
 * Formalizes a scan of a physical [Entity] in Cottontail DB.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
sealed class EntityScanNodeExpression : AbstractNodeExpression() {
    /** An estimation of the cots required to perform this [EntityScanNodeExpression]. */
    abstract override val cost: Cost

    /** The [Entity] that is being scanned. */
    abstract val entity: Entity

    /** The [Entity] that is being scanned. */
    abstract val columns: Array<ColumnDef<*>>

    /**
     * Simple (full-)table scan.
     */
    data class FullEntityScanNodeExpression(override val entity: Entity, override val columns: Array<ColumnDef<*>> = entity.allColumns().toTypedArray()) : EntityScanNodeExpression() {
        override val output = this.entity.statistics.rows
        override val cost = Cost(this.entity.statistics.rows * this.columns.size * Costs.DISK_ACCESS_READ, 0.0f, (this.output * this.columns.map { it.physicalSize }.sum()).toFloat())
        override fun copy(): NodeExpression = FullEntityScanNodeExpression(this.entity, this.columns)
        override fun toStage(context: QueryPlannerContext): ExecutionStage {
            val stage = ExecutionStage(ExecutionStage.MergeType.ONE)
            stage.addTask(EntityLinearScanTask(this.entity, this.columns))
            return stage
        }
    }

    /**
     * Ranged table scan i.e. a scan that returns a certain range of the underlying [Entity]
     */
    data class RangedEntityScanNodeExpression(override val entity: Entity, override val columns: Array<ColumnDef<*>> = entity.allColumns().toTypedArray(), val start: Long, val end: Long) : EntityScanNodeExpression() {
        init {
            require(this.end > this.start) { "Start of a ranged entity scan cannot be greater than its end." }
            require(this.start > 0L) { "Start of a ranged entity scan must be greater than zero." }
        }

        override val output = this.end - this.start
        override val cost = Cost(this.output * this.columns.size * Costs.DISK_ACCESS_READ, 0.0f, (this.output * this.columns.map { it.physicalSize }.sum()).toFloat())
        override fun copy(): NodeExpression = RangedEntityScanNodeExpression(this.entity, this.columns, this.start, this.end)
        override fun toStage(context: QueryPlannerContext): ExecutionStage {
            val stage = ExecutionStage(ExecutionStage.MergeType.ONE)
            stage.addTask(EntityLinearScanTask(this.entity, this.columns, this.start, this.end))
            return stage
        }
    }

    /**
     * Sampled table scan i.e. as scan that returns a random subset of the [Entity].
     */
    data class SampledEntityScanNodeExpression(override val entity: Entity, override val columns: Array<ColumnDef<*>> = entity.allColumns().toTypedArray(), val size: Long, val seed: Long = System.currentTimeMillis()) : EntityScanNodeExpression() {
        init {
            require(size > 0) { "Sample size must be greater than zero for a sampled scan." }
        }

        override val output = this.size
        override val cost = Cost(this.size * this.columns.size * Costs.DISK_ACCESS_READ, 5 * this.size * Costs.MEMORY_ACCESS_READ, (this.output * this.columns.map { it.physicalSize }.sum()).toFloat())
        override fun copy(): NodeExpression = SampledEntityScanNodeExpression(this.entity, this.columns, this.size, this.seed)
        override fun toStage(context: QueryPlannerContext): ExecutionStage {
            val stage = ExecutionStage(ExecutionStage.MergeType.ONE)
            stage.addTask(EntitySampleTask(this.entity, this.columns, this.size, this.seed))
            return stage
        }
    }
}