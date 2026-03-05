package com.stockandorder.domain.supplier.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSupplier is a Querydsl query type for Supplier
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSupplier extends EntityPathBase<Supplier> {

    private static final long serialVersionUID = 1650728625L;

    public static final QSupplier supplier = new QSupplier("supplier");

    public final com.stockandorder.global.common.QBaseTimeEntity _super = new com.stockandorder.global.common.QBaseTimeEntity(this);

    public final StringPath address = createString("address");

    public final StringPath contactEmail = createString("contactEmail");

    public final StringPath contactName = createString("contactName");

    public final StringPath contactPhone = createString("contactPhone");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath name = createString("name");

    public final NumberPath<Long> supplierId = createNumber("supplierId", Long.class);

    public final EnumPath<com.stockandorder.domain.supplier.enums.SupplierType> supplierType = createEnum("supplierType", com.stockandorder.domain.supplier.enums.SupplierType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSupplier(String variable) {
        super(Supplier.class, forVariable(variable));
    }

    public QSupplier(Path<? extends Supplier> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSupplier(PathMetadata metadata) {
        super(Supplier.class, metadata);
    }

}

