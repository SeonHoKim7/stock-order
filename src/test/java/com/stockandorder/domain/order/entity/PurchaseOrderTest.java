package com.stockandorder.domain.order.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.order.enums.OrderStatus;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseOrderTest {

    private Supplier supplier;
    private Member requester;
    private Member approver;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        supplier = Supplier.create("테스트 공급처", SupplierType.PURCHASE,
                "담당자", "010-1234-5678", "test@test.com", "서울");
        requester = Member.create("staff1", "password", "직원1", "staff1@test.com", Role.STAFF);
        approver = Member.create("manager1", "password", "매니저1", "manager1@test.com", Role.MANAGER);

        Category category = Category.create("식자재", null);
        product1 = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
        product2 = Product.create("PRD-002", "설탕", category, "KG",
                BigDecimal.valueOf(5000), 5, null);
    }

    // create

    @Test
    @DisplayName("create() 호출 시 PENDING 상태, totalAmount 0, orderedAt이 설정된다")
    void create_setsInitialState() {
        PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, "테스트 비고");

        assertThat(order.getOrderNumber()).isEqualTo("PO-20260305-001");
        assertThat(order.getSupplier()).isEqualTo(supplier);
        assertThat(order.getRequester()).isEqualTo(requester);
        assertThat(order.getNote()).isEqualTo("테스트 비고");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getOrderedAt()).isNotNull();
        assertThat(order.getApprover()).isNull();
        assertThat(order.getProcessedAt()).isNull();
        assertThat(order.getItems()).isEmpty();
    }

    // addItem + totalAmount 자동 계산

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("항목 추가 시 items에 반영되고 totalAmount가 자동 계산된다")
        void addItem_addsItemAndCalculatesTotalAmount() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);
            PurchaseOrderItem item = PurchaseOrderItem.create(product1, 10, BigDecimal.valueOf(10000));

            order.addItem(item);

            assertThat(order.getItems()).hasSize(1);
            assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(item.getPurchaseOrder()).isEqualTo(order);
        }

        @Test
        @DisplayName("여러 항목 추가 시 totalAmount가 합산된다")
        void addItem_multipleItems_calculatesSumOfAllItems() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);

            order.addItem(PurchaseOrderItem.create(product1, 10, BigDecimal.valueOf(10000)));
            order.addItem(PurchaseOrderItem.create(product2, 20, BigDecimal.valueOf(5000)));

            assertThat(order.getItems()).hasSize(2);
            // 10 * 10,000 + 20 * 5,000 = 200,000
            assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200000));
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 항목 추가 시 ORDER_CANNOT_MODIFY 예외가 발생한다")
        void addItem_notPending_throwsException() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);
            order.approve(approver);

            PurchaseOrderItem item = PurchaseOrderItem.create(product1, 10, BigDecimal.valueOf(10000));

            assertThatThrownBy(() -> order.addItem(item))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_CANNOT_MODIFY));
        }
    }

    // removeItem

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("항목 제거 시 items에서 삭제되고 totalAmount가 재계산된다")
        void removeItem_removesItemAndRecalculatesTotalAmount() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);
            PurchaseOrderItem item1 = PurchaseOrderItem.create(product1, 10, BigDecimal.valueOf(10000));
            PurchaseOrderItem item2 = PurchaseOrderItem.create(product2, 20, BigDecimal.valueOf(5000));
            order.addItem(item1);
            order.addItem(item2);

            order.removeItem(item1);

            assertThat(order.getItems()).hasSize(1);
            assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 항목 제거 시 ORDER_CANNOT_MODIFY 예외가 발생한다")
        void removeItem_notPending_throwsException() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);
            PurchaseOrderItem item = PurchaseOrderItem.create(product1, 10, BigDecimal.valueOf(10000));
            order.addItem(item);
            order.approve(approver);

            assertThatThrownBy(() -> order.removeItem(item))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_CANNOT_MODIFY));
        }
    }

    // approve

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("PENDING 상태에서 승인 시 APPROVED 상태로 전이되고 approver, processedAt이 설정된다")
        void approve_pendingOrder_changesStatusToApproved() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);

            order.approve(approver);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
            assertThat(order.getApprover()).isEqualTo(approver);
            assertThat(order.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 승인 시 ORDER_STATUS_CANNOT_APPROVE 예외가 발생한다")
        void approve_notPending_throwsException() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);
            order.cancel();

            assertThatThrownBy(() -> order.approve(approver))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_APPROVE));
        }
    }

    // reject

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("PENDING 상태에서 반려 시 REJECTED 상태로 전이되고 approver, processedAt, rejectReason이 설정된다")
        void reject_pendingOrder_changesStatusToRejected() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);

            order.reject(approver, "단가 재협상 필요");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
            assertThat(order.getApprover()).isEqualTo(approver);
            assertThat(order.getProcessedAt()).isNotNull();
            assertThat(order.getRejectReason()).isEqualTo("단가 재협상 필요");
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 반려 시 ORDER_STATUS_CANNOT_REJECT 예외가 발생한다")
        void reject_notPending_throwsException() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);
            order.approve(approver);

            assertThatThrownBy(() -> order.reject(approver, "사유"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_REJECT));
        }
    }

    // cancel

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("PENDING 상태에서 취소 시 CANCELLED 상태로 전이된다")
        void cancel_pendingOrder_changesStatusToCancelled() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);

            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 취소 시 ORDER_STATUS_CANNOT_CANCEL 예외가 발생한다")
        void cancel_notPending_throwsException() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260305-001", supplier, requester, null);
            order.approve(approver);

            assertThatThrownBy(() -> order.cancel())
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_STATUS_CANNOT_CANCEL));
        }
    }
}
