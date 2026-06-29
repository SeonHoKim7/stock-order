package com.stockandorder.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    MEMBER_LOGIN_ID_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    MEMBER_INACTIVE(HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
    MEMBER_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    CATEGORY_NAME_DUPLICATE(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다."),
    CATEGORY_HAS_PRODUCTS(HttpStatus.CONFLICT, "해당 카테고리에 상품이 존재하여 삭제할 수 없습니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    PRODUCT_CODE_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 상품 코드입니다."),
    PRODUCT_INACTIVE(HttpStatus.BAD_REQUEST, "비활성화된 상품입니다."),

    // Supplier
    SUPPLIER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 거래처입니다."),
    SUPPLIER_INACTIVE(HttpStatus.BAD_REQUEST, "비활성화된 거래처입니다."),
    SUPPLIER_TYPE_INVALID(HttpStatus.BAD_REQUEST, "해당 거래처는 이 작업을 수행할 수 없는 유형입니다."),

    // PurchaseOrder
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 발주입니다."),
    ORDER_STATUS_CANNOT_APPROVE(HttpStatus.BAD_REQUEST, "대기 상태의 발주만 승인할 수 있습니다."),
    ORDER_STATUS_CANNOT_REJECT(HttpStatus.BAD_REQUEST, "대기 상태의 발주만 반려할 수 있습니다."),
    ORDER_STATUS_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "대기 상태의 발주만 취소할 수 있습니다."),
    ORDER_CANNOT_MODIFY(HttpStatus.BAD_REQUEST, "승인 이후의 발주는 수정하거나 삭제할 수 없습니다."),
    ORDER_SELF_APPROVAL(HttpStatus.BAD_REQUEST, "본인이 요청한 발주는 본인이 승인/반려할 수 없습니다."),
    ORDER_NOT_REQUESTER(HttpStatus.FORBIDDEN, "발주 요청자만 취소할 수 있습니다."),
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 발주 항목입니다."),

    // Inbound
    INBOUND_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 입고 내역입니다."),
    INBOUND_ORDER_NOT_APPROVED(HttpStatus.BAD_REQUEST, "승인된 발주에 대해서만 입고 처리를 할 수 있습니다."),
    INBOUND_QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, "입고 수량이 발주 잔여 수량을 초과했습니다."),
    INBOUND_ITEM_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 전량 입고 완료된 항목입니다."),
    INBOUND_DUPLICATE_ORDER_ITEM(HttpStatus.BAD_REQUEST, "같은 발주 항목을 중복으로 입력할 수 없습니다."),

    // Outbound (단일화: 생성 즉시 확정·재고 차감. status 없음 → 확정/취소 상태 에러코드 없음)
    OUTBOUND_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 출고 내역입니다."),
    OUTBOUND_DUPLICATE_PRODUCT(HttpStatus.BAD_REQUEST, "같은 상품을 중복으로 입력할 수 없습니다. 한 줄로 합쳐주세요."),

    // Stock
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상품의 재고 정보가 존재하지 않습니다."),
    STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),
    STOCK_ADJUST_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "재고 수동 조정 시 사유 입력은 필수입니다."),
    STOCK_ADJUST_NO_CHANGE(HttpStatus.BAD_REQUEST, "실사 수량이 현재고와 동일하여 조정할 내용이 없습니다."),
    STOCK_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "조회 이후 재고가 변경되었습니다. 현재고를 다시 확인한 뒤 조정해 주세요.");

    private final HttpStatus status;
    private final String message;
}
