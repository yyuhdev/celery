package de.yyuh.celery.platform.mongodb.query;

import com.mongodb.client.model.Filters;
import de.yyuh.celery.api.query.IQuery;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BsonQueryBuilder {

    private BsonQueryBuilder() {
    }

    @NotNull
    public static Bson buildFilter(final @NotNull IQuery<?> query) {
        final Map<String, Object> filters = query.filters();
        if (filters.isEmpty()) {
            return Filters.empty();
        }

        final List<Bson> bsonFilters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            bsonFilters.add(Filters.eq(entry.getKey(), entry.getValue()));
        }

        if (bsonFilters.size() == 1) {
            return bsonFilters.get(0);
        }

        return Filters.and(bsonFilters);
    }
}
