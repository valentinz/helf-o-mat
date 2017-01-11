package de.helfenkannjeder.helfomat.service;

import de.helfenkannjeder.helfomat.domain.Answer;
import de.helfenkannjeder.helfomat.domain.BoundingBox;
import de.helfenkannjeder.helfomat.domain.GeoPoint;
import de.helfenkannjeder.helfomat.domain.Question;
import de.helfenkannjeder.helfomat.dto.ClusteredGeoPointDto;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoBoundingBoxQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoDistanceQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * @author Valentin Zickner
 */
// TODO: Split in multiple search services, one for organisations and one for questions
@Service
public class SearchService {

    private static final int DEFAULT_MAX_RESULT_SIZE = 10000;
    private Client client;
    private String index;
    private String type;

    @Autowired
    public SearchService(Client client,
                         @Value("${elasticsearch.index}") String index,
                         @Value("${elasticsearch.type.organisation}") String type) {
        this.client = client;
        this.index = index;
        this.type = type;
    }

    public List<Map<String, Object>> findOrganisation(List<Answer> answers,
                                                      GeoPoint position,
                                                      double distance) {
        BoolQueryBuilder boolQueryBuilder = boolQuery();
        for (Answer answer : answers) {
            QueryBuilder questionQuery = buildQuestionQuery(answer);
            if (questionQuery != null) {
                boolQueryBuilder.should(questionQuery);
            }
        }

        boolQueryBuilder.filter(nestedQuery("addresses", filterDistance(position, distance)));

        SortBuilder sortBuilder =
                SortBuilders
                        .geoDistanceSort("addresses.location")
                        .setNestedPath("addresses")
                        .point(position.getLat(), position.getLon())
                        .unit(DistanceUnit.KILOMETERS)
                        .order(SortOrder.DESC);

        SearchResponse searchResponse = client
                .prepareSearch(index)
                .setTypes(type)
                .setQuery(boolQueryBuilder)
                .setSize(DEFAULT_MAX_RESULT_SIZE)
                .addSort(SortBuilders.scoreSort())
                .addSort(sortBuilder)
                .execute()
                .actionGet();
        return extractOrganisations(searchResponse);
    }

    public List<ClusteredGeoPointDto> findClusteredGeoPoints(GeoPoint position,
                                                             double distance,
                                                             BoundingBox boundingBox, int zoom) {
        BoolQueryBuilder boolQueryBuilder = boolQuery();
        boolQueryBuilder.filter(nestedQuery("addresses",
                boolQuery()
                        .must(filterBox(boundingBox))
                        .mustNot(filterDistance(position, distance))
                )
        );

        SearchResponse searchResponse = client
                .prepareSearch(index)
                .setTypes(type)
                .setQuery(boolQueryBuilder)
                .setSize(0) // Hide results, only aggregation relevant
                .addAggregation(
                        AggregationBuilders
                                .nested("addresses")
                                .path("addresses")
                                .subAggregation(AggregationBuilders
                                        .geohashGrid("grouped_organizations")
                                        .field("addresses.location")
                                        .precision(zoom / 2)
                                        .size(DEFAULT_MAX_RESULT_SIZE)
                                )
                )
                .get();
        return extractClusteredOrganisations(searchResponse);
    }

    private QueryBuilder buildQuestionQuery(Answer answer) {
        if (isQuestionAnswered(answer)) {
            return null;
        }

        return nestedQuery("questions",
                boolQuery()
                        .must(termQuery("questions.uid", answer.getId()))
                        .must(termQuery("questions.answer", convertAnswerToString(answer)))
        );
    }

    private static boolean isQuestionAnswered(Answer answer) {
        return answer.getAnswer() != -1 && answer.getAnswer() != 1;
    }

    private static String convertAnswerToString(Answer answer) {
        if (answer.getAnswer() == 1) {
            return "YES";
        }
        return "NO";
    }

    private GeoBoundingBoxQueryBuilder filterBox(BoundingBox boundingBox) {
        GeoPoint topRight = boundingBox.getNorthEast();
        GeoPoint bottomLeft = boundingBox.getSouthWest();

        return geoBoundingBoxQuery("addresses.location")
                .topRight(topRight.getLat(), topRight.getLon())
                .bottomLeft(bottomLeft.getLat(), bottomLeft.getLon());
    }

