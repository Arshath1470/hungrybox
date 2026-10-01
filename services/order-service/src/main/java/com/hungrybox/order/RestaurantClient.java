package com.hungrybox.order;

import com.hungrybox.common.web.ApiException;
import com.hungrybox.order.OrderDtos.MenuItemSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Synchronous call to restaurant-service to fetch authoritative prices and availability. */
@Component
public class RestaurantClient {

    private final RestClient restClient;

    public RestaurantClient(RestClient.Builder builder,
                            @Value("${hungrybox.restaurant-service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(3_000);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public List<MenuItemSnapshot> menuItems(Collection<Long> ids) {
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<MenuItemSnapshot> items = restClient.get()
                    .uri(uri -> uri.path("/api/restaurants/menu-items").queryParam("ids", joined).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MenuItemSnapshot>>() { });
            return items == null ? List.of() : items;
        } catch (RestClientException e) {
            throw ApiException.unavailable("Restaurant service is unavailable, please retry");
        }
    }
}
