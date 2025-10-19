package com.paklog.wes.task.infrastructure.config;

import com.paklog.wes.task.domain.aggregate.WorkTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoConfigTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IndexOperations indexOperations;

    @Test
    void initIndexesRegistersAllRequiredIndexes() {
        when(mongoTemplate.indexOps(WorkTask.class)).thenReturn(indexOperations);

        MongoConfig config = new MongoConfig(mongoTemplate);
        config.initIndexes();

        verify(indexOperations, org.mockito.Mockito.times(9)).ensureIndex(any(Index.class));
    }
}
