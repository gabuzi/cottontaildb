package org.vitrivr.cottontail.server.grpc.services

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.vitrivr.cottontail.database.catalogue.Catalogue
import org.vitrivr.cottontail.execution.ExecutionEngine
import org.vitrivr.cottontail.execution.tasks.ExecutionPlanException
import org.vitrivr.cottontail.grpc.CottonDQLGrpc
import org.vitrivr.cottontail.grpc.CottontailGrpc
import org.vitrivr.cottontail.model.basics.Record
import org.vitrivr.cottontail.model.exceptions.DatabaseException
import org.vitrivr.cottontail.model.exceptions.QueryException
import org.vitrivr.cottontail.model.recordset.Recordset
import org.vitrivr.cottontail.server.grpc.helper.DataHelper
import org.vitrivr.cottontail.server.grpc.helper.GrpcQueryBinder
import org.vitrivr.cottontail.utilities.math.BitUtil
import java.util.*

class CottonDQLService(val catalogue: Catalogue, val engine: ExecutionEngine, val maxMessageSize: Int) : CottonDQLGrpc.CottonDQLImplBase() {
    /** Logger used for logging the output. */
    companion object {
        private val LOGGER = LoggerFactory.getLogger(CottonDQLService::class.java)
    }

    /**
     *  gRPC endpoint for handling simple queries.
     */
    override fun query(request: CottontailGrpc.QueryMessage, responseObserver: StreamObserver<CottontailGrpc.QueryResponseMessage>) = try {
        /* Start the query by giving the start signal. */
        val queryId = if (request.queryId == null || request.queryId == "") {
            UUID.randomUUID().toString()
        } else {
            request.queryId
        }

        /* Bind query and generate execution plan */
        val startBinding = System.currentTimeMillis()
        val binder = GrpcQueryBinder(catalogue = this.catalogue, engine = this.engine)
        val plan = binder.parseAndBind(request.query)
        LOGGER.trace("Parsing & binding query $queryId took ${System.currentTimeMillis() - startBinding}ms.")

        /* Execute query. */
        val startExecution = System.currentTimeMillis()
        val results = plan.execute()
        LOGGER.trace("Executing query $queryId took ${System.currentTimeMillis() - startExecution}ms.")

        /* Send back results. */
        this.spoolResults(queryId, results, responseObserver)

        /* Complete query. */
        LOGGER.info("Query $queryId took ${System.currentTimeMillis() - startBinding}ms.")
        responseObserver.onCompleted()
    } catch (e: QueryException.QuerySyntaxException) {
        LOGGER.error("Error while executing query $request", e)
        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Query syntax is invalid: ${e.message}").asException())
    } catch (e: QueryException.QueryBindException) {
        LOGGER.error("Error while executing query $request", e)
        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Query binding failed: ${e.message}").asException())
    } catch (e: ExecutionPlanException) {
        LOGGER.error("Error while executing query $request", e)
        responseObserver.onError(Status.INTERNAL.withDescription("Query execution failed because execution engine signaled an error: ${e.message}").asException())
    } catch (e: DatabaseException) {
        LOGGER.error("Error while executing query $request", e)
        responseObserver.onError(Status.INTERNAL.withDescription("Query execution failed because of a database error: ${e.message}").asException())
    } catch (e: Throwable) {
        LOGGER.error("Error while executing query $request", e)
        responseObserver.onError(Status.UNKNOWN.withDescription("Query execution failed failed because of a unknown error: ${e.message}").asException())
    }

