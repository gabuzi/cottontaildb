package ch.unibas.dmi.dbis.cottontail.server.grpc.services

import ch.unibas.dmi.dbis.cottontail.database.catalogue.Catalogue
import ch.unibas.dmi.dbis.cottontail.database.column.ColumnDef
import ch.unibas.dmi.dbis.cottontail.database.column.ColumnType
import ch.unibas.dmi.dbis.cottontail.grpc.CottonDDLGrpc
import ch.unibas.dmi.dbis.cottontail.grpc.CottontailGrpc
import ch.unibas.dmi.dbis.cottontail.model.exceptions.DatabaseException
import ch.unibas.dmi.dbis.cottontail.server.grpc.helper.fqn

import com.google.protobuf.Empty

import io.grpc.Status
import io.grpc.stub.StreamObserver

/**
 * This is a GRPC service endpoint that handles DDL (=Data Definition Language) request for Cottontail DB.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
internal class CottonDDLService (val catalogue: Catalogue): CottonDDLGrpc.CottonDDLImplBase() {
    /**
     * GRPC endpoint for creating a new [Schema]
     */
    override fun createSchema(request: CottontailGrpc.Schema, responseObserver: StreamObserver<Empty>) = try {
        this.catalogue.createSchema(request.name)
        responseObserver.onCompleted()
    } catch (e: DatabaseException.SchemaAlreadyExistsException) {
        responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Schema '${request.name} cannot be created because it already exists!").asException())
    } catch (e: DatabaseException) {
        responseObserver.onError(Status.UNKNOWN.withDescription("Failed to create schema '${request.name} because of database error: ${e.message}").asException())
    }

    /**
     * GRPC endpoint for dropping a [Schema]
     */
    override fun dropSchema(request: CottontailGrpc.Schema, responseObserver: StreamObserver<Empty>) = try {
        this.catalogue.dropSchema(request.name)
        responseObserver.onCompleted()
    } catch (e: DatabaseException.SchemaDoesNotExistException) {
        responseObserver.onError(Status.NOT_FOUND.withDescription("Schema '${request.name} does not exist!").asException())
    } catch (e: DatabaseException) {
        responseObserver.onError(Status.UNKNOWN.withDescription("Failed to drop schema '${request.name} because of database error: ${e.message}").asException())
    }

    /**
     * GRPC endpoint listing the available [Schema]s.
     */
    override fun listSchemas(request: Empty?, responseObserver: StreamObserver<CottontailGrpc.Schema>) = try {
        this.catalogue.listSchemas().forEach {
            responseObserver.onNext(CottontailGrpc.Schema.newBuilder().setName(it).build())
        }
        responseObserver.onCompleted()
    } catch (e: DatabaseException) {
        responseObserver.onError(Status.UNKNOWN.withDescription("Failed to list schemas because of database error: ${e.message}").asException())
    }
    /**
     *
     * GRPC endpoint for creating a new [Entity]
     */
    override fun createEntity(request: CottontailGrpc.CreateEntityMessage, responseObserver: StreamObserver<Empty>) = try {
        val schema = this.catalogue.getSchema(request.entity.schema.name)
        val columns = request.columnsList.map {
            val type = ColumnType.forName(it.type.name)
            ColumnDef(it.name, type, it.length, it.nullable)
        }
        schema.createEntity(request.entity.name, *columns.toTypedArray())
        responseObserver.onCompleted()
    } catch (e: DatabaseException.SchemaDoesNotExistException) {
        responseObserver.onError(Status.NOT_FOUND.withDescription("Schema '${request.entity.schema.name} does not exist!").asException())
    } catch (e: DatabaseException.EntityAlreadyExistsException) {
        responseObserver.onError(Status.NOT_FOUND.withDescription("Entity '${request.entity.fqn()} does already exist!").asException())
    } catch (e: DatabaseException) {
        responseObserver.onError(Status.UNKNOWN.withDescription("Failed to create entity '${request.entity.fqn()}' because of database error: ${e.message}").asException())
    }

    /**
     * GRPC endpoint for dropping a particular [Schema]
     */
    override fun dropEntity(request: CottontailGrpc.Entity, responseObserver: StreamObserver<Empty>) = try {
        this.catalogue.getSchema(request.schema.name).dropEntity(request.name)
        responseObserver.onCompleted()
    } catch (e: DatabaseException.SchemaDoesNotExistException) {
        responseObserver.onError(Status.NOT_FOUND.withDescription("Schema '${request.schema.name} does not exist!").asException())
    } catch (e: DatabaseException.EntityDoesNotExistException) {
        responseObserver.onError(Status.NOT_FOUND.withDescription("Entity '${request.fqn()} does not exist!").asException())
    } catch (e: DatabaseException) {
        responseObserver.onError(Status.UNKNOWN.withDescription("Failed to drop entity '${request.fqn()}' because of database error: ${e.message}").asException())
    }

    /**
     * GRPC endpoint listing the available [Entity]s for the provided [Schema].
     */
    override fun listEntities(request: CottontailGrpc.Schema, responseObserver: StreamObserver<CottontailGrpc.Entity>) = try {
        if (request.name == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("You must provide a valid schema in order to list entities.").asException())
        } else {
            val builder = CottontailGrpc.Entity.newBuilder()
            this.catalogue.getSchema(request.name).entities.forEach {
                responseObserver.onNext(builder.setName(it).setSchema(request).build())
            }
            responseObserver.onCompleted()
        }
    } catch (e: DatabaseException.SchemaDoesNotExistException) {
        responseObserver.onError(Status.NOT_FOUND.withDescription("Schema '${request.name} does not exist!").asException())
    } catch (e: DatabaseException) {
        responseObserver.onError(Status.UNKNOWN.withDescription("Failed to list entities for schema ${request.name} because of database error: ${e.message}").asException())
    }
}