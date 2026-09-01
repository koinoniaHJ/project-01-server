package com.erp.server.sales.shipment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.erp.server.sales.shipment.domain.DeliveryNote;
import com.erp.server.sales.shipment.domain.DeliveryNoteStatus;

import jakarta.persistence.LockModeType;

// ********** DELIVERY_NOTE의 기본 CRUD와 주문 취소 시 유효 납품서 잠금 조회를 처리하기 위한 Repository interface **********
public interface DeliveryNoteRepository extends JpaRepository<DeliveryNote, Long> {

	// ========== 출고 상세에 표시할 납품서 발행·무효 이력을 최신 회차부터 조회하는 메서드 ==========
	@Query("""
			select deliveryNote
			from DeliveryNote deliveryNote
			join fetch deliveryNote.issuedBy
			left join fetch deliveryNote.voidedBy
			where deliveryNote.shipment.shipmentId = :shipmentId
			order by deliveryNote.issueSequence desc
			""")
	List<DeliveryNote> findAllByShipmentIdWithUsers(@Param("shipmentId") Long shipmentId);

	// ========== 요청 회차의 유효 납품서와 출고·주문·거래처·창고를 PDF 생성용으로 조회하는 메서드 ==========
	@Query("""
			select deliveryNote
			from DeliveryNote deliveryNote
			join fetch deliveryNote.shipment shipment
			join fetch shipment.salesOrder salesOrder
			join fetch salesOrder.customer
			join fetch shipment.warehouse
			join fetch deliveryNote.issuedBy
			where shipment.shipmentId = :shipmentId
			  and deliveryNote.issueSequence = :issueSequence
			  and deliveryNote.status = :status
			""")
	Optional<DeliveryNote> findByShipmentIdAndIssueSequenceAndStatus(@Param("shipmentId") Long shipmentId,
			@Param("issueSequence") Integer issueSequence, @Param("status") DeliveryNoteStatus status);

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
