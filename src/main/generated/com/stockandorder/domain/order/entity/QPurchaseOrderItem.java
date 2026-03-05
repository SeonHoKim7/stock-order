package com.stockandorder.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPurchaseOrderItem is a Querydsl query type for PurchaseOrderItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPurchaseOrderItem extends EntityPathBase<PurchaseOrderItem> {

    private static final long serialVersionUID = -2055444751L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPurchaseOrderItem purchaseOrderItem = new QPurchaseOrderItem("purchaseOrderItem");

    public final NumberPath<Long> orderItemId = createNumber("orderItemId", Long.class);

    public final com.stockandorder.domain.product.entity.QProduct product;

    public final QPurchaseOrder purchaseOrder;

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final NumberPath<Integer> receivedQuantity = createNumber("receivedQuantity", Integer.class);

    public final NumberPath<java.math.BigDecimal> unitPrice = createNumber("unitPrice", java.math.BigDecimal.class);

    public QPurchaseOrderItem(String variable) {
        this(PurchaseOrderItem.class, forVariable(variable), INITS);
    }

    public QPurchaseOrderItem(Path<? extends PurchaseOrderItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPurchaseOrderItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPurchaseOrderItem(PathMetadata metadata, PathInits inits) {
        this(PurchaseOrderItem.class, metadata, inits);
    }

    public QPurchaseOrderItem(Class<? extends PurchaseOrderItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new com.stockandorder.domain.product.entity.QProduct(forProperty("product"), inits.get("product")) : null;
        this.purchaseOrder = inits.isInitialized("purchaseOrder") ? new QPurchaseOrder(forProperty("purchaseOrder"), inits.get("purchaseOrder")) : null;
    }

}

