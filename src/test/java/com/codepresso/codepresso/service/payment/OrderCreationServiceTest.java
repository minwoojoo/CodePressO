package com.codepresso.codepresso.service.payment;

import com.codepresso.codepresso.dto.payment.CheckoutRequest;
import com.codepresso.codepresso.entity.order.Orders;
import com.codepresso.codepresso.entity.order.OrdersDetail;
import com.codepresso.codepresso.entity.product.Product;
import com.codepresso.codepresso.entity.product.ProductOption;
import com.codepresso.codepresso.repository.product.ProductOptionRepository;
import com.codepresso.codepresso.repository.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceTest {

    @InjectMocks
    private OrderCreationService orderCreationService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Test
    void createOrderDetails_usesBulkLookupAndCreatesSameResult() {
        Product product1 = Product.builder().id(1L).productName("아메리카노").build();
        Product product2 = Product.builder().id(2L).productName("라떼").build();
        ProductOption option1 = ProductOption.builder().id(10L).product(product1).build();
        ProductOption option2 = ProductOption.builder().id(20L).product(product2).build();
        List<CheckoutRequest.OrderItem> orderItems = List.of(
                CheckoutRequest.OrderItem.builder()
                        .productId(1L)
                        .quantity(2)
                        .price(3000)
                        .optionIds(List.of(10L))
                        .build(),
                CheckoutRequest.OrderItem.builder()
                        .productId(2L)
                        .quantity(1)
                        .price(4000)
                        .optionIds(List.of(20L))
                        .build()
        );
        Orders orders = Orders.builder().discountAmount(1000).build();

        when(productRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(product1, product2));
        when(productOptionRepository.findAllWithProductByIdIn(Set.of(10L, 20L)))
                .thenReturn(List.of(option1, option2));

        List<OrdersDetail> result = orderCreationService.createOrderDetails(orderItems, orders);

        assertEquals(2, result.size());
        assertEquals(product1, result.get(0).getProduct());
        assertEquals(5400, result.get(0).getPrice());
        assertEquals(2, result.get(0).getQuantity());
        assertEquals(option1, result.get(0).getOptions().get(0).getOption());
        assertEquals(product2, result.get(1).getProduct());
        assertEquals(3600, result.get(1).getPrice());
        assertEquals(1, result.get(1).getQuantity());
        assertEquals(option2, result.get(1).getOptions().get(0).getOption());
        verify(productRepository, never()).findById(1L);
        verify(productRepository, never()).findById(2L);
        verify(productOptionRepository, never()).findById(10L);
        verify(productOptionRepository, never()).findById(20L);
    }

    @Test
    void createOrderDetails_throwsWhenProductDoesNotExist() {
        List<CheckoutRequest.OrderItem> orderItems = List.of(
                CheckoutRequest.OrderItem.builder()
                        .productId(1L)
                        .quantity(1)
                        .price(3000)
                        .build(),
                CheckoutRequest.OrderItem.builder()
                        .productId(999L)
                        .quantity(1)
                        .price(4000)
                        .build()
        );

        when(productRepository.findAllById(Set.of(1L, 999L)))
                .thenReturn(List.of(Product.builder().id(1L).build()));

        assertThrows(IllegalArgumentException.class,
                () -> orderCreationService.createOrderDetails(orderItems, Orders.builder().build()));
        verify(productOptionRepository, never()).findAllWithProductByIdIn(anySet());
    }

    @Test
    void createOrderDetails_throwsWhenOptionBelongsToOtherProduct() {
        Product product1 = Product.builder().id(1L).build();
        Product product2 = Product.builder().id(2L).build();
        ProductOption otherProductOption = ProductOption.builder().id(20L).product(product2).build();
        List<CheckoutRequest.OrderItem> orderItems = List.of(
                CheckoutRequest.OrderItem.builder()
                        .productId(1L)
                        .quantity(1)
                        .price(3000)
                        .optionIds(List.of(20L))
                        .build()
        );

        when(productRepository.findAllById(Set.of(1L))).thenReturn(List.of(product1));
        when(productOptionRepository.findAllWithProductByIdIn(Set.of(20L)))
                .thenReturn(List.of(otherProductOption));

        assertThrows(IllegalArgumentException.class,
                () -> orderCreationService.createOrderDetails(orderItems, Orders.builder().build()));
    }

    @Test
    void createOrderDetails_throwsWhenOptionDoesNotExist() {
        Product product = Product.builder().id(1L).build();
        List<CheckoutRequest.OrderItem> orderItems = List.of(
                CheckoutRequest.OrderItem.builder()
                        .productId(1L)
                        .quantity(1)
                        .price(3000)
                        .optionIds(List.of(999L))
                        .build()
        );

        when(productRepository.findAllById(Set.of(1L))).thenReturn(List.of(product));
        when(productOptionRepository.findAllWithProductByIdIn(Set.of(999L))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> orderCreationService.createOrderDetails(orderItems, Orders.builder().build()));
    }
}
