package com.stockandorder.domain.stock.entity;

import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.global.common.BaseTimeEntity;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "stock")
@Check(constraints = "quantity >= 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    public static Stock create(Product product) {
        Stock stock = new Stock();
        stock.product = product;
        stock.quantity = 0;
        return stock;
    }

    public void increase(int amount) {
        validatePositive(amount);
        this.quantity += amount;
    }

    public void decrease(int amount) {
        validatePositive(amount);
        if (this.quantity < amount) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }
        this.quantity -= amount;
    }

    // 수동 조정: 부호 포함 delta. 결과가 음수가 되면 차단.
    public void adjust(int delta) {
        if (delta == 0) {
            throw new IllegalArgumentException("조정 수량은 0이 될 수 없습니다.");
        }
        int next = this.quantity + delta;
        if (next < 0) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }
        this.quantity = next;
    }

    private void validatePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
    }
}
