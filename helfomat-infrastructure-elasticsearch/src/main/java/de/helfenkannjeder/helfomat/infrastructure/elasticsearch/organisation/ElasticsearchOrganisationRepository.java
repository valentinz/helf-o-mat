package de.helfenkannjeder.helfomat.infrastructure.elasticsearch.organisation;

import de.helfenkannjeder.helfomat.core.geopoint.BoundingBox;
import de.helfenkannjeder.helfomat.core.geopoint.GeoPoint;
import de.helfenkannjeder.helfomat.core.organisation.Address;
import de.helfenkannjeder.helfomat.core.organisation.ContactPerson;
import de.helfenkannjeder.helfomat.core.organisation.Organisation;
import de.helfenkannjeder.helfomat.core.organisation.OrganisationRepository;
import de.helfenkannjeder.helfomat.core.organisation.PictureId;
import de.helfenkannjeder.helfomat.core.question.Answer;
import de.helfenkannjeder.helfomat.infrastructure.elasticsearch.ElasticsearchConfiguration;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.rest.action.admin.indices.alias.delete.AliasesNotFoundException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoBoundingBoxQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoDistanceQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@Component
public class ElasticsearchOrganisationRepository implements OrganisationRepository {

    private static final int DEFAULT_MAX_RESULT_SIZE = 10000;

