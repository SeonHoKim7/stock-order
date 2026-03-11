package com.stockandorder.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPurchaseOrder is a Querydsl query type for PurchaseOrder
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPurchaseOrder extends EntityPathBase<PurchaseOrder> {

    private static final long serialVersionUID = -371784002L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPurchaseOrder purchaseOrder = new QPurchaseOrder("purchaseOrder");

    public final com.stockandorder.global.common.QBaseTimeEntity _super = new com.stockandorder.global.common.QBaseTimeEntity(this);

    public final com.stockandorder.domain.member.entity.QMember approver;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final ListPath<PurchaseOrderItem, QPurchaseOrderItem> items = this.<PurchaseOrderItem, QPurchaseOrderItem>createList("items", PurchaseOrderItem.class, QPurchaseOrderItem.class, PathInits.DIRECT2);

    public final StringPath note = createString("note");

    public final DateTimePath<java.time.LocalDateTime> orderedAt = createDateTime("orderedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> orderId = createNumber("orderId", Long.class);

    public final StringPath orderNumber = createString("orderNumber");

    public final DateTimePath<java.time.LocalDateTime> processedAt = createDateTime("processedAt", java.time.LocalDateTime.class);

    public final StringPath rejectReason = createString("rejectReason");

    public final com.stockandorder.domain.member.entity.QMember requester;

    public final EnumPath<com.stockandorder.domain.order.enums.OrderStatus> status = createEnum("status", com.stockandorder.domain.order.enums.OrderStatus.class);

    public final com.stockandorder.domain.supplier.entity.QSupplier supplier;

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QPurchaseOrder(String variable) {
        this(PurchaseOrder.class, forVariable(variable), INITS);
    }

    public QPurchaseOrder(Path<? extends PurchaseOrder> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPurchaseOrder(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPurchaseOrder(PathMetadata metadata, PathInits inits) {
        this(PurchaseOrder.class, metadata, inits);
    }

    public QPurchaseOrder(Class<? extends PurchaseOrder> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.approver = inits.isInitialized("approver") ? new com.stockandorder.domain.member.entity.QMember(forProperty("approver")) : null;
        this.requester = inits.isInitialized("requester") ? new com.stockandorder.domain.member.entity.QMember(forProperty("requester")) : null;
        this.supplier = inits.isInitialized("supplier") ? new com.stockandorder.domain.supplier.entity.QSupplier(forProperty("supplier")) : null;
    }

}

