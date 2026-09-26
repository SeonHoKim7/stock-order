package com.stockandorder.domain.stock.controller;

import com.stockandorder.domain.category.service.CategoryService;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.domain.stock.dto.StockLogResponse;
import com.stockandorder.domain.stock.enums.StockChangeType;
import com.stockandorder.domain.stock.service.StockService;
import com.stockandorder.global.auth.CustomUserDetails;
import com.stockandorder.global.auth.CustomUserDetailsService;
import com.stockandorder.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 재고 변동 이력 화면이 실제로 렌더되는지 확인한다.
 * 서비스 단위 테스트는 Thymeleaf 표현식 오류를 잡지 못하므로 템플릿까지 태운다.
 */
@WebMvcTest(controllers = StockController.class)
@Import(SecurityConfig.class)
class StockLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private MemberRepository memberRepository;

    private static final long STAFF_ID = 1L;

    private CustomUserDetails staffUserDetails() {
        Member member = Member.create("staff01", "encoded", "홍길동", null, Role.STAFF);
        ReflectionTestUtils.setField(member, "memberId", STAFF_ID);
        given(memberRepository.findById(STAFF_ID)).willReturn(Optional.of(member));
        return new CustomUserDetails(member);
    }

    private Page<StockLogResponse> pageOf(StockLogResponse... logs) {
        return new PageImpl<>(List.of(logs), PageRequest.of(0, 20), logs.length);
    }

    @Test
    @DisplayName("이력 목록 화면이 렌더되고 유형·변동량·처리자가 표시된다")
    void logs_rendersHistory() throws Exception {
        StockLogResponse inbound = new StockLogResponse(LocalDateTime.now(), 1L, "PRD-001", "밀가루",
                StockChangeType.INBOUND, 50, 0, 50, 100L, null, "김매니저");
        StockLogResponse adjust = new StockLogResponse(LocalDateTime.now(), 1L, "PRD-001", "밀가루",
                StockChangeType.ADJUST, -3, 50, 47, null, "파손 처리", "박관리자");
        given(stockService.searchStockLogs(any(), any())).willReturn(pageOf(adjust, inbound));

        mockMvc.perform(get("/stocks/logs").with(user(staffUserDetails())))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/logs"))
                .andExpect(model().attributeExists("logs"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("재고 변동 이력")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("+50")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("-3")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("파손 처리")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("김매니저")))
                // 입고 이력은 원본 문서로 연결된다
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/inbounds/100")));
    }

    @Test
    @DisplayName("처리자가 없는 과거 이력도 화면에서 깨지지 않는다")
    void logs_rendersNullActor() throws Exception {
        StockLogResponse legacy = new StockLogResponse(LocalDateTime.now(), 1L, "PRD-001", "밀가루",
                StockChangeType.INBOUND, 10, 0, 10, 100L, null, null);
        given(stockService.searchStockLogs(any(), any())).willReturn(pageOf(legacy));

        mockMvc.perform(get("/stocks/logs").with(user(staffUserDetails())))
                .andExpect(status().isOk())
                .andExpect(view().name("stock/logs"));
    }

    @Test
    @DisplayName("이력이 없으면 빈 상태 문구를 보여준다")
    void logs_emptyState() throws Exception {
        given(stockService.searchStockLogs(any(), any())).willReturn(pageOf());

        mockMvc.perform(get("/stocks/logs").with(user(staffUserDetails())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("조회된 이력이 없습니다")));
    }

    @Test
    @DisplayName("상품을 지정해 들어오면 그 범위가 조회 조건에 실린다")
    void logs_withProductId_bindsCondition() throws Exception {
        given(stockService.searchStockLogs(any(), any())).willReturn(pageOf());

        mockMvc.perform(get("/stocks/logs").param("productId", "7").with(user(staffUserDetails())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("condition",
                        org.hamcrest.Matchers.hasProperty("productId", org.hamcrest.Matchers.is(7L))));
    }

    @Test
    @DisplayName("이력 조회는 STAFF도 볼 수 있다(재고 현황과 같은 기준)")
    void logs_allowedForStaff() throws Exception {
        given(stockService.searchStockLogs(any(), any())).willReturn(pageOf());

        mockMvc.perform(get("/stocks/logs").with(user(staffUserDetails())))
                .andExpect(status().isOk());
    }
}