    private final Client client;
    private final ElasticsearchCrudOrganisationRepository elasticsearchCrudOrganisationRepository;
    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    public ElasticsearchOrganisationRepository(Client client,
                                               ElasticsearchCrudOrganisationRepository elasticsearchCrudOrganisationRepository,
                                               ElasticsearchConfiguration elasticsearchConfiguration, ElasticsearchTemplate elasticsearchTemplate) {
        this.client = client;
        this.elasticsearchCrudOrganisationRepository = elasticsearchCrudOrganisationRepository;
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    public boolean existsOrganisationWithSameTypeInDistance(String index, Organisation organisation, Long distanceInMeters) {
        List<Address> addresses = organisation.getAddresses();
        if (addresses.size() == 0) {
            return false;
        }
        GeoPoint locationToCheck = addresses.get(0).getLocation();

        GeoDistanceQueryBuilder geoDistanceQuery = geoDistanceQuery("addresses.location")
            .point(locationToCheck.getLat(), locationToCheck.getLon())
            .distance(distanceInMeters, DistanceUnit.METERS);

        SearchResponse searchResponse = client
            .prepareSearch(index)
            .setTypes(elasticsearchConfiguration.getType().getOrganisation())
            .setQuery(boolQuery()
                .must(matchQuery("type", organisation.getType()))
                .must(nestedQuery("addresses", geoDistanceQuery)))
            .execute()
            .actionGet();

        return searchResponse.getHits().totalHits() > 0;
    }

    @Override
    public Organisation findOne(String id) {
        return this.elasticsearchCrudOrganisationRepository.findOne(id);
    }

    @Override
    public LinkedHashMap<Organisation, Float> findOrganisation(Map<String, Answer> questionAnswers,
                                                               GeoPoint position,
                                                               double distance) {
        BoolQueryBuilder boolQueryBuilder = boolQuery();
        for (Map.Entry<String, Answer> questionAnswerDto : questionAnswers.entrySet()) {
            boolQueryBuilder.should(buildQuestionQuery(questionAnswerDto));
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
            .prepareSearch(this.elasticsearchConfiguration.getIndex())
            .setTypes(this.elasticsearchConfiguration.getType().getOrganisation())
            .setQuery(boolQueryBuilder)
            .setSize(DEFAULT_MAX_RESULT_SIZE)
            .addSort(SortBuilders.scoreSort())
            .addSort(sortBuilder)
            .execute()
            .actionGet();
        return extractOrganisations(searchResponse);
    }

    @Override
    public List<GeoPoint> findClusteredGeoPoints(GeoPoint position,
                                                 double distance,
                                                 BoundingBox boundingBox) {
        BoolQueryBuilder boolQueryBuilder = boolQuery();
        boolQueryBuilder.filter(nestedQuery("addresses",
            boolQuery()
                .must(filterBox(boundingBox))
                .mustNot(filterDistance(position, distance))
            )
        );

        SearchResponse searchResponse = client
            .prepareSearch(this.elasticsearchConfiguration.getIndex())
            .setTypes(this.elasticsearchConfiguration.getType().getOrganisation())
            .setQuery(boolQueryBuilder)
            .setSize(DEFAULT_MAX_RESULT_SIZE) // Hide results, only aggregation relevant
            .get();
        return extractOrganisations(searchResponse)
            .keySet()
            .stream()
            .map(Organisation::getAddresses)
            .filter(addresses -> !addresses.isEmpty())
            .map(addresses -> addresses.get(0))
            .map(Address::getLocation)
            .collect(Collectors.toList());
    }

    @Override
    public void save(String index, List<? extends Organisation> items) {
        List<IndexQuery> indexQueries = items.stream()
            .map(item -> new IndexQueryBuilder()
                .withId(String.valueOf(item.getId()))
                .withObject(item))
            .map(builder -> builder.withType(this.elasticsearchConfiguration.getType().getOrganisation()))
            .map(builder -> builder.withIndexName(index))
            .map(IndexQueryBuilder::build)
            .collect(Collectors.toList());

        this.elasticsearchTemplate.bulkIndex(indexQueries);
    }

    @Override
    public void createIndex(String index, String mapping) {
        this.elasticsearchTemplate.createIndex(index);
        this.elasticsearchTemplate.putMapping(index, this.elasticsearchConfiguration.getType().getOrganisation(), mapping);
    }

    @Override
    public void updateAlias(String index) {
        String alias = this.elasticsearchConfiguration.getIndex();
        try {
            AliasQuery removeAliasQuery = new AliasQuery();
            removeAliasQuery.setAliasName(alias);
            removeAliasQuery.setIndexName(alias + "-*");
            elasticsearchTemplate.removeAlias(removeAliasQuery);
        } catch (AliasesNotFoundException exception) {
            // Ignore
        }

        AliasQuery aliasQuery = new AliasQuery();
        aliasQuery.setAliasName(alias);
        aliasQuery.setIndexName(index);
        elasticsearchTemplate.addAlias(aliasQuery);
    }

    private QueryBuilder buildQuestionQuery(Map.Entry<String, Answer> questionAnswerDto) {
        BoolQueryBuilder questionQuery = boolQuery()
            .minimumNumberShouldMatch(1)
            .must(termQuery("questions.uid", questionAnswerDto.getKey()))
            .should(termQuery("questions.answer", questionAnswerDto.getValue().toString()).boost(2.0f));

        for (Answer neighbour : questionAnswerDto.getValue().getNeighbours()) {
            questionQuery.should(termQuery("questions.answer", neighbour.toString()).boost(1.0f));
        }

        return nestedQuery("questions", questionQuery);
    }

    private GeoBoundingBoxQueryBuilder filterBox(BoundingBox boundingBoxDto) {
        GeoPoint topRight = boundingBoxDto.getNorthEast();
        GeoPoint bottomLeft = boundingBoxDto.getSouthWest();

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

    private LinkedHashMap<Organisation, Float> extractOrganisations(SearchResponse searchResponse) {
        LinkedHashMap<Organisation, Float> organisations = new LinkedHashMap<>();
        Float maxScore = null;
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            if (maxScore == null) {
                maxScore = hit.getScore();
            }

            Organisation organisation = extractOrganisation(hit.getSource());
            float score = (hit.getScore() * 100) / maxScore;
            organisations.put(organisation, score);
        }
        return organisations;
    }

    @SuppressWarnings("unchecked")
    private Organisation extractOrganisation(Map<String, Object> source) {
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) source.get("addresses");
        List<ContactPerson> contactPersonDtos = extractContactPersons(source);
        return new Organisation.Builder()
            .setId((String) source.get("id"))
            .setName((String) source.get("name"))
            .setDescription((String) source.get("description"))
            .setWebsite((String) source.get("website"))
            .setMapPin((String) source.get("mapPin"))
            .setAddresses(addresses.stream().map(this::extractAddress).collect(Collectors.toList()))
            .setContactPersons(contactPersonDtos)
            .setLogo(extractLogoId(source))
            .build();
    }

    @SuppressWarnings("unchecked")
    private List<ContactPerson> extractContactPersons(Map<String, Object> source) {
        List<Map<String, Object>> contactPersons = (List<Map<String, Object>>) source.get("contactPersons");
        if (contactPersons == null) {
            return Collections.emptyList();
        }
        return contactPersons.stream().map(this::extractContactPerson).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private PictureId extractLogoId(Map<String, Object> source) {
        Map<String, Object> logo = (Map<String, Object>) source.get("logo");
        if (logo == null) {
            return null;
        }
        return new PictureId((String) logo.get("value"));
    }

    private ContactPerson extractContactPerson(Map<String, Object> contactPerson) {
        return new ContactPerson.Builder()
            .setFirstname((String) contactPerson.get("firstname"))
            .setLastname((String) contactPerson.get("lastname"))
            .setRank((String) contactPerson.get("rank"))
            .setTelephone((String) contactPerson.get("telephone"))
            .build();
    }

    @SuppressWarnings("unchecked")
    private Address extractAddress(Map<String, Object> address) {
        return new Address.Builder()
            .setStreet((String) address.get("street"))
            .setAddressAppendix((String) address.get("addressAppendix"))
            .setCity((String) address.get("city"))
            .setZipcode((String) address.get("zipcode"))
            .setLocation(extractGeoPoint((Map<String, Object>) address.get("location")))
            .setTelephone((String) address.get("telephone"))
            .setWebsite((String) address.get("website"))
            .build();
    }

    private GeoPoint extractGeoPoint(Map<String, Object> geoPoint) {
        return new GeoPoint(
            (double) geoPoint.get("lat"),
            (double) geoPoint.get("lon")
        );
    }

}