    private GeoDistanceQueryBuilder filterDistance(GeoPoint position, double distance) {
        return geoDistanceQuery("addresses.location")
                .lat(position.getLat())
                .lon(position.getLon())
                .distance(distance, DistanceUnit.KILOMETERS);
    }

    private List<ClusteredGeoPointDto> extractClusteredOrganisations(SearchResponse searchResponse) {
        return searchResponse
                .getAggregations().<InternalNested>get("addresses")
                .getAggregations().<GeoHashGrid>get("grouped_organizations")
                .getBuckets().stream()
                .map(bucket -> new ClusteredGeoPointDto(
                        GeoPoint.fromGeoPoint((org.elasticsearch.common.geo.GeoPoint) bucket.getKey()),
                        bucket.getDocCount()
                ))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> extractOrganisations(SearchResponse searchResponse) {
        List<Map<String, Object>> organisations = new ArrayList<>();
        Float maxScore = null;
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            if (maxScore == null) {
                maxScore = hit.getScore();
            }

            Map<String, Object> response = new HashMap<>(hit.getSource());
            response.put("_score", hit.getScore());
            response.put("_scoreNorm", (hit.getScore() * 100) / maxScore);
            organisations.add(response);
        }
        return organisations;
    }

    private SearchResponse executeQuery(QueryBuilder queryBuilder, SortBuilder sortBuilder) {
        return client
                .prepareSearch(index)
                .setTypes(type)
                .setQuery(queryBuilder)
                .setSize(DEFAULT_MAX_RESULT_SIZE)
                .addSort(SortBuilders.scoreSort())
                .addSort(sortBuilder)
                .execute()
                .actionGet();
    }

    public List<Question> findQuestions() {
        Nested nested = client.prepareSearch(index)
                .addAggregation(AggregationBuilders
                        .nested("questions")
                        .path("questions")
                        .subAggregation(
                                AggregationBuilders.terms("question")
                                        .field("questions.question")
                                        .size(DEFAULT_MAX_RESULT_SIZE)
                                        .subAggregation(
                                                AggregationBuilders.terms("id")
                                                        .field("questions.uid")
                                                        .size(1)
                                        )
                                        .subAggregation(
                                                AggregationBuilders.terms("description")
                                                        .field("questions.description")
                                                        .size(1)
                                        )
                                        .subAggregation(
                                                AggregationBuilders.terms("position")
                                                        .field("questions.position")
                                                        .size(1)
                                        )
                        )
                ).get().getAggregations().get("questions");
        StringTerms questions = nested.getAggregations().get("question");
        return questions.getBuckets().stream()
                .map(this::bucketToQuestion)
                .sorted(this::sortQuestions)
                .collect(Collectors.toList());

    }

    private Question bucketToQuestion(Terms.Bucket s) {
        String id = String.valueOf(this.getSubbucket(s, "id", this::convertIntBucket).orElse(0));
        String question = s.getKeyAsString();
        String description = this.getSubbucket(s, "description", this::convertStringBucket).orElse(null);
        Integer position = this.getSubbucket(s, "position", this::convertIntBucket).orElse(0);
        return new Question(id, question, description, position);
    }

    private int sortQuestions(Question q1, Question q2) {
        return q1.getPosition() < q2.getPosition() ? -1 : 1;
    }

    private Integer convertIntBucket(Terms.Bucket intBucket) {
        return intBucket.getKeyAsNumber().intValue();
    }

    private String convertStringBucket(Terms.Bucket stringBucket) {
        return stringBucket.getKeyAsString();

    }

    private <T extends Terms, R> Optional<R> getSubbucket(Terms.Bucket bucket,
                                                          String aggregationName,
                                                          Function<Terms.Bucket, R> bucketConversion) {
        return bucket.getAggregations().<T>get(aggregationName)
                .getBuckets()
                .stream()
                .map(bucketConversion)
                .findFirst();
    }

}
