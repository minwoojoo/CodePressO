package com.codepresso.codepresso.service.payment;

import com.codepresso.codepresso.dto.payment.CheckoutRequest;
import com.codepresso.codepresso.entity.order.Orders;
import com.codepresso.codepresso.entity.order.OrdersDetail;
import com.codepresso.codepresso.entity.order.OrdersItemOptions;
import com.codepresso.codepresso.entity.product.Product;
import com.codepresso.codepresso.entity.product.ProductOption;
import com.codepresso.codepresso.repository.product.ProductOptionRepository;
import com.codepresso.codepresso.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 주문 생성 로직을 담당하는 서비스 클래스
 */
@Component
@RequiredArgsConstructor
public class OrderCreationService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;


    /**
     * 주문 상세 생성
     */
    public List<OrdersDetail> createOrderDetails(List<CheckoutRequest.OrderItem> orderItems, Orders orders) {
        List<OrdersDetail> orderDetails = new ArrayList<>();
        Map<Long, Product> productMap = getProductMap(orderItems);
        Map<Long, ProductOption> optionMap = getProductOptionMap(orderItems);

        // 할인 전 총액 계산
        int totalBeforeDiscount = orderItems.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        // 할인 금액 가져오기
        int totalDiscount =orders.getDiscountAmount() != null ? orders.getDiscountAmount() : 0;

        // 할인율 계산
        double discountRate = totalBeforeDiscount > 0 ? (double) totalDiscount/totalBeforeDiscount : 0;

        int accumulateDiscount = 0;

        for (int i = 0; i<orderItems.size(); i++) {
            CheckoutRequest.OrderItem item = orderItems.get(i);

            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("존재하지 않는 상품이 포함되어 있습니다.");
            }

            int itemOriginalPrice = item.getPrice() * item.getQuantity();

            int itemDiscount;
            if(i==orderItems.size()-1) {
                itemDiscount = totalDiscount - accumulateDiscount;
            }else {
                itemDiscount = (int)Math.round(itemOriginalPrice * discountRate);
                accumulateDiscount = accumulateDiscount + itemDiscount;
            }

            // 할인 적용된 가격
            int discountedPrice = itemOriginalPrice - itemDiscount;

            // 주문 상세 생성
            OrdersDetail ordersDetail = OrdersDetail.builder()
                    .orders(orders)
                    .product(product)
                    .price(discountedPrice)
                    .quantity(item.getQuantity())
                    .build();

            if(item.getOptionIds() != null && !item.getOptionIds().isEmpty()) {
                List<OrdersItemOptions> options = createOrderItemOptions(
                        item.getOptionIds(), ordersDetail, item.getProductId(), optionMap);
                ordersDetail.setOptions(options);
            }

            orderDetails.add(ordersDetail);
        }

        return orderDetails;
    }

    private Map<Long, Product> getProductMap(List<CheckoutRequest.OrderItem> orderItems) {
        Set<Long> productIds = orderItems.stream()
                .map(CheckoutRequest.OrderItem::getProductId)
                .collect(Collectors.toSet());

        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 상품이 포함되어 있습니다.");
        }

        return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<Long, ProductOption> getProductOptionMap(List<CheckoutRequest.OrderItem> orderItems) {
        Set<Long> optionIds = orderItems.stream()
                .filter(item -> item.getOptionIds() != null)
                .flatMap(item -> item.getOptionIds().stream())
                .collect(Collectors.toSet());

        if (optionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ProductOption> productOptions = productOptionRepository.findAllWithProductByIdIn(optionIds);
        if (productOptions.size() != optionIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 옵션이 포함되어 있습니다.");
        }

        return productOptions.stream()
                .collect(Collectors.toMap(ProductOption::getId, Function.identity()));
    }

    /**
     * 주문 아이템 옵션 생성
     */
    private List<OrdersItemOptions> createOrderItemOptions(
            List<Long> optionIds,
            OrdersDetail orderDetail,
            Long productId,
            Map<Long, ProductOption> optionMap) {
        List<OrdersItemOptions> orderItemOptions = new ArrayList<>();
        Set<Long> distinctOptionIds = new HashSet<>(optionIds);

        if (distinctOptionIds.size() != optionIds.size()) {
            throw new IllegalArgumentException("중복 옵션은 허용되지 않습니다.");
        }

        for (Long optionId : optionIds) {
            ProductOption productOption = optionMap.get(optionId);
            if (productOption == null) {
                throw new IllegalArgumentException("존재하지 않는 옵션입니다: " + optionId);
            }
            if (!productOption.getProduct().getId().equals(productId)) {
                throw new IllegalArgumentException("해당 상품에 속하지 않는 옵션입니다.");
            }

            OrdersItemOptions orderItemOption = OrdersItemOptions.builder()
                    .option(productOption)
                    .ordersDetail(orderDetail)
                    .build();

            orderItemOptions.add(orderItemOption);
        }

        return orderItemOptions;
    }
}
