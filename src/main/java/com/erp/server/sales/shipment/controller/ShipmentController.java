package com.erp.server.sales.shipment.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.server.common.response.ApiResponse;
import com.erp.server.common.response.PageMeta;
import com.erp.server.common.security.AppUserDetails;
import com.erp.server.sales.shipment.domain.ShipmentStatus;
import com.erp.server.sales.shipment.dto.ShipmentAvailableLotResponse;
import com.erp.server.sales.shipment.dto.ShipmentCompleteResponse;
import com.erp.server.sales.shipment.dto.ShipmentDetailResponse;
import com.erp.server.sales.shipment.dto.ShipmentListResponse;
import com.erp.server.sales.shipment.dto.ShipmentPackResponse;
import com.erp.server.sales.shipment.dto.ShipmentPackingRequest;
import com.erp.server.sales.shipment.dto.ShipmentUnpackRequest;
import com.erp.server.sales.shipment.dto.ShipmentUnpackResponse;
import com.erp.server.sales.shipment.dto.ShipmentVersionRequest;
import com.erp.server.sales.shipment.service.ShipmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** 출고 목록·상세·LOT·포장·완료·납품서 요청을 업무 Service에 전달하기 위한 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

	private final ShipmentService shipmentService;

	// ========== 거래처·출고 상태·주문 접수 기간 조건으로 출고 목록을 페이지 조회하는 메서드 ==========
	@GetMapping
	public ApiResponse<List<ShipmentListResponse>> getShipments(
			@RequestParam(name = "customerId", required = false) Long customerId,
			@RequestParam(name = "status", required = false) ShipmentStatus status,
			@RequestParam(name = "startDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(name = "endDate", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(name = "page", defaultValue = "0") int page, Authentication authentication) {
		AppUserDetails currentUser = getCurrentUser(authentication);
		Page<ShipmentListResponse> shipments = shipmentService.getShipments(customerId, status, startDate, endDate,
				page, currentUser.getRole());
		return ApiResponse.success(shipments.getContent(), PageMeta.from(shipments));
	}

	// ========== shipmentId로 주문·배송·포장 LOT·납품서·처리 이력을 상세 조회하는 메서드 ==========
	@GetMapping("/{shipmentId}")
	public ApiResponse<ShipmentDetailResponse> getShipment(@PathVariable(name = "shipmentId") Long shipmentId,
			Authentication authentication) {
		return ApiResponse.success(shipmentService.getShipment(shipmentId, getCurrentUser(authentication).getRole()));
	}

	// ========== 선택한 ACTIVE 창고에서 주문 품목에 사용할 수 있는 최신 가용 LOT를 조회하는 메서드 ==========
	@GetMapping("/{shipmentId}/available-lots")
	public ApiResponse<List<ShipmentAvailableLotResponse>> getAvailableLots(
			@PathVariable(name = "shipmentId") Long shipmentId,
			@RequestParam(name = "warehouseId") Long warehouseId) {
		return ApiResponse.success(shipmentService.getAvailableLots(shipmentId, warehouseId));
	}

	// ========== PENDING 출고의 단일 창고와 LOT별 포장 수량을 예약 없이 저장하는 메서드 ==========
	@PutMapping("/{shipmentId}/packing")
	public ApiResponse<ShipmentDetailResponse> savePacking(@PathVariable(name = "shipmentId") Long shipmentId,
			@Valid @RequestBody ShipmentPackingRequest request, Authentication authentication) {
		return ApiResponse.success(shipmentService.savePacking(shipmentId, request,
				getCurrentUser(authentication).getRole()));
	}

	// ========== 저장된 전량 포장안을 최신 재고에 예약하고 납품서를 발행하는 메서드 ==========
	@PostMapping("/{shipmentId}/pack")
	public ApiResponse<ShipmentPackResponse> packShipment(@PathVariable(name = "shipmentId") Long shipmentId,
			@Valid @RequestBody ShipmentVersionRequest request, Authentication authentication) {
		return ApiResponse.success(shipmentService.packShipment(shipmentId, request.version(),
				getCurrentUser(authentication).getUserId()));
	}

	// ========== PACKED 출고의 재고 예약과 납품서를 해제·무효화하고 PENDING으로 되돌리는 메서드 ==========
	@PostMapping("/{shipmentId}/unpack")
	public ApiResponse<ShipmentUnpackResponse> unpackShipment(@PathVariable(name = "shipmentId") Long shipmentId,
			@Valid @RequestBody ShipmentUnpackRequest request, Authentication authentication) {
		return ApiResponse.success(shipmentService.unpackShipment(shipmentId, request,
				getCurrentUser(authentication).getUserId()));
	}

	// ========== PACKED 출고를 실제 재고 감소·매출 전표·주문과 함께 완료하는 메서드 ==========
	@PostMapping("/{shipmentId}/complete")
	public ApiResponse<ShipmentCompleteResponse> completeShipment(
			@PathVariable(name = "shipmentId") Long shipmentId,
			@Valid @RequestBody ShipmentVersionRequest request, Authentication authentication) {
		return ApiResponse.success(shipmentService.completeShipment(shipmentId, request.version(),
				getCurrentUser(authentication).getUserId()));
	}

	// ========== ACTIVE 납품서 회차를 application/pdf 응답으로 생성하여 내려받는 메서드 ==========
	@GetMapping("/{shipmentId}/delivery-notes/{issueSequence}/pdf")
	public ResponseEntity<byte[]> getDeliveryNotePdf(@PathVariable(name = "shipmentId") Long shipmentId,
			@PathVariable(name = "issueSequence") Integer issueSequence) {
		byte[] pdf = shipmentService.getDeliveryNotePdf(shipmentId, issueSequence);
		String fileName = "delivery-note-" + shipmentId + "-" + issueSequence + ".pdf";
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(fileName).build().toString())
				.body(pdf);
	}

	private AppUserDetails getCurrentUser(Authentication authentication) {
		return (AppUserDetails) authentication.getPrincipal();
	}
}
