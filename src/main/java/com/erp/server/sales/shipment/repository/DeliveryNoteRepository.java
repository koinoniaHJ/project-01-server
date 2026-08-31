package com.erp.server.sales.shipment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.sales.shipment.domain.DeliveryNote;
import com.erp.server.sales.shipment.domain.DeliveryNoteStatus;

import jakarta.persistence.LockModeType;

// ********** DELIVERY_NOTE의 기본 CRUD와 주문 취소 시 유효 납품서 잠금 조회를 처리하기 위한 Repository interface **********
public interface DeliveryNoteRepository extends JpaRepository<DeliveryNote, Long> {

	// ========== PACKED 주문 취소 시 출고의 ACTIVE 납품서를 발행 회차 순서로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select deliveryNote
			from DeliveryNote deliveryNote
			where deliveryNote.shipment.shipmentId = :shipmentId
			  and deliveryNote.status = :status
			order by deliveryNote.issueSequence asc
			""")
	List<DeliveryNote> findAllByShipmentIdAndStatusForUpdate(@Param("shipmentId") Long shipmentId,
			@Param("status") DeliveryNoteStatus status);
}