    /**
     *  gRPC endpoint for handling batched queries.
     */
    override fun batchedQuery(request: CottontailGrpc.BatchedQueryMessage, responseObserver: StreamObserver<CottontailGrpc.QueryResponseMessage>) = try {
        /* Start the query by giving the start signal. */
        val start = System.currentTimeMillis()
        val queryId = UUID.randomUUID().toString()

        request.queriesList.forEachIndexed { index, query ->
            /* Bind query and generate execution plan */
            val startBinding = System.currentTimeMillis()
            val binder = GrpcQueryBinder(catalogue = this@CottonDQLService.catalogue, engine = this@CottonDQLService.engine)
            val plan = binder.parseAndBind(query)
            LOGGER.trace("Parsing & binding query $index of batch $queryId took ${System.currentTimeMillis() - startBinding}ms.")

            /* Execute query. */
            val startExecution = System.currentTimeMillis()
            val results = plan.execute()
            LOGGER.trace("Executing query $index of batch $queryId took ${System.currentTimeMillis() - startExecution}ms.")

            /* Send back results. */
            this.spoolResults(queryId, results, responseObserver, index)
        }

        /* Complete query. */
        LOGGER.info("Batched query $queryId took ${System.currentTimeMillis() - start}ms to complete.")
        responseObserver.onCompleted()
    } catch (e: QueryException.QuerySyntaxException) {
        LOGGER.error("Error while executing batched query $request", e)
        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Query syntax is invalid: ${e.message}").asException())
    } catch (e: QueryException.QueryBindException) {
        LOGGER.error("Error while executing batched query $request", e)
        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Query binding failed: ${e.message}").asException())
    } catch (e: ExecutionPlanException) {
        LOGGER.error("Error while executing batched query $request", e)
        responseObserver.onError(Status.INTERNAL.withDescription("Query execution failed because execution engine signaled an error: ${e.message}").asException())
    } catch (e: DatabaseException) {
        LOGGER.error("Error while executing batched query $request", e)
        responseObserver.onError(Status.INTERNAL.withDescription("Query execution failed because of a database error: ${e.message}").asException())
    } catch (e: Throwable) {
        LOGGER.error("Error while executing batched query $request", e)
        responseObserver.onError(Status.UNKNOWN.withDescription("Query execution failed failed because of a unknown error: ${e.message}").asException())
    }

    /**
     * gRPC endpoint for handling PING requests.
     */
    override fun ping(request: CottontailGrpc.Empty, responseObserver: StreamObserver<CottontailGrpc.SuccessStatus>) {
        responseObserver.onNext(CottontailGrpc.SuccessStatus.newBuilder().setTimestamp(System.currentTimeMillis()).build())
        responseObserver.onCompleted()
    }

    /**
     * Batches and spools the results in the given [Recordset]
     *
     * @param queryId The ID of the query.
     * @param results [Recordset] containing the results.
     * @param responseObserver [StreamObserver] used to send back the results.
     * @param index Optional index of the result (for batched queries).
     */
    private fun spoolResults(queryId: String, results: Recordset, responseObserver: StreamObserver<CottontailGrpc.QueryResponseMessage>, index: Int = 0) {
        if (results.rowCount > 0) {
            val startSending = System.currentTimeMillis()
            val first = results.first()
            if (first != null) {
                val exampleSize = BitUtil.nextPowerOfTwo(recordToTuple(first).build().serializedSize)
                val pageSize = (this.maxMessageSize / exampleSize)
                val maxPages = Math.floorDiv(results.rowCount, pageSize).toInt()

                /* Return results. */
                val iterator = results.iterator()
                for (i in 0..maxPages) {
                    val responseBuilder = CottontailGrpc.QueryResponseMessage.newBuilder().setStart(i == 0).setPageSize(pageSize).setPage(i).setMaxPage(maxPages).setTotalHits(results.rowCount.toInt()) /* TODO: Make necessary values in Proto Definition Longs. */
                    for (j in i * pageSize until kotlin.math.min(results.rowCount, (i * pageSize + pageSize).toLong())) {
                        responseBuilder.addResults(recordToTuple(iterator.next()))
                    }
                    responseObserver.onNext(responseBuilder.build())
                }
            }
            LOGGER.trace("Sending back ${results.rowCount} rows for position $index of query $queryId took ${System.currentTimeMillis() - startSending}ms.")
        } else {
            responseObserver.onNext(CottontailGrpc.QueryResponseMessage.newBuilder().setStart(false).setPageSize(0).setPage(0).setMaxPage(0).setTotalHits(0).build())
            LOGGER.trace("Position $index of query $queryId yielded no results.")
        }
    }

    /**
     * Generates a new [CottontailGrpc.Tuple.Builder] from a given [Record].
     *
     * @param record [Record] to create a [CottontailGrpc.Tuple.Builder] from.
     * @return Resulting [CottontailGrpc.Tuple.Builder]
     */
    private fun recordToTuple(record: Record): CottontailGrpc.Tuple.Builder = CottontailGrpc.Tuple.newBuilder().putAllData(record.toMap().map { it.key.name to DataHelper.toData(it.value) }.toMap())
}
