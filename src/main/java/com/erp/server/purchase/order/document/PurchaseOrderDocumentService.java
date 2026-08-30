package com.erp.server.purchase.order.document;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.master.item.domain.ItemUnit;
import com.erp.server.purchase.order.domain.PurchaseOrder;
import com.erp.server.purchase.order.domain.PurchaseOrderItem;
import com.erp.server.purchase.order.domain.PurchaseOrderStatus;
import com.erp.server.purchase.order.repository.PurchaseOrderItemRepository;
import com.erp.server.purchase.order.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;

// ********** 발주서 PDF와 이메일에 필요한 DB 값을 조회하여 트랜잭션 밖에서도 사용할 불변 문서 데이터로 변환하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderDocumentService {

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PurchaseOrderItemRepository purchaseOrderItemRepository;

	// ========== ORDERED 발주와 품목을 조회하고 이메일 전송용 문서 데이터로 변환하는 메서드 ==========
	// 재전송 요청은 화면에서 조회한 version과 현재 DB version이 같은지 PDF·SMTP 처리 전에 확인한다.
	public PurchaseOrderDocumentData getPurchaseOrderDocument(Long purchaseOrderId, Long requestVersion) {
		PurchaseOrder purchaseOrder = purchaseOrderRepository.findDetailById(purchaseOrderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "발주를 찾을 수 없습니다."));

		if (purchaseOrder.getStatus() != PurchaseOrderStatus.ORDERED) {
			throw new BusinessException(ErrorCode.CONFLICT, "발주 확정 상태에서만 발주서 이메일을 전송할 수 있습니다.");
		}

		if (requestVersion != null && !Objects.equals(purchaseOrder.getVersion(), requestVersion)) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"다른 사용자가 먼저 발주를 처리했습니다. 최신 발주 정보를 다시 조회해 주세요.");
		}

		if (purchaseOrder.getSupplier().getEmail() == null || purchaseOrder.getSupplier().getEmail().isBlank()) {
			throw new BusinessException(ErrorCode.CONFLICT, "공급업체 발주 이메일이 없어 발주서를 전송할 수 없습니다.");
		}

		List<PurchaseOrderItem> purchaseOrderItems = purchaseOrderItemRepository
				.findAllByPurchaseOrderId(purchaseOrderId);

		if (purchaseOrderItems.isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "발주 품목이 없어 발주서를 전송할 수 없습니다.");
		}

		List<PurchaseOrderDocumentData.Item> documentItems = purchaseOrderItems.stream()
				.map(this::createDocumentItem)
				.toList();

		return new PurchaseOrderDocumentData(purchaseOrder.getPurchaseOrderId(),
				purchaseOrder.getSupplier().getSupplierCode(), purchaseOrder.getSupplier().getSupplierName(),
				purchaseOrder.getSupplier().getEmail(), purchaseOrder.getOrderedAt(), purchaseOrder.getMemo(),
				purchaseOrder.getTotalAmount(), documentItems, purchaseOrder.getVersion());
	}

	// ========== 발주 품목 Entity를 PDF Table 한 행에 사용할 불변 문서 품목으로 변환하는 메서드 ==========
	private PurchaseOrderDocumentData.Item createDocumentItem(PurchaseOrderItem purchaseOrderItem) {
		String unitName = purchaseOrderItem.getItem().getUnit() == ItemUnit.OTHER
				? purchaseOrderItem.getItem().getOtherUnitName()
				: purchaseOrderItem.getItem().getUnit().name();

		return new PurchaseOrderDocumentData.Item(purchaseOrderItem.getLineNo(),
				purchaseOrderItem.getItem().getItemCode(), purchaseOrderItem.getItem().getItemName(), unitName,
				purchaseOrderItem.getOrderedQuantity(), purchaseOrderItem.getUnitPrice(),
				purchaseOrderItem.getLineAmount());
	}
}
