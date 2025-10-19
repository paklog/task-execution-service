package com.paklog.wes.task.infrastructure.config;

import com.paklog.wes.task.domain.aggregate.WorkTask;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * MongoDB configuration and index creation
 */
@Configuration
public class MongoConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        logger.info("Creating MongoDB indexes for WorkTask collection");

        IndexOperations indexOps = mongoTemplate.indexOps(WorkTask.class);

        // Index for querying by status
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .named("idx_status"));

        // Compound index for warehouse and status queries
        indexOps.ensureIndex(new Index()
                .on("warehouseId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_warehouse_status"));

        // Compound index for zone and status queries
        indexOps.ensureIndex(new Index()
                .on("zone", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_zone_status"));

        // Compound index for assigned worker queries
        indexOps.ensureIndex(new Index()
                .on("assignedTo", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_assigned_status"));

        // Index for overdue task queries
        indexOps.ensureIndex(new Index()
                .on("deadline", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_deadline_status"));

        // Index for reference ID (wave, order, etc.) queries
        indexOps.ensureIndex(new Index()
                .on("referenceId", Sort.Direction.ASC)
                .named("idx_reference"));

        // Compound index for type and status queries
        indexOps.ensureIndex(new Index()
                .on("type", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_type_status"));

        // Index for created timestamp (useful for reporting)
        indexOps.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_created_at"));

        // Compound index for worker performance queries
        indexOps.ensureIndex(new Index()
                .on("assignedTo", Sort.Direction.ASC)
                .on("completedAt", Sort.Direction.DESC)
                .named("idx_worker_completed"));

        logger.info("MongoDB indexes created successfully");
    }
}
